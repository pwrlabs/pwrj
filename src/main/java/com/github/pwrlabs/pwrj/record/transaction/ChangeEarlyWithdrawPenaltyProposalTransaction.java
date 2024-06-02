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
    private String description;


    public ChangeEarlyWithdrawPenaltyProposalTransaction(JSONObject json, long blockNumber, long timestamp, int positionInTheBlock) {
        super(json, blockNumber, timestamp, positionInTheBlock);
        this.withdrawalPenaltyTime = json.optLong("withdrawal_penalty_time" , 8);
        this.withdrawalPenalty = json.optLong("withdrawal_penalty" , 4);
        this.description = json.optString("description" , "x");
    }

    @Override
    public JSONObject toJSON() {
        JSONObject data = super.toJSON();
        data.put("type", type);
        data.put("withdrawal_penalty_time", withdrawalPenaltyTime);

        data.put("withdrawal_penalty", withdrawalPenalty);

        data.put("description", description);

        return data;
    }
}
