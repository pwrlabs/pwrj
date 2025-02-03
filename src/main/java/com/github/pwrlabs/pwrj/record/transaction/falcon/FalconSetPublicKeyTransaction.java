package com.github.pwrlabs.pwrj.record.transaction.falcon;

import com.github.pwrlabs.pwrj.Utils.Hex;
import com.github.pwrlabs.pwrj.record.transaction.Interface.Transaction;
import lombok.Getter;
import org.json.JSONObject;

@Getter
public class FalconSetPublicKeyTransaction extends Transaction {
    public static final String type = "Set Falcon Public Key";

    private final byte[] publicKey;
    //Transaction(JSONObject json, long blockNumber, long timestamp, int positionInTheBlock)
    public FalconSetPublicKeyTransaction(JSONObject json, long blockNumber, long timestamp, int positionInTheBlock) {
        super(json, blockNumber, timestamp, positionInTheBlock);

        String key = json.getString("publicKey");
        if(key.startsWith("0x")) {
            key = key.substring(2);
        }

        this.publicKey = Hex.decode(key);
    }

    @Override
    public String getType() {
        return type;
    }
}
