package com.github.pwrlabs.pwrj.record.transaction.ecdsa;

import com.github.pwrlabs.pwrj.record.transaction.Interface.Transaction;
import lombok.Getter;
import lombok.experimental.SuperBuilder;
import org.json.JSONObject;

@Getter
@SuperBuilder
public class SetGuardianTransaction extends Transaction {
    public static final String type = "Set Guardian";

    private String guardian;
    private long expiryDate;

    public SetGuardianTransaction(JSONObject json, long blockNumber, long timestamp, int positionInTheBlock) {
        super(json, blockNumber, timestamp, positionInTheBlock);
        this.guardian = json.optString("guardian", "0x");
        this.expiryDate = json.optLong("guardianExpiryDate", 0);
    }


    public String getType() {
        return type;
    }

    @Override
    public JSONObject toJSON() {
        JSONObject data = super.toJSON();
        data.put("guardian", guardian);
        data.put("expiryDate", expiryDate);
        return data;
    }

}
