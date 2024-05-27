package com.github.pwrlabs.pwrj.record.block;

import com.github.pwrlabs.pwrj.record.transaction.Transaction;
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
    private final boolean processedWithoutCriticalErrors;
    private final Transaction[] transactions;

    public Block(JSONObject blockJson) {
        transactionCount = blockJson.optInt("transactionCount", 0);
        size = blockJson.optInt("blockSize", 0);
        number = blockJson.optLong("blockNumber", 0);
        reward = blockJson.optLong("blockReward", 0);
        timestamp = blockJson.optLong("timestamp", 0);
        hash = blockJson.optString("blockHash", null);
        submitter = blockJson.optString("blockSubmitter", null);
        processedWithoutCriticalErrors = blockJson.optBoolean("processedWithoutCriticalErrors", true);

        JSONArray txns = blockJson.getJSONArray("transactions");
        transactions = new Transaction[txns.length()];

        for(int i = 0; i < txns.length(); i++) {
            JSONObject txnObject = txns.getJSONObject(i);
            Transaction txn = Transaction.fromJSON(txnObject, number, timestamp, i);
            transactions[i] = txn;
        }

    }

    public boolean processedWithoutCriticalErrors() {
        return processedWithoutCriticalErrors;
    }
}
