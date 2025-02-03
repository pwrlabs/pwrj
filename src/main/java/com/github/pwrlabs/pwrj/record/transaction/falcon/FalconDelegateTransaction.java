package com.github.pwrlabs.pwrj.record.transaction.falcon;

import com.github.pwrlabs.pwrj.record.transaction.Interface.Transaction;
import lombok.Getter;
import org.json.JSONObject;

@Getter
public class FalconDelegateTransaction extends Transaction {
    public static final String type = "Falcon Delegate";

    private final String validator;
    private final long pwrAmount;
    public FalconDelegateTransaction(JSONObject json, long blockNumber, long timestamp, int positionInTheBlock) {
        super(json, blockNumber, timestamp, positionInTheBlock);

        validator = json.getString("validator");
        pwrAmount = json.getLong("pwrAmount");
    }

    @Override
    public String getType() {
        return type;
    }
}
