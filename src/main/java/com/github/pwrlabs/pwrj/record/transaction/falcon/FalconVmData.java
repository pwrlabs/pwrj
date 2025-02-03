package com.github.pwrlabs.pwrj.record.transaction.falcon;

import com.github.pwrlabs.pwrj.Utils.Hex;
import com.github.pwrlabs.pwrj.record.transaction.Interface.Transaction;
import lombok.Getter;
import org.json.JSONObject;

@Getter
public class FalconVmData extends Transaction {
    public static final String type = "Falcon VM Data";

    private final long vmId;
    private final byte[] data;

    //Transaction(JSONObject json, long blockNumber, long timestamp, int positionInTheBlock)
    public FalconVmData(JSONObject json, long blockNumber, long timestamp, int positionInTheBlock) {
        super(json, blockNumber, timestamp, positionInTheBlock);

        vmId = json.getLong("vmId");
        String dataStr = json.getString("data");
        if(dataStr.startsWith("0x")) dataStr = dataStr.substring(2);

        data = Hex.decode(dataStr);
    }

    @Override
    public String getType() {
        return type;
    }
}
