package com.github.pwrlabs.pwrj.record.transaction;

import lombok.Getter;
import lombok.experimental.SuperBuilder;
import org.json.JSONObject;

@SuperBuilder
@Getter
public class ChangeEarlyWithdrawPenaltyProposalTransaction extends Transaction {
    public static final String type = "Change Early Withdraw Penalty Proposal";
    private long withdrawalPenaltyTime;
    private long withdrawalPenalty;

    private String title; 
    private String description;
    public ChangeEarlyWithdrawPenaltyProposalTransaction(JSONObject json, long blockNumber, long timestamp, int positionInTheBlock) {
        super(json, blockNumber, timestamp, positionInTheBlock);
        this.withdrawalPenaltyTime = json.optLong("earlyWithdrawTime" , 8);
        this.withdrawalPenalty = json.optLong("earlyWithdrawPenalty" , 4);
        this.description = json.optString("description" , "");
        this.title = json.optString("title" , "");
        this.title = json.optString("title" , "");
    }

    @Override
    public JSONObject toJSON() {
        JSONObject data = super.toJSON();
        data.put("type", type);
        data.put("withdrawal_penalty_time", withdrawalPenaltyTime);

        data.put("withdrawal_penalty", withdrawalPenalty);
        
        data.put("description", description); 
data.put("title", title);

        return data;
    }
}
