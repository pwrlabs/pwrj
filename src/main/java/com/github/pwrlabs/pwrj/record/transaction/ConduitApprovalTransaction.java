package com.github.pwrlabs.pwrj.record.transaction;

import com.github.pwrlabs.pwrj.protocol.TransactionDecoder;
import lombok.Getter;
import lombok.experimental.SuperBuilder;
import org.json.JSONArray;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;

@SuperBuilder
@Getter
public class ConduitApprovalTransaction extends Transaction {
    public static final String type = "Conduit Approval";

    private long vmId;
    private List<Transaction> transactions = new ArrayList<>();

    public ConduitApprovalTransaction(JSONObject json, long blockNumber, long timestamp, int positionInTheBlock) {
        super(json, blockNumber, timestamp, positionInTheBlock);
        this.vmId = json.optLong("vmId", 0);
        JSONArray transactions = json.getJSONArray("transactions");
        if (transactions != null) {
            for (int i = 0; i < transactions.length(); i++) {
                this.transactions.add(Transaction.fromJSON(new JSONObject(transactions.getString(i)), blockNumber, timestamp, positionInTheBlock));
            }
        }
    }
}
