package com.github.pwrlabs.pwrj.record.transaction.ecdsa;

import com.github.pwrlabs.pwrj.record.transaction.Interface.Transaction;
import lombok.Getter;
import lombok.experimental.SuperBuilder;
import org.json.JSONObject;

@SuperBuilder
@Getter
public class MoveStakeTransaction extends Transaction {
    public static final String type = "Move Stake";

    private final String fromValidator;
    private final String toValidator;
    private final long sharesAmount;

    public MoveStakeTransaction(JSONObject json, long blockNumber, long timestamp, int positionInTheBlock) {
        super(json, blockNumber, timestamp, positionInTheBlock);
        this.fromValidator = json.optString("fromValidator", "0x");
        this.toValidator = json.optString("toValidator", "0x");
        this.sharesAmount = json.optLong("sharesAmount", 0);
    }


    public String getType() {
        return type;
    }

    @Override
    public JSONObject toJSON() {
        JSONObject data = super.toJSON();
        data.put("fromValidator", fromValidator);
        data.put("toValidator", toValidator);
        data.put("sharesAmount", sharesAmount);
        return data;
    }
}
