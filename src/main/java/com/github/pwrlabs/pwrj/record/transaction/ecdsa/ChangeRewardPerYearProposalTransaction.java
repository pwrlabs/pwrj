package com.github.pwrlabs.pwrj.record.transaction.ecdsa;

import com.github.pwrlabs.pwrj.record.transaction.Interface.Transaction;
import lombok.Getter;
import lombok.experimental.SuperBuilder;
import org.json.JSONObject;

@SuperBuilder
@Getter
public class ChangeRewardPerYearProposalTransaction extends Transaction {
    public static final String type = "Change Reward Per Year Proposal";
    private long rewardPerYear;

    private String title;
    private String description;

    public ChangeRewardPerYearProposalTransaction(JSONObject json, long blockNumber, long timestamp, int positionInTheBlock) {
        super(json, blockNumber, timestamp, positionInTheBlock);
        this.rewardPerYear = json.optLong("rewardPerYear", 8);
        this.description = json.optString("description", "");
        this.title = json.optString("title", "");
    }


    @Override
    public String getType() {
        return type;
    }

    public JSONObject toJSON() {
        JSONObject data = super.toJSON();
        data.put("type", type);
        data.put("rewardPerYear", rewardPerYear);
        data.put("description", description);
        data.put("title", title);

        return data;
    }
}
