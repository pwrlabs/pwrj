package com.github.pwrlabs.pwrj.Block;

import com.github.pwrlabs.pwrj.Transaction.Transaction;
import com.github.pwrlabs.pwrj.Transaction.TransferTxn;
import com.github.pwrlabs.pwrj.Transaction.VmDataTxn;
import org.json.JSONArray;
import org.json.JSONObject;

public class Block {
    private final int transactionCount;
    private final int blockSize;
    private final long blockNumber;
    private final long blockReward;
    private final long timeStamp;
    private final String blockHash;
    private final String blockSubmitter;
    private final boolean success;
    private final Transaction[] transactions;

    public Block(JSONObject blockJson) {
        transactionCount = blockJson.getInt("transactionCount");
        blockSize = blockJson.getInt("blockSize");
        blockNumber = blockJson.getLong("blockNumber");
        blockReward = blockJson.getLong("blockReward");
        timeStamp = blockJson.getLong("timestamp");
        blockHash = blockJson.getString("blockHash");
        blockSubmitter = blockJson.getString("blockSubmitter");
        success = blockJson.getBoolean("success");

        JSONArray txns = blockJson.getJSONArray("transactions");
        transactions = new Transaction[txns.length()];

        for(int i = 0; i < txns.length(); i++) {
            JSONObject txnObject = txns.getJSONObject(i);
            Transaction txn = null;
            
            String txnType = txnObject.getString("type");
            if(txnType.equalsIgnoreCase("Transfer")) {
                txn = new TransferTxn(txnObject.getInt("size"), txnObject.getInt("positionInTheBlock"), txnObject.getLong("fee"), txnType, txnObject.getString("from"), txnObject.getString("to"), txnObject.getString("hash"), txnObject.getLong("value"));
            } else if(txnType.equalsIgnoreCase("VM Data")) {
                txn = new VmDataTxn(txnObject.getInt("size"), txnObject.getInt("positionInTheBlock"), txnObject.getLong("fee"), txnType, txnObject.getString("from"), txnObject.getString("to"), txnObject.getString("hash"), txnObject.getLong("vmId"), txnObject.getString("data"));
            } else {
                txn = new Transaction(txnObject.getInt("size"), txnObject.getInt("positionInTheBlock"), txnObject.getLong("fee"), txnType, txnObject.getString("from"), txnObject.getString("to"), txnObject.getString("hash"));
            }

            transactions[i] = txn;
        }

    }

    //Getters

    public int getTransactionCount() {
        return transactionCount;
    }

    public int getBlockSize() {
        return blockSize;
    }

    public long getBlockNumber() {
        return blockNumber;
    }

    public long getBlockReward() {
        return blockReward;
    }

    public long getTimeStamp() {
        return timeStamp;
    }

    public String getBlockHash() {
        return blockHash;
    }

    public String getBlockSubmitter() {
        return blockSubmitter;
    }

    public boolean isSuccess() {
        return success;
    }

    public Transaction[] getTransactions() {
        return transactions;
    }
}
