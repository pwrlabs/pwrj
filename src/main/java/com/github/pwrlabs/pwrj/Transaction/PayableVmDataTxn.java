package com.github.pwrlabs.pwrj.Transaction;

import lombok.Getter;
import lombok.experimental.SuperBuilder;
import org.json.JSONObject;

@Getter
@SuperBuilder
public class PayableVmDataTxn extends Transaction {
    public static String type = "Payable VM Data";

    private long vmId;
    private String data;

    public PayableVmDataTxn(JSONObject json) {
        super(json);
        this.vmId = json.optLong("vmId", 0);
        this.data = json.optString("data", "0x");
    }

    @Override
    public JSONObject toJSON() {
        JSONObject txn = super.toJSON();
        txn.put("vmId", vmId);
        txn.put("data", data);
        return txn;
    }
}
