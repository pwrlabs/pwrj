package com.github.pwrlabs.pwrj.record.transaction;

import lombok.Getter;
import lombok.experimental.SuperBuilder;
import org.json.JSONObject;

@SuperBuilder
@Getter
public class ChangeRewardPerYearProposalTxn extends Transaction {
    public static final String type = "Change Reward Per Year Proposal";
    private long rewardPerYear;
    private String description;

    public ChangeRewardPerYearProposalTxn(JSONObject json, long blockNumber, long timestamp, int positionInTheBlock) {
        super(json, blockNumber, timestamp, positionInTheBlock);
        this.rewardPerYear = json.optLong("rewardPerYear", 8);
        this.description = json.optString("description", "x");
    }

    public JSONObject toJSON() {
        JSONObject Transaction = super.toJSON();
        Transaction.put("rewardPerYear", rewardPerYear);
        Transaction.put("description", description);

        return Transaction;
    }
}
