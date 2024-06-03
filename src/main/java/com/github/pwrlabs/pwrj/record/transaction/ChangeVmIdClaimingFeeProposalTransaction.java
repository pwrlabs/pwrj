package com.github.pwrlabs.pwrj.record.transaction;

import lombok.Getter;
import lombok.experimental.SuperBuilder;
import org.json.JSONObject;

@SuperBuilder
@Getter
public class ChangeVmIdClaimingFeeProposalTransaction extends Transaction {
    public static final String type = "Change Vm Id Claiming Fee Proposal";
    private long claimingFee;

    private String title; 
    private String description;    public ChangeVmIdClaimingFeeProposalTransaction(JSONObject json, long blockNumber, long timestamp, int positionInTheBlock) {
        super(json, blockNumber, timestamp, positionInTheBlock);
        this.claimingFee = json.optLong("claimingFee", 8);
        this.description = json.optString("description" , "");
        this.title = json.optString("title" , "");
    }

    @Override
    public JSONObject toJSON() {
        JSONObject data = super.toJSON();
        data.put("type", type);
        data.put("claimingFee", claimingFee);
        data.put("description", description); 
data.put("title", title);

        return data;
    }
}
