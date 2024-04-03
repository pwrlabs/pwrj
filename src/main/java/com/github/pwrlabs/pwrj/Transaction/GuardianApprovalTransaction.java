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

    private List<Transaction> transactions = new ArrayList<>();

    public GuardianApprovalTransaction(JSONObject json, long blockNumber, long timestamp, int positionInTheBlock) {
        super(json, blockNumber, timestamp, positionInTheBlock);

        JSONArray transactions = json.getJSONArray("transactions");
        if (transactions != null) {
            for (int i = 0; i < transactions.length(); i++) {
                this.transactions.add(new Transaction(transactions.getJSONObject(i), blockNumber, timestamp, positionInTheBlock));
            }
        }
    }

    @Override
    public JSONObject toJSON() {
        JSONObject Transaction = super.toJSON();
        JSONArray transactions = new JSONArray();
        for (Transaction transaction : this.transactions) {
            transactions.put(transaction.toJSON());
        }
        Transaction.put("transactions", transactions);
        return Transaction;
    }
}
