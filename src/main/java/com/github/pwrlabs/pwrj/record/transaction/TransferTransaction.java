package com.github.pwrlabs.pwrj.record.transaction;

import lombok.Getter;
import lombok.experimental.SuperBuilder;
import org.json.JSONObject;
@Getter
@SuperBuilder
public class TransferTransaction extends Transaction {

    public static final String type = "Transfer";

    public TransferTransaction(JSONObject json, long blockNumber, long timestamp, int positionInTheBlock) {
        super(json, blockNumber, timestamp, positionInTheBlock);
    }
}


