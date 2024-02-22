package com.github.pwrlabs.pwrj.Block;

import com.github.pwrlabs.pwrj.Transaction.*;
import com.github.pwrlabs.pwrj.protocol.PWRJ;
import lombok.Getter;
import org.json.JSONArray;
import org.json.JSONObject;

@Getter
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
        transactionCount = blockJson.optInt("transactionCount", 0);
        size = blockJson.optInt("blockSize", 0);
        number = blockJson.optLong("blockNumber", 0);
        reward = blockJson.optLong("blockReward", 0);
        timestamp = blockJson.optLong("timestamp", 0);
        hash = blockJson.optString("blockHash", null);
        submitter = blockJson.optString("blockSubmitter", null);
        success = blockJson.optBoolean("success", false);

        JSONArray txns = (JSONArray) PWRJ.getOrDefault(blockJson, "transactions", new JSONArray());
        transactions = new Transaction[txns.length()];

        for(int i = 0; i < txns.length(); i++) {
            JSONObject txnObject = txns.getJSONObject(i);
            Transaction txn = null;
            
            String txnType = txnObject.has("type") ? txnObject.getString("type") : "Unknown";
            if(txnType.equalsIgnoreCase("Transfer")) {
                txn = new TransferTxn(txnObject);
            } else if(txnType.equalsIgnoreCase("VM Data")) {
                txn = new VmDataTxn(txnObject);
            } else if(txnType.equalsIgnoreCase("Delegate")) {
                txn = new DelegateTxn(txnObject);
            } else if(txnType.equalsIgnoreCase("Withdraw")) {
                txn = new WithdrawTxn(txnObject);
            } else if(txnType.equalsIgnoreCase("Validator Join")) {
                txn = new JoinTxn(txnObject);
            } else if(txnType.equalsIgnoreCase("Claim VM ID")) {
                txn = new ClaimVmIdTxn(txnObject);
            } else {
                txn = new Transaction(txnObject);
            }

            transactions[i] = txn;
        }

    }
}
