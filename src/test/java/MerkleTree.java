
import com.sun.tools.javac.Main;
import io.pwrlabs.hashing.PWRHash;
import io.pwrlabs.util.encoders.BiResult;
import io.pwrlabs.util.encoders.ByteArrayWrapper;
import io.pwrlabs.util.encoders.Hex;
import io.pwrlabs.util.files.FileUtils;
import lombok.Getter;
import org.rocksdb.*;

import java.io.File;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

import static io.pwrlabs.newerror.NewError.errorIf;

/**
 * EdyMerkleTree: A Merkle Tree backed by RocksDB storage.
 */
public class MerkleTree {

    //region ===================== Statics =====================
    private static final org.slf4j.Logger logger = org.slf4j.LoggerFactory.getLogger(MerkleTree.class);

    static {
        RocksDB.loadLibrary();
    }

    private static Map<String /*Tree Name*/, MerkleTree> openTrees = new ConcurrentHashMap<>();
    //endregion

    //region ===================== Constants =====================
    private static final int HASH_LENGTH = 32;
    private static final String METADATA_DB_NAME = "metaData";
    private static final String NODES_DB_NAME = "nodes";
    private static final String KEY_DATA_DB_NAME = "keyData";

    // Metadata Keys
    private static final String KEY_ROOT_HASH = "rootHash";
    private static final String KEY_NUM_LEAVES = "numLeaves";
    private static final String KEY_DEPTH = "depth";
    private static final String KEY_HANGING_NODE_PREFIX = "hangingNode";
    //endregion

    //region ===================== Fields =====================
    @Getter
    private final String treeName;
    @Getter
    private final String path;

    private RocksDB db;
    private ColumnFamilyHandle metaDataHandle;
    private ColumnFamilyHandle nodesHandle;
    private ColumnFamilyHandle keyDataHandle;


    /**
     * Cache of loaded nodes (in-memory for quick access).
     */
    private final Map<ByteArrayWrapper, Node> nodesCache = new ConcurrentHashMap<>();
    private final Map<Integer /*level*/, byte[]> hangingNodes = new ConcurrentHashMap<>();
    private final Map<ByteArrayWrapper /*Key*/, byte[] /*data*/> keyDataCache = new ConcurrentHashMap<>();

    @Getter
    private int numLeaves = 0;
    @Getter
    private int depth = 0;
    private byte[] rootHash = null;

    private AtomicBoolean closed = new AtomicBoolean(false);
    private AtomicBoolean hasUnsavedChanges = new AtomicBoolean(false);

    /**
     * Lock for reading/writing to the tree.
     */
    private final ReadWriteLock lock = new ReentrantReadWriteLock();
    //endregion

    //region ===================== Constructors =====================
    public MerkleTree(String treeName) throws RocksDBException {
        //ObjectsInMemoryTracker.trackObject(this);
        RocksDB.loadLibrary();
        this.treeName = treeName;
        errorIf(openTrees.containsKey(treeName), "There is already open instance of this tree");

        // 1. Ensure directory exists
        path = "merkleTree/" + treeName;
        File directory = new File(path);
        if (!directory.exists() && !directory.mkdirs()) {
            throw new RocksDBException("Failed to create directory: " + path);
        }

        // 2. Initialize DB
        initializeDb();

        // 7. Load initial metadata
        loadMetaData();

        // 8. Register instance
        openTrees.put(treeName, this);

        // 9. Force manual compaction on startup to reduce memory footprint
        try {
            db.compactRange();
        } catch (Exception e) {
            // Ignore compaction errors
        }
    }
    //endregion

    //region ===================== Public Methods =====================

    /**
     * Get the current root hash of the Merkle tree.
     */
    public byte[] getRootHash() {
        errorIfClosed();
        getReadLock();
        try {
            if (rootHash == null) return null;
            else return Arrays.copyOf(rootHash, rootHash.length);
        } finally {
            releaseReadLock();
        }
    }

    public byte[] getRootHashSavedOnDisk() {
        errorIfClosed();
        getReadLock();
        try {
            return db.get(metaDataHandle, KEY_ROOT_HASH.getBytes());
        } catch (RocksDBException e) {
            throw new RuntimeException(e);
        } finally {
            releaseReadLock();
        }
    }

    public int getNumLeaves() {
        errorIfClosed();
        getReadLock();
        try {
            return numLeaves;
        } finally {
            releaseReadLock();
        }
    }

    public int getDepth() {
        errorIfClosed();
        getReadLock();
        try {
            return depth;
        } finally {
            releaseReadLock();
        }
    }

    /**
     * Get data for a key from the Merkle Tree.
     *
     * @param key The key to retrieve data for
     * @return The data for the key, or null if the key doesn't exist
     * @throws RocksDBException         If there's an error accessing RocksDB
     * @throws IllegalArgumentException If key is null
     */
    public byte[] getData(byte[] key) {
        errorIfClosed();
        byte[] data = keyDataCache.get(new ByteArrayWrapper(key));
        if (data != null) return data;

        try {
            return db.get(keyDataHandle, key);
        } catch (RocksDBException e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * Add or update data for a key in the Merkle Tree.
     * This will create a new leaf node with a hash derived from the key and data,
     * or update an existing leaf if the key already exists.
     *
     * @param key  The key to store data for
     * @param data The data to store
     * @throws RocksDBException         If there's an error accessing RocksDB
     * @throws IllegalArgumentException If key or data is null
     */
    public void addOrUpdateData(byte[] key, byte[] data) throws RocksDBException {
        errorIfClosed();

        if (key == null) {
            throw new IllegalArgumentException("Key cannot be null");
        }
        if (data == null) {
            throw new IllegalArgumentException("Data cannot be null");
        }

        getWriteLock();
        try {
            //            Check if key already exists
            byte[] existingData = getData(key);
            byte[] oldLeafHash = existingData == null ? null : calculateLeafHash(key, existingData);

            // Calculate hash from key and data
            byte[] newLeafHash = calculateLeafHash(key, data);

            if (oldLeafHash != null && Arrays.equals(oldLeafHash, newLeafHash)) return;

            // Store key-data mapping
            keyDataCache.put(new ByteArrayWrapper(key), data);
            hasUnsavedChanges.set(true);

            if (oldLeafHash == null) {
                // Key doesn't exist, add new leaf
                addLeaf(new Node(newLeafHash));
            } else {
                // Key exists, update leaf
                // First get the old leaf hash
                updateLeaf(oldLeafHash, newLeafHash);
            }
        } finally {
            releaseWriteLock();
        }
    }

    public void revertUnsavedChanges() {
        if (!hasUnsavedChanges.get()) return;
        errorIfClosed();

        getWriteLock();
        try {
            nodesCache.clear();
            hangingNodes.clear();
            keyDataCache.clear();

            loadMetaData();

            hasUnsavedChanges.set(false);
        } catch (RocksDBException e) {
            throw new RuntimeException(e);
        } finally {
            releaseWriteLock();
        }
    }

    public boolean containsKey(byte[] key) {
        errorIfClosed();

        if (key == null) {
            throw new IllegalArgumentException("Key cannot be null");
        }

        getReadLock();
        try {
            return db.get(keyDataHandle, key) != null;
        } catch (RocksDBException e) {
            throw new RuntimeException(e);
        } finally {
            releaseReadLock();
        }
    }

    /**
     * Flush all in-memory changes (nodes, metadata) to RocksDB.
     */
    public void flushToDisk() throws RocksDBException {
        if (!hasUnsavedChanges.get()) return;

        errorIfClosed();
        getWriteLock();
        try {
            try (WriteBatch batch = new WriteBatch()) {
                //Clear old metadata from disk
                try (RocksIterator iterator = db.newIterator(metaDataHandle)) {
                    iterator.seekToFirst();
                    while (iterator.isValid()) {
                        batch.delete(metaDataHandle, iterator.key());
                        iterator.next();
                    }
                }

                if (rootHash != null) {
                    batch.put(metaDataHandle, KEY_ROOT_HASH.getBytes(), rootHash);
                } else {
                    batch.delete(metaDataHandle, KEY_ROOT_HASH.getBytes());
                }
                batch.put(metaDataHandle, KEY_NUM_LEAVES.getBytes(), ByteBuffer.allocate(4).putInt(numLeaves).array());
                batch.put(metaDataHandle, KEY_DEPTH.getBytes(), ByteBuffer.allocate(4).putInt(depth).array());

                for (Map.Entry<Integer, byte[]> entry : hangingNodes.entrySet()) {
                    Integer level = entry.getKey();
                    byte[] nodeHash = entry.getValue();
                    batch.put(metaDataHandle, (KEY_HANGING_NODE_PREFIX + level).getBytes(), nodeHash);
                }

                for (Node node : nodesCache.values()) {
                    batch.put(nodesHandle, node.hash, node.encode());

                    if (node.getNodeHashToRemoveFromDb() != null) {
                        batch.delete(nodesHandle, node.getNodeHashToRemoveFromDb());
                    }
                }

                for (Map.Entry<ByteArrayWrapper, byte[]> entry : keyDataCache.entrySet()) {
                    batch.put(keyDataHandle, entry.getKey().data(), entry.getValue());
                }

                try (WriteOptions writeOptions = new WriteOptions()) {
                    db.write(writeOptions, batch);
                }

                nodesCache.clear();
                keyDataCache.clear();
                hasUnsavedChanges.set(false);
            }
        } finally {
            releaseWriteLock();
        }
    }

    /**
     * Close the databases (optional, if you need cleanup).
     */
    public void close() throws RocksDBException {
        getWriteLock();
        try {
            if (closed.get()) return;
            flushToDisk();

            if (metaDataHandle != null) {
                try {
                    metaDataHandle.close();
                } catch (Exception e) {
                    // Log error
                }
            }

            if (nodesHandle != null) {
                try {
                    nodesHandle.close();
                } catch (Exception e) {
                    // Log error
                }
            }

            if (keyDataHandle != null) {
                try {
                    keyDataHandle.close();
                } catch (Exception e) {
                    // Log error
                }
            }

            if (db != null) {
                try {
                    db.close();
                } catch (Exception e) {
                    // Log error
                }
            }

            openTrees.remove(treeName);
            closed.set(true);
        } finally {
            releaseWriteLock();
        }
    }

    /**
     * Efficiently clears the entire MerkleTree by closing, deleting and recreating the RocksDB instance.
     * This is much faster than iterating through all entries and deleting them individually.
     */
    public void clear() throws RocksDBException {
        errorIfClosed();
        getWriteLock();
        try {
            // mark every key in each CF as deleted (empty → 0xFF)
            byte[] start = new byte[0];
            byte[] end = new byte[]{(byte) 0xFF};

            // these three calls are each O(1)
            db.deleteRange(metaDataHandle, start, end);
            db.deleteRange(nodesHandle, start, end);
            db.deleteRange(keyDataHandle, start, end);

            // OPTIONAL: reclaim space immediately
            db.compactRange(metaDataHandle);
            db.compactRange(nodesHandle);
            db.compactRange(keyDataHandle);

            // reset your in-memory state
            nodesCache.clear();
            keyDataCache.clear();
            hangingNodes.clear();
            rootHash = null;
            numLeaves = depth = 0;
            hasUnsavedChanges.set(false);

        } finally {
            releaseWriteLock();
        }
    }

    //endregion

    //region ===================== Private Methods =====================
    private void getWriteLock() {
        lock.writeLock().lock();
    }

    private void releaseWriteLock() {
        lock.writeLock().unlock();
    }

    private void getReadLock() {
        lock.readLock().lock();
    }

    private void releaseReadLock() {
        lock.readLock().unlock();
    }

    private void initializeDb() throws RocksDBException {
        // 1) DBOptions
        DBOptions dbOptions = new DBOptions()
                .setCreateIfMissing(true)
                .setCreateMissingColumnFamilies(true)
                .setMaxOpenFiles(100)
                .setMaxBackgroundJobs(1)
                .setInfoLogLevel(InfoLogLevel.FATAL_LEVEL);
        // (omit setNoBlockCache or any “disable cache” flags)

        // 2) Table format: enable a 64 MB off-heap LRU cache
        BlockBasedTableConfig tableConfig = new BlockBasedTableConfig()
                .setBlockCache(new LRUCache(64 * 1024L * 1024L))  // 64 MiB
                .setBlockSize(4 * 1024)        // 4 KiB blocks
                .setFormatVersion(5)
                .setChecksumType(ChecksumType.kxxHash);

        // 3) ColumnFamilyOptions: reference our tableConfig
        ColumnFamilyOptions cfOptions = new ColumnFamilyOptions()
                .setTableFormatConfig(tableConfig)
                .setCompressionType(CompressionType.NO_COMPRESSION)
                .setWriteBufferSize(16 * 1024 * 1024)
                .setMaxWriteBufferNumber(1)
                .setMinWriteBufferNumberToMerge(1)
                .optimizeUniversalStyleCompaction();

        // 4) Prepare column-family descriptors & handles as before…
        List<ColumnFamilyDescriptor> cfDescriptors = List.of(
                new ColumnFamilyDescriptor(RocksDB.DEFAULT_COLUMN_FAMILY, cfOptions),
                new ColumnFamilyDescriptor(METADATA_DB_NAME.getBytes(), cfOptions),
                new ColumnFamilyDescriptor(NODES_DB_NAME.getBytes(), cfOptions),
                new ColumnFamilyDescriptor(KEY_DATA_DB_NAME.getBytes(), cfOptions)
        );
        List<ColumnFamilyHandle> cfHandles = new ArrayList<>();

        // 5) Open the DB
        this.db = RocksDB.open(dbOptions, path, cfDescriptors, cfHandles);

        // 6) Assign handles…
        this.metaDataHandle = cfHandles.get(1);
        this.nodesHandle    = cfHandles.get(2);
        this.keyDataHandle  = cfHandles.get(3);
    }

    /**
     * Load the tree's metadata from RocksDB.
     */
    private void loadMetaData() throws RocksDBException {
        getReadLock();
        try {
            this.rootHash = db.get(metaDataHandle, KEY_ROOT_HASH.getBytes());

            byte[] numLeavesBytes = db.get(metaDataHandle, KEY_NUM_LEAVES.getBytes());
            this.numLeaves = (numLeavesBytes == null)
                    ? 0
                    : ByteBuffer.wrap(numLeavesBytes).getInt();

            byte[] depthBytes = db.get(metaDataHandle, KEY_DEPTH.getBytes());
            this.depth = (depthBytes == null)
                    ? 0
                    : ByteBuffer.wrap(depthBytes).getInt();

            // Load any hangingNodes from metadata
            hangingNodes.clear();
            for (int i = 0; i <= depth; i++) {
                byte[] hash = db.get(metaDataHandle, (KEY_HANGING_NODE_PREFIX + i).getBytes());
                if (hash != null) {
                    Node node = getNodeByHash(hash);
                    hangingNodes.put(i, node.hash);
                }
            }
        } finally {
            releaseReadLock();
        }
    }

    /**
     * Fetch a node by its hash, either from the in-memory cache or from RocksDB.
     */
    private Node getNodeByHash(byte[] hash) throws RocksDBException {
        getReadLock();
        try {
            if (hash == null) return null;

            ByteArrayWrapper baw = new ByteArrayWrapper(hash);
            Node node = nodesCache.get(baw);
            if (node == null) {
                try {
                    byte[] encodedData = db.get(nodesHandle, hash);
                    if (encodedData == null) {
                        return null;
                    }
                    node = decodeNode(encodedData);
                    nodesCache.put(baw, node);
                } catch (Exception e) {
                    e.printStackTrace();
                    throw e;
                }
            }

            return node;
        } finally {
            releaseReadLock();
        }
    }

    /**
     * Decode a node from bytes.
     */
    private Node decodeNode(byte[] encodedData) {
        ByteBuffer buffer = ByteBuffer.wrap(encodedData);

        byte[] hash = new byte[HASH_LENGTH];
        buffer.get(hash);

        boolean hasLeft = buffer.get() == 1;
        boolean hasRight = buffer.get() == 1;
        boolean hasParent = buffer.get() == 1;

        byte[] left = hasLeft ? new byte[HASH_LENGTH] : null;
        byte[] right = hasRight ? new byte[HASH_LENGTH] : null;
        byte[] parent = hasParent ? new byte[HASH_LENGTH] : null;

        if (hasLeft) buffer.get(left);
        if (hasRight) buffer.get(right);
        if (hasParent) buffer.get(parent);

        return new Node(hash, left, right, parent);
    }

    private byte[] calculateLeafHash(byte[] key, byte[] data) {
        return PWRHash.hash256(key, data);
    }

    /**
     * Add a new leaf node to the Merkle Tree.
     */
    private void addLeaf(Node leafNode) throws RocksDBException {
        if (leafNode == null) {
            throw new IllegalArgumentException("Leaf node cannot be null");
        }
        if (leafNode.hash == null) {
            throw new IllegalArgumentException("Leaf node hash cannot be null");
        }

        getWriteLock();
        try {
            if (numLeaves == 0) {
                hangingNodes.put(0, leafNode.hash);
                rootHash = leafNode.hash;
            } else {
                Node hangingLeaf = getNodeByHash(hangingNodes.get(0));

                // If there's no hanging leaf at level 0, place this one there.
                if (hangingLeaf == null) {
                    hangingNodes.put(0, leafNode.hash);

                    Node parentNode = new Node(leafNode.hash, null);
                    leafNode.setParentNodeHash(parentNode.hash);
                    addNode(1, parentNode);
                } else {
                    // If a leaf is already hanging, connect this leaf with the parent's parent
                    if (hangingLeaf.parent == null) { //If the hanging leaf is the root
                        Node parentNode = new Node(hangingLeaf.hash, leafNode.hash);
                        hangingLeaf.setParentNodeHash(parentNode.hash);
                        leafNode.setParentNodeHash(parentNode.hash);
                        addNode(1, parentNode);
                    } else {
                        Node parentNodeOfHangingLeaf = getNodeByHash(hangingLeaf.parent);
                        if (parentNodeOfHangingLeaf == null) {
                            throw new IllegalStateException("Parent node of hanging leaf not found");
                        }
                        parentNodeOfHangingLeaf.addLeaf(leafNode.hash);
                    }
                    hangingNodes.remove(0);
                }
            }

            numLeaves++;
        } finally {
            releaseWriteLock();
        }
    }

    private void updateLeaf(byte[] oldLeafHash, byte[] newLeafHash) {
        if (oldLeafHash == null) {
            throw new IllegalArgumentException("Old leaf hash cannot be null");
        }
        if (newLeafHash == null) {
            throw new IllegalArgumentException("New leaf hash cannot be null");
        }
        errorIf(Arrays.equals(oldLeafHash, newLeafHash), "Old and new leaf hashes cannot be the same");

        getWriteLock();
        try {
            Node leaf = getNodeByHash(oldLeafHash);

            if (leaf == null) {
                throw new IllegalArgumentException("Leaf not found: " + Hex.toHexString(oldLeafHash));
            } else {
                leaf.updateNodeHash(newLeafHash);
            }
        } catch (RocksDBException e) {
            throw new RuntimeException("Error updating leaf: " + e.getMessage(), e);
        } finally {
            releaseWriteLock();
        }
    }

    /**
     * Add a node at a given level.
     */
    private void addNode(int level, Node node) throws RocksDBException {
        getWriteLock();
        try {
            if (level > depth) depth = level;
            Node hangingNode = getNodeByHash(hangingNodes.get(level));

            if (hangingNode == null) {
                // No hanging node at this level, so let's hang this node.
                hangingNodes.put(level, node.hash);

                // If this level is the depth level, this node's hash is the new root hash
                if (level >= depth) {
                    rootHash = node.hash;
                } else {
                    // Otherwise, create a parent and keep going up
                    Node parentNode = new Node(node.hash, null);
                    node.setParentNodeHash(parentNode.hash);
                    addNode(level + 1, parentNode);
                }
            } else if (hangingNode.parent == null) {
                Node parent = new Node(hangingNode.hash, node.hash);
                hangingNode.setParentNodeHash(parent.hash);
                node.setParentNodeHash(parent.hash);
                hangingNodes.remove(level);
                addNode(level + 1, parent);
            } else {
                // If a node is already hanging at this level, attach the new node as a leaf
                Node parentNodeOfHangingNode = getNodeByHash(hangingNode.parent);
                if (parentNodeOfHangingNode != null) {
                    parentNodeOfHangingNode.addLeaf(node.hash);
                    hangingNodes.remove(level);
                } else {
                    // If parent node is null, create a new parent node
                    Node parent = new Node(hangingNode.hash, node.hash);
                    hangingNode.setParentNodeHash(parent.hash);
                    node.setParentNodeHash(parent.hash);
                    hangingNodes.remove(level);
                    addNode(level + 1, parent);
                }
            }
        } finally {
            releaseWriteLock();
        }
    }

    private void errorIfClosed() {
        if (closed.get()) {
            throw new IllegalStateException("MerkleTree is closed");
        }
    }

    //endregion

    //region ===================== Nested Classes =====================

    /**
     * Represents a single node in the Merkle Tree.
     */
    @Getter
    public class Node {
        private byte[] hash;
        private byte[] left;
        private byte[] right;
        private byte[] parent;

        /**
         * The old hash of the node before it was updated. This is used to delete the old node from the db.
         */
        @Getter
        private byte[] nodeHashToRemoveFromDb = null;

        /**
         * Construct a leaf node with a known hash.
         */
        public Node(byte[] hash) {
            if (hash == null) {
                throw new IllegalArgumentException("Node hash cannot be null");
            }
            this.hash = hash;

            nodesCache.put(new ByteArrayWrapper(hash), this);
        }

        /**
         * Construct a node with all fields.
         */
        public Node(byte[] hash, byte[] left, byte[] right, byte[] parent) {
            if (hash == null) {
                throw new IllegalArgumentException("Node hash cannot be null");
            }
            this.hash = hash;
            this.left = left;
            this.right = right;
            this.parent = parent;

            nodesCache.put(new ByteArrayWrapper(hash), this);
        }

        /**
         * Construct a node (non-leaf) with leftHash and rightHash, auto-calculate node hash.
         */
        public Node(byte[] left, byte[] right) {
            // At least one of left or right must be non-null
            if (left == null && right == null) {
                throw new IllegalArgumentException("At least one of left or right hash must be non-null");
            }

            this.left = left;
            this.right = right;
            this.hash = calculateHash();

            if (this.hash == null) {
                throw new IllegalStateException("Failed to calculate node hash");
            }

            nodesCache.put(new ByteArrayWrapper(hash), this);
        }

        /**
         * Calculate the hash of this node based on the left and right child hashes.
         */
        public byte[] calculateHash() {
            getReadLock();
            try {
                if (left == null && right == null) {
                    // Could be a leaf that hasn't set its hash, or zero-length node
                    return null;
                }

                byte[] leftHash = (left != null) ? left : right; // Duplicate if necessary
                byte[] rightHash = (right != null) ? right : left;
                return PWRHash.hash256(leftHash, rightHash);
            } finally {
                releaseReadLock();
            }
        }

        /**
         * Encode the node into a byte[] for storage in RocksDB.
         */
        public byte[] encode() {
            getReadLock();
            try {
                boolean hasLeft = (left != null);
                boolean hasRight = (right != null);
                boolean hasParent = (parent != null);

                int length = HASH_LENGTH + 3 // flags for hasLeft, hasRight, hasParent
                        + (hasLeft ? HASH_LENGTH : 0)
                        + (hasRight ? HASH_LENGTH : 0)
                        + (hasParent ? HASH_LENGTH : 0);

                ByteBuffer buffer = ByteBuffer.allocate(length);
                buffer.put(hash);
                buffer.put(hasLeft ? (byte) 1 : (byte) 0);
                buffer.put(hasRight ? (byte) 1 : (byte) 0);
                buffer.put(hasParent ? (byte) 1 : (byte) 0);

                if (hasLeft) buffer.put(left);
                if (hasRight) buffer.put(right);
                if (hasParent) buffer.put(parent);

                return buffer.array();
            } finally {
                releaseReadLock();
            }
        }

        /**
         * Set this node's parent.
         */
        public void setParentNodeHash(byte[] parentHash) {
            getWriteLock();
            try {
                this.parent = parentHash;
            } finally {
                releaseWriteLock();
            }
        }

        /**
         * Update this node's hash and propagate the change upward.
         */
        public void updateNodeHash(byte[] newHash) throws RocksDBException {
            getWriteLock();
            try {
                //We only store the old hash if it is not already set. Because we only need to delete the old node from the db once.
                //New hashes don't have copies in db since they haven't been flushed to disk yet
                if (nodeHashToRemoveFromDb == null) nodeHashToRemoveFromDb = this.hash;

                byte[] oldHash = Arrays.copyOf(this.hash, this.hash.length);
                this.hash = newHash;

                for (Map.Entry<Integer, byte[]> entry : hangingNodes.entrySet()) {
                    if (Arrays.equals(entry.getValue(), oldHash)) {
                        hangingNodes.put(entry.getKey(), newHash);
                        break;
                    }
                }

                nodesCache.remove(new ByteArrayWrapper(oldHash));
                nodesCache.put(new ByteArrayWrapper(newHash), this);

                // Distinguish whether it is a leaf, internal node, or root
                boolean isLeaf = (left == null && right == null);
                boolean isRoot = (parent == null);

                // If this is the root node, update the root hash
                if (isRoot) {
                    rootHash = newHash;

                    if (left != null) {
                        Node leftNode = getNodeByHash(left);
                        if (leftNode != null) {
                            leftNode.setParentNodeHash(newHash);
                        }
                    }

                    if (right != null) {
                        Node rightNode = getNodeByHash(right);
                        if (rightNode != null) {
                            rightNode.setParentNodeHash(newHash);
                        }
                    }
                }

                // If this is a leaf node with a parent, update the parent
                if (isLeaf && !isRoot) {
                    Node parentNode = getNodeByHash(parent);
                    if (parentNode != null) {
                        parentNode.updateLeaf(oldHash, newHash);
                        byte[] newParentHash = parentNode.calculateHash();
                        parentNode.updateNodeHash(newParentHash);
                    }
                }
                // If this is an internal node with a parent, update the parent and children
                else if (!isLeaf && !isRoot) {
                    if (left != null) {
                        Node leftNode = getNodeByHash(left);
                        if (leftNode != null) {
                            leftNode.setParentNodeHash(newHash);
                        }
                    }
                    if (right != null) {
                        Node rightNode = getNodeByHash(right);
                        if (rightNode != null) {
                            rightNode.setParentNodeHash(newHash);
                        }
                    }

                    Node parentNode = getNodeByHash(parent);
                    if (parentNode != null) {
                        parentNode.updateLeaf(oldHash, newHash);
                        byte[] newParentHash = parentNode.calculateHash();
                        parentNode.updateNodeHash(newParentHash);
                    }
                }
            } finally {
                releaseWriteLock();
            }
        }

        /**
         * Update a leaf (left or right) if it matches the old hash.
         */
        public void updateLeaf(byte[] oldLeafHash, byte[] newLeafHash) {
            getWriteLock();
            try {
                if (left != null && Arrays.equals(left, oldLeafHash)) {
                    left = newLeafHash;
                } else if (right != null && Arrays.equals(right, oldLeafHash)) {
                    right = newLeafHash;
                } else {
                    throw new IllegalArgumentException("Old hash not found among this node's children");
                }
            } finally {
                releaseWriteLock();
            }
        }

        /**
         * Add a leaf to this node (either left or right).
         */
        public void addLeaf(byte[] leafHash) throws RocksDBException {
            if (leafHash == null) {
                throw new IllegalArgumentException("Leaf hash cannot be null");
            }

            getWriteLock();
            try {
                Node leafNode = getNodeByHash(leafHash);
                if (leafNode == null) {
                    throw new IllegalArgumentException("Leaf node not found: " + Hex.toHexString(leafHash));
                }

                if (left == null) {
                    left = leafHash;
                } else if (right == null) {
                    right = leafHash;
                } else {
                    throw new IllegalArgumentException("Node already has both left and right children");
                }

                byte[] newHash = calculateHash();
                if (newHash == null) {
                    throw new IllegalStateException("Failed to calculate new hash after adding leaf");
                }
                updateNodeHash(newHash);
            } finally {
                releaseWriteLock();
            }
        }

        @Override
        public int hashCode() {
            return Arrays.hashCode(encode());
        }

        @Override
        public boolean equals(Object obj) {
            if (obj == null || !(obj instanceof Node)) {
                return false;
            }

            Node other = (Node) obj;

            // Compare hash - this is the most important field
            if (this.hash == null && other.hash != null) {
                return false;
            } else if (this.hash != null && other.hash == null) {
                return false;
            } else if (this.hash != null && other.hash != null) {
                if (!Arrays.equals(this.hash, other.hash)) {
                    return false;
                }
            }

            // Compare left child reference
            if (this.left == null && other.left != null) {
                return false;
            } else if (this.left != null && other.left == null) {
                return false;
            } else if (this.left != null && other.left != null) {
                if (!Arrays.equals(this.left, other.left)) {
                    return false;
                }
            }

            // Compare right child reference
            if (this.right == null && other.right != null) {
                return false;
            } else if (this.right != null && other.right == null) {
                return false;
            } else if (this.right != null && other.right != null) {
                if (!Arrays.equals(this.right, other.right)) {
                    return false;
                }
            }

            if (this.parent == null && other.parent != null) {
                return false;
            } else if (this.parent != null && other.parent == null) {
                return false;
            } else if (this.parent != null && other.parent != null) {
                if (!Arrays.equals(this.parent, other.parent)) {
                    return false;
                }
            }

            // Note: We don't compare parent references as they can legitimately differ
            // between source and cloned trees while still representing the same logical node

            return true;
        }
    }
    //endregion
}
