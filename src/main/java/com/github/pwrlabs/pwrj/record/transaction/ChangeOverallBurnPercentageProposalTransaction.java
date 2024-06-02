package com.github.pwrlabs.pwrj.record.transaction;

import lombok.Getter;
import lombok.experimental.SuperBuilder;
import org.json.JSONObject;

@SuperBuilder
@Getter
public class ChangeOverallBurnPercentageProposalTransaction extends Transaction {
    public static final String type = "Change Overall Burn Percentage Proposal";
    private int burnPercentage;
    private String description;

    public ChangeOverallBurnPercentageProposalTransaction(JSONObject json, long blockNumber, long timestamp, int positionInTheBlock) {
        super(json, blockNumber, timestamp, positionInTheBlock);
        this.burnPercentage = json.optInt("burnPercentage", 4);
        this.description = json.optString("description", "x");
    }

    @Override
    public JSONObject toJSON() {
        JSONObject data = super.toJSON();
        data.put("type", type);
        data.put("burnPercentage", burnPercentage);
        data.put("description", description);

        return data;

    }
}