package com.github.pwrlabs.pwrj.Transaction;

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
        this.expiryDate = json.optLong("expiryDate", 0);
    }

    @Override
    public JSONObject toJSON() {
        JSONObject Transaction = super.toJSON();
        Transaction.put("guardian", guardian);
        Transaction.put("expiryDate", expiryDate);
        return Transaction;
    }

}
