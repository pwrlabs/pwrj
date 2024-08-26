package com.github.pwrlabs.pwrj.record.transaction;

import lombok.Getter;
import lombok.experimental.SuperBuilder;
import org.json.JSONObject;

@Getter
@SuperBuilder
public class DelegateTransaction extends Transaction {
    public static final String type = "Delegate";

    private final String validator;

    public DelegateTransaction(JSONObject json, long blockNumber, long timestamp, int positionInTheBlock) {
        super(json, blockNumber, timestamp, positionInTheBlock);
        this.validator = json.optString("validator", "0x");
    }

    @Override
    public JSONObject toJSON() {
        JSONObject data = super.toJSON();
        data.put("validator", validator);
        return data;
    }
}
