package com.github.pwrlabs.pwrj.record.transaction.falcon;

import com.github.pwrlabs.pwrj.record.transaction.Interface.Transaction;
import org.json.JSONObject;

public class FalconTransferTransaction extends Transaction {
    public static final String type = "Falcon Transfer";

    //Transaction(JSONObject json, long blockNumber, long timestamp, int positionInTheBlock)
    public FalconTransferTransaction(JSONObject json, long blockNumber, long timestamp, int positionInTheBlock) {
        super(json, blockNumber, timestamp, positionInTheBlock);
    }

    @Override
    public String getType() {
        return type;
    }
}
