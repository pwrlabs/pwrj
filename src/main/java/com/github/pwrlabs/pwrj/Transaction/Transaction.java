package com.github.pwrlabs.pwrj.Transaction;

import org.json.JSONObject;

public class Transaction {
    private final int size;
    private final long blockNumber;
    private final int positionInTheBlock;
    private final long fee;
    private final String type;
    private final String from;
    private final String to;
    private final String nonceOrValidationHash;
    private final String hash;
    private final long timestamp;

    public Transaction(int size, long blockNumber, int positionInTheBlock, long fee, String type, String from, String to, String nonceOrValidationHash, String hash, long timestamp) {
        this.size = size;
        this.blockNumber = blockNumber;
        this.positionInTheBlock = positionInTheBlock;
        this.fee = fee;
        this.type = type;
        this.from = from;
        this.to = to;
        this.nonceOrValidationHash = nonceOrValidationHash;
        this.hash = hash;
        this.timestamp = timestamp;
    }

    //Getters

    //javadoc of the below function
    /**
     * @return the size of the transaction
     */
    public int getSize() {
        return size;
    }

    /**
     * @return the block number of the transaction
     */
    public long getBlockNumber() {
        return blockNumber;
    }

    //javadoc of the below function
    /**
     * @return the position of the transaction in the block
     */
    public int getPositionInTheBlock() {
        return positionInTheBlock;
    }

    //javadoc of the below function
    /**
     * @return the fee of the transaction
     */
    public long getFee() {
        return fee;
    }

    //javadoc of the below function
    /**
     * @return the type of the transaction
     */
    public String getType() {
        return type;
    }

    //javadoc of the below function
    /**
     * @return the address of the sender of the transaction
     */
    public String getFrom() {
        return from;
    }

    //javadoc of the below function
    /**
     * @return the address of the receiver of the transaction
     */
    public String getTo() {
        return to;
    }

    //javadoc of the below function
    /**
     * @return the nonce of the transaction if it is a ECDSA transaction, or the validation hash if it is a MHBS transaction
     */
    public String getNonceOrValidationHash() {
        return nonceOrValidationHash;
    }

    //javadoc of the below function
    /**
     * @return the hash of the transaction
     */
    public String getHash() {
        return hash;
    }

    //javadoc of the below function
    /**
     * @return the timestamp of the transaction
     */
    public long getTimestamp() {
        return timestamp;
    }

    //Abstract Methods
    public long getValue() {
        return 0;
    }

    public JSONObject toJSON() {
        JSONObject json = new JSONObject();

        json.put("size", size);
        json.put("positionInTheBlock", positionInTheBlock);
        json.put("fee", fee);
        json.put("type", type);
        json.put("from", from);
        json.put("to", to);
        json.put("nonceOrValidationHash", nonceOrValidationHash);
        json.put("hash", hash);
        json.put("blockNumber", getBlockNumber());
        json.put("nonceOrValidationHash", getNonceOrValidationHash());
        json.put("timestamp", getTimestamp());

        return json;
    }


}
