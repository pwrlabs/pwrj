package com.github.pwrlabs.pwrj.Transaction;

import lombok.Getter;
import lombok.experimental.SuperBuilder;
import org.json.JSONArray;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;

@Getter
@SuperBuilder
public class GuardianApprovalTransaction extends Transaction {
    public static final String type = "Guardian Approval";

    private List<String> transactions = new ArrayList<>();

    public GuardianApprovalTransaction(JSONObject json, long blockNumber, long timestamp, int positionInTheBlock) {
        super(json, blockNumber, timestamp, positionInTheBlock);

        JSONArray transactions = json.optJSONArray("transactions", null);
        if (transactions != null) {
            for (int i = 0; i < transactions.length(); i++) {
                this.transactions.add(transactions.getString(i));
            }
        }
    }

    @Override
    public JSONObject toJSON() {
        JSONObject Transaction = super.toJSON();
        JSONArray transactions = new JSONArray();
        for (String transaction : this.transactions) {
            transactions.put(transaction);
        }
        Transaction.put("transactions", transactions);
        return Transaction;
    }
}
