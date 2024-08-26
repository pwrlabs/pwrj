package com.github.pwrlabs.pwrj.record.transaction;

import lombok.Getter;
import lombok.experimental.SuperBuilder;
import org.json.JSONObject;


@SuperBuilder
@Getter
public class ChangeIpTransaction extends Transaction {
    public static final String type = "Validator Change IP";
    private String newIp;

    public ChangeIpTransaction(JSONObject json, long blockNumber, long timestamp, int positionInTheBlock) {
        super(json, blockNumber, timestamp, positionInTheBlock);
        this.newIp = json.optString("newIp", "");
    }

    @Override
    public JSONObject toJSON() {
        JSONObject data = super.toJSON();
        data.put("type", type);
        data.put("newIp", newIp);

        return data;
    }

}
