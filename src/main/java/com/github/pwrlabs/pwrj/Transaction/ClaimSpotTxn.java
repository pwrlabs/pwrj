package com.github.pwrlabs.pwrj.Transaction;

import lombok.Getter;
import lombok.experimental.SuperBuilder;
import org.json.JSONObject;

@Getter
@SuperBuilder
public class ClaimSpotTxn extends Transaction {
    public static final String type = "Validator Claim Spot";

    private String validator;

    public ClaimSpotTxn(JSONObject json) {
        super(json);
        this.validator = getSender();
    }

    public JSONObject toJSON() {
        JSONObject txn = super.toJSON();
        txn.put("validator", validator);
        return txn;
    }
}
