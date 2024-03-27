package com.github.pwrlabs.pwrj.Transaction;

import lombok.Getter;
import lombok.experimental.SuperBuilder;
import org.json.JSONObject;

@Getter
@SuperBuilder
public class VmDataTransaction extends Transaction {
    public static final String type = "VM Data";

    private final long vmId;
    private final String data;

    public VmDataTransaction(JSONObject json, long blockNumber, long timestamp, int positionInTheBlock) {
        super(json, blockNumber, timestamp, positionInTheBlock);
        this.vmId = json.getLong("vmId");
        this.data = json.getString("data");
    }

    @Override
    public JSONObject toJSON() {
        JSONObject Transaction = super.toJSON();
        Transaction.put("vmId", vmId);
        Transaction.put("data", data);
        Transaction.put("type", "VM Data");
        return Transaction;
    }
}
