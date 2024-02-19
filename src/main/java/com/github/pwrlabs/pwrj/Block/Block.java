package com.github.pwrlabs.pwrj.Block;

import com.github.pwrlabs.pwrj.Transaction.*;
import com.github.pwrlabs.pwrj.protocol.PWRJ;
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

//        transactionCount = (Integer) PWRJ.getOrDefault(blockJson, "transactionCount", 0);
//        size = (Integer) PWRJ.getOrDefault(blockJson, "blockSize", 0);
//        number = (Long) PWRJ.getOrDefault(blockJson, "blockNumber", 0L);
//        reward = (Long) PWRJ.getOrDefault(blockJson, "blockReward", 0L);
//        timestamp = (Long) PWRJ.getOrDefault(blockJson, "timestamp", 0L);
//        hash = (String) PWRJ.getOrDefault(blockJson, "blockHash", "");
//        submitter = (String) PWRJ.getOrDefault(blockJson, "blockSubmitter", "");
//        success = (Boolean) PWRJ.getOrDefault(blockJson, "success", false);

        JSONArray txns = (JSONArray) PWRJ.getOrDefault(blockJson, "transactions", new JSONArray());
        transactions = new Transaction[txns.length()];

        for(int i = 0; i < txns.length(); i++) {
            JSONObject txnObject = txns.getJSONObject(i);
            Transaction txn = null;
            
            String txnType = txnObject.has("type") ? txnObject.getString("type") : "Unknown";
            if(txnType.equalsIgnoreCase("Transfer")) {
                txn = new TransferTxn(txnObject.getInt("size"), number, txnObject.getInt("positionInTheBlock"), txnObject.getLong("fee"), txnType, txnObject.getString("sender"), txnObject.getString("receiver"), txnObject.getInt("nonce"), txnObject.getString("hash"), timestamp, txnObject.getLong("value"));
            } else if(txnType.equalsIgnoreCase("VM Data")) {
                txn = new VmDataTxn(txnObject.getInt("size"), number, txnObject.getInt("positionInTheBlock"), txnObject.getLong("fee"), txnType, txnObject.getString("sender"), txnObject.getLong("vmId") + "", txnObject.getInt("nonce"), txnObject.getString("hash"), timestamp, txnObject.getLong("vmId"), txnObject.getString("data"));
            } else if(txnType.equalsIgnoreCase("Delegate")) {
                txn = new DelegateTxn(txnObject.getInt("size"), number, txnObject.getInt("positionInTheBlock"), txnObject.getLong("fee"), txnType, txnObject.getString("sender"), txnObject.getString("validator"), txnObject.getInt("nonce"), txnObject.getString("hash"), timestamp, txnObject.getLong("value"));
            } else if(txnType.equalsIgnoreCase("Withdraw")) {
                txn = new WithdrawTxn(txnObject.getInt("size"), number, txnObject.getInt("positionInTheBlock"), txnObject.getLong("fee"), txnType, txnObject.getString("sender"), txnObject.getString("validator"), txnObject.getInt("nonce"), txnObject.getString("hash"), timestamp, txnObject.getLong("sharesAmount"));
            } else if(txnType.equalsIgnoreCase("Validator Join")) {
                txn = new JoinTxn(txnObject.getInt("size"), number, txnObject.getInt("positionInTheBlock"), txnObject.getLong("fee"), txnType, txnObject.getString("sender"), "PWR Chain", txnObject.getInt("nonce"), txnObject.getString("hash"), timestamp);
            } else if(txnType.equalsIgnoreCase("Claim VM ID")) {
                txn = new ClaimVmIdTxn(txnObject.getInt("size"), number, txnObject.getInt("positionInTheBlock"), txnObject.getLong("fee"), txnType, txnObject.getString("sender"), txnObject.getLong("vmId") + "", txnObject.getInt("nonce"), txnObject.getString("hash"), timestamp, txnObject.getLong("vmId"));
            } else {
                txn = new Transaction((int)PWRJ.getOrDefault(txnObject, "size", 0), number, (int)PWRJ.getOrDefault(txnObject, "positionInTheBlock", 0), ((Number)PWRJ.getOrDefault(txnObject, "fee", 0L)).longValue(), txnType, (String)PWRJ.getOrDefault(txnObject, "sender", "0x"), (String)PWRJ.getOrDefault(txnObject, "to", "0x"), (int)PWRJ.getOrDefault(txnObject, "nonce", "0"), (String)PWRJ.getOrDefault(txnObject, "hash", "0x"), timestamp);
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

    public static void main(String[] args) throws Exception {
        PWRJ.setRpcNodeUrl("http://localhost:8085/");
        for(int t=1; t < 1000; ++t) {
            PWRJ.getBlockByNumber(t);
        }
    }
}
