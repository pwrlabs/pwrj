package com.github.pwrlabs.pwrj.Transaction;

import lombok.Getter;
import lombok.experimental.SuperBuilder;
import org.json.JSONObject;

@Getter
@SuperBuilder
public class ClaimVmIdTxn extends Transaction {
    private final long vmId;

    public ClaimVmIdTxn(JSONObject json) {
        super(json);
        this.vmId = json.getLong("vmId");
    }

    @Override
    public JSONObject toJSON() {
        JSONObject txn = super.toJSON();
        txn.put("vmId", vmId);
        return txn;
    }
}
