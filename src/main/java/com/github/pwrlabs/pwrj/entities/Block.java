package com.github.pwrlabs.pwrj.entities;

import io.pwrlabs.utils.BinaryJSONKeyMapper;
import lombok.Getter;
import org.json.JSONArray;
import org.json.JSONObject;

import java.math.BigInteger;
import java.util.ArrayList;
import java.util.List;

@Getter
/**
 * Block class.
 */
public class Block {
    private final long blockNumber;
    private final long timestamp;
    private final long blockReward;
    private final long burnedFees;
    private final int blockSize;
    private final String blockHash;
    private final String previousBlockHash;
    private final String rootHash;
    private final String proposer;
    private final BigInteger newSharesPerSpark;
    private List<String> transactionHashes = new ArrayList<>();
    private final boolean processedWithoutCriticalErrors;

    public Block(JSONObject blockJson) {
        this.blockNumber = blockJson.getLong(BinaryJSONKeyMapper.BLOCK_NUMBER);
        this.timestamp = blockJson.getLong(BinaryJSONKeyMapper.TIME_STAMP);
        this.blockReward = blockJson.getLong(BinaryJSONKeyMapper.BLOCK_REWARD);
        this.burnedFees = blockJson.getLong(BinaryJSONKeyMapper.BURNED_FEES);
        this.blockSize = blockJson.getInt(BinaryJSONKeyMapper.SIZE);
        this.blockHash = blockJson.getString(BinaryJSONKeyMapper.BLOCK_HASH);
        this.previousBlockHash = blockJson.getString(BinaryJSONKeyMapper.PREVIOUS_BLOCK_HASH);
        this.rootHash = blockJson.getString(BinaryJSONKeyMapper.ROOT_HASH);
        this.proposer = blockJson.getString(BinaryJSONKeyMapper.PROPOSER);
        this.newSharesPerSpark = blockJson.getBigInteger(BinaryJSONKeyMapper.NEW_SHARES_PER_SPARK);
        this.processedWithoutCriticalErrors = blockJson.getBoolean(BinaryJSONKeyMapper.PROCESS_WITHOUT_CRITICAL_ERRORS);

        JSONArray transactionHashJson = blockJson.optJSONArray(BinaryJSONKeyMapper.TRANSACTIONS);
        if(transactionHashJson != null && !transactionHashJson.isEmpty()) {
            for (int i = 0; i < transactionHashJson.length(); i++) {
                JSONObject txnInfo = transactionHashJson.getJSONObject(i);
                this.transactionHashes.add(txnInfo.getString(BinaryJSONKeyMapper.TRANSACTION_HASH));
            }
        }
    }

/**
 * getTransactionHashes method.
 * @return value
 */
    public List<String> getTransactionHashes() {
        if(transactionHashes == null) return new ArrayList<>();
        else return transactionHashes;
    }

/**
 * getTransactionCount method.
 * @return value
 */
    public int getTransactionCount() {
        if(transactionHashes == null) return 0;
        else return transactionHashes.size();
    }

}
