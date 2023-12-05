package com.github.pwrlabs.pwrj.Block;

import com.github.pwrlabs.pwrj.Transaction.DelegateTxn;
import com.github.pwrlabs.pwrj.Transaction.Transaction;
import com.github.pwrlabs.pwrj.Transaction.TransferTxn;
import com.github.pwrlabs.pwrj.Transaction.VmDataTxn;
import org.json.JSONArray;
import org.json.JSONObject;

public class Block {
    private final int transactionCount;
    private final int size;
    private final long number;
    private final long reward;
    private final long timestamp;
    private final String hash;
    private final String submitter;
    private final boolean success;
    private final Transaction[] transactions;

    public Block(JSONObject blockJson) {
        transactionCount = blockJson.getInt("transactionCount");
        size = blockJson.getInt("blockSize");
        number = blockJson.getLong("blockNumber");
        reward = blockJson.getLong("blockReward");
        timestamp = blockJson.getLong("timestamp");
        hash = blockJson.getString("blockHash");
        submitter = blockJson.getString("blockSubmitter");
        success = blockJson.getBoolean("success");

        JSONArray txns = blockJson.getJSONArray("transactions");
        transactions = new Transaction[txns.length()];

        for(int i = 0; i < txns.length(); i++) {
            JSONObject txnObject = txns.getJSONObject(i);
            Transaction txn = null;
            
            String txnType = txnObject.has("type") ? txnObject.getString("type") : "Unknown";
            if(txnType.equalsIgnoreCase("Transfer")) {
                txn = new TransferTxn(txnObject.getInt("size"), txnObject.getInt("positionInTheBlock"), txnObject.getLong("fee"), txnType, txnObject.getString("from"), txnObject.getString("to"), txnObject.getString("nonceOrValidationHash"), txnObject.getString("hash"), txnObject.getLong("value"));
            } else if(txnType.equalsIgnoreCase("VM Data")) {
                txn = new VmDataTxn(txnObject.getInt("size"), txnObject.getInt("positionInTheBlock"), txnObject.getLong("fee"), txnType, txnObject.getString("from"), txnObject.getString("to"), txnObject.getString("nonceOrValidationHash"), txnObject.getString("hash"), txnObject.getLong("vmId"), txnObject.getString("data"));
            } else if(txnType.equalsIgnoreCase("Delegate")) {
                txn = new DelegateTxn(txnObject.getInt("size"), txnObject.getInt("positionInTheBlock"), txnObject.getLong("fee"), txnType, txnObject.getString("from"), txnObject.getString("to"), txnObject.getString("nonceOrValidationHash"), txnObject.getString("hash"), txnObject.getLong("amount"));
            } else {
                txn = new Transaction(txnObject.getInt("size"), txnObject.getInt("positionInTheBlock"), txnObject.getLong("fee"), txnType, txnObject.getString("from"), txnObject.getString("to"), txnObject.getString("nonceOrValidationHash"), txnObject.getString("hash"));
            }

            transactions[i] = txn;
        }

    }

    //Getters

    //javadoc of the below function
    /**
     * @return the number of transactions in the block
     */
    public int getTransactionCount() {
        return transactionCount;
    }

    //javadoc of the below function
    /**
     * @return the byte size of the block
     */
    public int getSize() {
        return size;
    }

    //javadoc of the below function
    /**
     * @return the number of the block
     */
    public long getNumber() {
        return number;
    }

    //javadoc of the below function
    /**
     * @return the reward of the block
     */
    public long getReward() {
        return reward;
    }

    //javadoc of the below function
    /**
     * @return the timestamp of the block
     */
    public long getTimestamp() {
        return timestamp;
    }

    //javadoc of the below function
    /**
     * @return the hash of the block
     */
    public String getHash() {
        return hash;
    }

    //javadoc of the below function
    /**
     * @return the address of the submitter of the block
     */
    public String getSubmitter() {
        return submitter;
    }

    //javadoc of the below function
    /**
     * @return whether the block was processed without issues
     */
    public boolean isSuccess() {
        return success;
    }

    //javadoc of the below function
    /**
     * @return the transactions in the block
     */
    public Transaction[] getTransactions() {
        return transactions;
    }
}
