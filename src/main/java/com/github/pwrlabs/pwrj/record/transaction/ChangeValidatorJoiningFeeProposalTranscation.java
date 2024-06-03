package com.github.pwrlabs.pwrj.record.transaction;

import lombok.Getter;
import lombok.experimental.SuperBuilder;
import org.json.JSONObject;

@SuperBuilder
@Getter
public class ChangeValidatorJoiningFeeProposalTranscation extends Transaction {
    public static final String type = "Change Validator Joining Fee Proposal";
    private long joiningFee;

    private String title; 
    private String description;    public ChangeValidatorJoiningFeeProposalTranscation(JSONObject json, long blockNumber, long timestamp, int positionInTheBlock) {
        super(json, blockNumber, timestamp, positionInTheBlock);
        this.joiningFee = json.optLong("joiningFee", 8);
        this.description = json.optString("description" , "");
        this.title = json.optString("title" , "");
        
    }

    @Override
    public JSONObject toJSON() {
        JSONObject data = super.toJSON();
        data.put("joiningFee", joiningFee);
        data.put("description", description); 
data.put("title", title);

        return data;
    }
}
