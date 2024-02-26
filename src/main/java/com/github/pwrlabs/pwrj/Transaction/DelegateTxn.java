package com.github.pwrlabs.pwrj.Transaction;

import lombok.Getter;
import lombok.experimental.SuperBuilder;
import org.json.JSONObject;

@Getter
@SuperBuilder
public class DelegateTxn extends Transaction {
    public static final String type = "Delegate";

    private final String validator;

    public DelegateTxn(JSONObject json) {
        super(json);
        this.validator = json.optString("validator", "0x");
    }

    @Override
    public JSONObject toJSON() {
        JSONObject txn = super.toJSON();
        txn.put("validator", validator);
        return txn;
    }
}
