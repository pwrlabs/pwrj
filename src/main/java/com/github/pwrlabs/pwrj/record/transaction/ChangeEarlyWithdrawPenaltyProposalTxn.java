package com.github.pwrlabs.pwrj.record.transaction;

import lombok.Getter;
import lombok.experimental.SuperBuilder;
import org.json.JSONObject;

@SuperBuilder
@Getter
public class ChangeEarlyWithdrawPenaltyProposalTxn extends Transaction {
    public static final String type = "Change Early Withdraw Penalty Proposal";
    private long withdrawalPenaltyTime;
    private long withdrawalPenalty;
    private String description;


    public ChangeEarlyWithdrawPenaltyProposalTxn(JSONObject json, long blockNumber, long timestamp, int positionInTheBlock) {
        super(json, blockNumber, timestamp, positionInTheBlock);
        this.withdrawalPenaltyTime = json.optLong("withdrawal_penalty_time" , 8);
        this.withdrawalPenalty = json.optLong("withdrawal_penalty" , 4);
        this.description = json.optString("description" , "x");
    }

    @Override
    public JSONObject toJSON() {
        JSONObject Transaction = super.toJSON();
        Transaction.put("withdrawal_penalty_time", withdrawalPenaltyTime);

        Transaction.put("withdrawal_penalty", withdrawalPenalty);

        Transaction.put("description", description);

        return Transaction;
    }
}
