package com.github.pwrlabs.pwrj.Transaction;

import lombok.Getter;
import lombok.experimental.SuperBuilder;
import org.json.JSONObject;

@Getter
@SuperBuilder
public class WithdrawTransaction extends Transaction {
    public static final String type = "Withdraw";

    private final String validator;
    private final long shares;

    public WithdrawTransaction(JSONObject json, long blockNumber, long timestamp, int positionInTheBlock) {
        super(json, blockNumber, timestamp, positionInTheBlock);
        this.validator = json.optString("0x");
        this.shares = json.optLong("shares", 0);
    }

    @Override
    public JSONObject toJSON() {
        JSONObject Transaction = super.toJSON();
        Transaction.put("validator", validator);
        Transaction.put("shares", shares);
        return Transaction;
    }

}
