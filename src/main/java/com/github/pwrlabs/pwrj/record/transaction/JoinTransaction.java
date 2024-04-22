package com.github.pwrlabs.pwrj.record.transaction;

import lombok.Getter;
import lombok.experimental.SuperBuilder;
import org.json.JSONObject;

@Getter
@SuperBuilder
public class JoinTransaction extends Transaction {
    public static final String type = "Validator Join";

    private final String validator;
    private final String ip;

    public JoinTransaction(JSONObject json, long blockNumber, long timestamp, int positionInTheBlock) {
        super(json, blockNumber, timestamp, positionInTheBlock);
        this.validator = json.optString("sender", "0x");
        this.ip = json.optString("ip", "");
    }

    @Override
    public JSONObject toJSON() {
        JSONObject Transaction = super.toJSON();
        Transaction.put("validator", validator);
        Transaction.put("ip", ip);
        return Transaction;
    }
}
