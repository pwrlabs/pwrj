package com.github.pwrlabs.pwrj.record.transaction.ecdsa;

import com.github.pwrlabs.pwrj.record.transaction.Interface.Transaction;
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
        this.data = json.optString("data", "");
    }


    public String getType() {
        return type;
    }

    @Override
    public JSONObject toJSON() {
        JSONObject data = super.toJSON();
        data.put("vmId", vmId);
        data.put("data", this.data);
        data.put("type", "VM Data");
        return data;
    }
}