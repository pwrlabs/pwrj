package com.github.pwrlabs.pwrj.record.transaction;

import lombok.Getter;
import lombok.experimental.SuperBuilder;
import org.json.JSONObject;

@SuperBuilder
@Getter
public class ChangeMaxBlockSizeProposalTranscation extends Transaction {
    public static final String type = "Change Max Block Size Proposal";
    private int maxBlockSize;
    private String description;

    public ChangeMaxBlockSizeProposalTranscation(JSONObject json, long blockNumber, long timestamp, int positionInTheBlock) {
        super(json, blockNumber, timestamp, positionInTheBlock);
        this.maxBlockSize = json.optInt("maxBlockSize" , 4);
        this.description = json.optString("description", "x");
    }

    @Override
    public JSONObject toJSON() {
        JSONObject data = super.toJSON();
        data.put("type", type);
        data.put("maxBlockSize", maxBlockSize);
        data.put("description", description);

        return data;
    }
}
