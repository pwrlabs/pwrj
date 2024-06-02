package com.github.pwrlabs.pwrj.record.transaction;

import lombok.Getter;
import lombok.experimental.SuperBuilder;
import org.json.JSONObject;

@SuperBuilder
@Getter
public class ChangeFeePerByteProposalTransaction extends Transaction {
    public static final String type = "Change Fee Per Byte Proposal";
    private long feePerByte;
    private String description;

    public ChangeFeePerByteProposalTransaction(JSONObject json, long blockNumber, long timestamp, int positionInTheBlock) {
        super(json, blockNumber, timestamp, positionInTheBlock);
        this.feePerByte = json.optLong("feePerByte" , 8);
        this.description = json.optString("description", "x");
    }

    @Override
    public JSONObject toJSON() {
        JSONObject data = super.toJSON();
        data.put("type", type);
        data.put("feePerByte", feePerByte);
        data.put("description", description);

        return data;
    }
}

