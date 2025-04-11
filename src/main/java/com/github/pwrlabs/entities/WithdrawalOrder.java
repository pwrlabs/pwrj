package com.github.pwrlabs.entities;

import com.github.pwrlabs.pwrj.Utils.Hex;
import lombok.Getter;
import org.json.JSONObject;

@Getter
public class WithdrawalOrder {
    private final byte[] hash;
    private final byte[] address;
    private final byte[] fromValidator;
    private final long amount;
    private final long timestamp;
    private final long withdrawTime;
    private final boolean complete;

    public WithdrawalOrder(JSONObject object) {
        hash = Hex.decode(object.getString("hash"));
        address = Hex.decode(object.getString("address"));
        fromValidator = Hex.decode(object.getString("fromValidator"));
        amount = object.getLong("amount");
        timestamp = object.getLong("timestamp");
        withdrawTime = object.getLong("withdrawTime");
        complete = object.getBoolean("complete");
    }

    public JSONObject toJson() {
        JSONObject object = new JSONObject();
        object.put("hash", Hex.toHexString(hash));
        object.put("address", Hex.toHexString(address));
        object.put("fromValidator", Hex.toHexString(fromValidator));
        object.put("amount", amount);
        object.put("timestamp", timestamp);
        object.put("withdrawTime", withdrawTime);
        object.put("complete", complete);
        return object;
    }
}
