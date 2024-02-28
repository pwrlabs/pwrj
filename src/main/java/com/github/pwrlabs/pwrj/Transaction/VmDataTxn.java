package com.github.pwrlabs.pwrj.Transaction;

import lombok.Getter;
import lombok.experimental.SuperBuilder;
import org.json.JSONObject;

@Getter
@SuperBuilder
public class VmDataTxn extends Transaction {
    public static final String type = "VM Data";

    private final long vmId;
    private final String data;

    public VmDataTxn(JSONObject json, long blockNumber, long timestamp, int positionInTheBlock) {
        super(json, blockNumber, timestamp, positionInTheBlock);
        this.vmId = json.getLong("vmId");
        this.data = json.getString("data");
    }

    @Override
    public JSONObject toJSON() {
        JSONObject txn = super.toJSON();
        txn.put("vmId", vmId);
        txn.put("data", data);
        txn.put("type", "VM Data");
        return txn;
    }
}
