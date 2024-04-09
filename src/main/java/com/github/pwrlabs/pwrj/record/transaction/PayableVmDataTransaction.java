package com.github.pwrlabs.pwrj.record.transaction;

import lombok.Getter;
import lombok.experimental.SuperBuilder;
import org.json.JSONObject;

@Getter
@SuperBuilder
public class PayableVmDataTransaction extends Transaction {
    public static String type = "Payable VM Data";

    private long vmId;
    private String data;

    public PayableVmDataTransaction(JSONObject json, long blockNumber, long timestamp, int positionInTheBlock) {
        super(json, blockNumber, timestamp, positionInTheBlock);
        this.vmId = json.optLong("vmId", 0);
        this.data = json.optString("data", "0x");
    }

    @Override
    public JSONObject toJSON() {
        JSONObject Transaction = super.toJSON();
        Transaction.put("vmId", vmId);
        Transaction.put("data", data);
        return Transaction;
    }
}
