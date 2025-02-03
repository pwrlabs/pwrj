package com.github.pwrlabs.pwrj.record.transaction.falcon;

import com.github.pwrlabs.pwrj.record.transaction.Interface.Transaction;
import lombok.Getter;
import org.json.JSONObject;

@Getter
public class FalconJoinAsValidator extends Transaction {
    public static final String type = "Falcon Join As Validator";

    private final String ip;

    public FalconJoinAsValidator(JSONObject json, long blockNumber, long timestamp, int positionInTheBlock) {
        super(json, blockNumber, timestamp, positionInTheBlock);

        this.ip = json.getString("ip");
    }

    @Override
    public String getType() {
        return type;
    }
}
