package com.github.pwrlabs.pwrj.record.transaction.ecdsa;

import com.github.pwrlabs.pwrj.record.transaction.Interface.Transaction;
import lombok.Getter;
import lombok.experimental.SuperBuilder;
import org.json.JSONObject;

@SuperBuilder
@Getter
public class ChangeMaxBlockSizeProposalTranscation extends Transaction {
    public static final String type = "Change Max Block Size Proposal";
    private int maxBlockSize;

    private String title; 
    private String description;    public ChangeMaxBlockSizeProposalTranscation(JSONObject json, long blockNumber, long timestamp, int positionInTheBlock) {
        super(json, blockNumber, timestamp, positionInTheBlock);
        this.maxBlockSize = json.optInt("maxBlockSize" , 4);
        this.description = json.optString("description" , "");
        this.title = json.optString("title" , "");
    }


    public String getType() {
        return type;
    }

    @Override
    public JSONObject toJSON() {
        JSONObject data = super.toJSON();
        data.put("type", type);
        data.put("maxBlockSize", maxBlockSize);
        data.put("description", description); 
data.put("title", title);

        return data;
    }
}
