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
            if(txnType.equalsIgnoreCase(TransferTxn.type)) {
                txn = new TransferTxn(txnObject, number, timestamp, i);
            } else if(txnType.equalsIgnoreCase(VmDataTxn.type)) {
                txn = new VmDataTxn(txnObject, number, timestamp, i);
            } else if(txnType.equalsIgnoreCase(DelegateTxn.type)) {
                txn = new DelegateTxn(txnObject, number, timestamp, i);
            } else if(txnType.equalsIgnoreCase(WithdrawTxn.type)) {
                txn = new WithdrawTxn(txnObject, number, timestamp, i);
            } else if(txnType.equalsIgnoreCase(JoinTxn.type)) {
                txn = new JoinTxn(txnObject, number, timestamp, i);
            } else if(txnType.equalsIgnoreCase(ClaimVmIdTxn.type)) {
                txn = new ClaimVmIdTxn(txnObject, number, timestamp, i);
            } else if(txnType.equalsIgnoreCase(SetGuardianTxn.type)) {
                txn = new SetGuardianTxn(txnObject, number, timestamp, i);
            } else if(txnType.equalsIgnoreCase(PayableVmDataTxn.type)) {
                txn = new PayableVmDataTxn(txnObject, number, timestamp, i);
            } else if(txnType.equalsIgnoreCase(GuardianApprovalTxn.type)) {
                txn = new GuardianApprovalTxn(txnObject, number, timestamp, i);
            } else if(txnType.equalsIgnoreCase(ConduitApprovalTxn.type)) {
                txn = new ConduitApprovalTxn(txnObject, number, timestamp, i);
            } else if(txnType.equalsIgnoreCase(RemoveGuardianTxn.type)) {
                txn = new RemoveGuardianTxn(txnObject, number, timestamp, i);
            } else if(txnType.equalsIgnoreCase(ClaimSpotTxn.type)) {
                txn = new ClaimSpotTxn(txnObject, number, timestamp, i);
            } else {
                txn = new Transaction(txnObject, number, timestamp, i);
            }

            transactions[i] = txn;
        }

    }
}
