package com.github.pwrlabs.pwrj.Transaction;

import org.json.JSONArray;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;

public class ConduitApprovalTransaction extends Transaction {
    public static final String type = "Conduit Approval";

    private long vmId;
    private List<String> transactions = new ArrayList<>();

    public ConduitApprovalTransaction(JSONObject json, long blockNumber, long timestamp, int positionInTheBlock) {
        super(json, blockNumber, timestamp, positionInTheBlock);
        this.vmId = json.optLong("vmId", 0);
        JSONArray transactions = json.optJSONArray("transactions", null);
        if (transactions != null) {
            for (int i = 0; i < transactions.length(); i++) {
                this.transactions.add(transactions.getString(i));
            }
        }
    }
}
