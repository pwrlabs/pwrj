package com.github.pwrlabs.pwrj.record.transaction;

import lombok.Getter;
import lombok.experimental.SuperBuilder;
import org.json.JSONObject;

@Getter
@SuperBuilder
public class ClaimVmIdTransaction extends Transaction {
    public static final String type = "Claim VM ID";

    private final long vmId;

    public ClaimVmIdTransaction(JSONObject json, long blockNumber, long timestamp, int positionInTheBlock) {
        super(json, blockNumber, timestamp, positionInTheBlock);
        this.vmId = json.optLong("vmId", 0);
    }

    @Override
    public JSONObject toJSON() {
        JSONObject data = super.toJSON();
        data.put("vmId", vmId);
        return data;
    }
}
