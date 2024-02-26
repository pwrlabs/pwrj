package com.github.pwrlabs.pwrj.Transaction;

import lombok.Getter;
import lombok.experimental.SuperBuilder;
import org.json.JSONObject;

@Getter
@SuperBuilder
public class SetGuardianTxn extends Transaction {
    public static final String type = "Set Guardian";

    private String guardian;
    private long expiryDate;

    public SetGuardianTxn(JSONObject json) {
        super(json);
        this.guardian = json.optString("guardian", "0x");
        this.expiryDate = json.optLong("expiryDate", 0);
    }

    @Override
    public JSONObject toJSON() {
        JSONObject txn = super.toJSON();
        txn.put("guardian", guardian);
        txn.put("expiryDate", expiryDate);
        return txn;
    }

}
