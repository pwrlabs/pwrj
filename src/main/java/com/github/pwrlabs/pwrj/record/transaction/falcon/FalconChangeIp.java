package com.github.pwrlabs.pwrj.record.transaction.falcon;

import com.github.pwrlabs.pwrj.record.transaction.Interface.Transaction;
import lombok.Getter;
import org.json.JSONObject;

@Getter
public class FalconChangeIp extends Transaction {
    public static final String type = "Falcon Change IP";

    private final String newIp;

    public FalconChangeIp(JSONObject json, long blockNumber, long timestamp, int positionInTheBlock) {
        super(json, blockNumber, timestamp, positionInTheBlock);

        newIp = json.getString("newIp");
    }

    @Override
    public String getType() {
        return type;
    }
}
