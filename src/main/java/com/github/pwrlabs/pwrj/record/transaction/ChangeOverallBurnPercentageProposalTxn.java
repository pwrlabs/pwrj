package com.github.pwrlabs.pwrj.record.transaction;

import lombok.Getter;
import lombok.experimental.SuperBuilder;
import org.json.JSONObject;

@SuperBuilder
@Getter
public class ChangeOverallBurnPercentageProposalTxn extends Transaction {
    public static final String type = "Change Overall Percentage Burn Proposal";
    private int burnPercentage;
    private String description;

    public ChangeOverallBurnPercentageProposalTxn(JSONObject json, long blockNumber, long timestamp, int positionInTheBlock) {
        super(json, blockNumber, timestamp, positionInTheBlock);
        this.burnPercentage = json.optInt("burnPercentage", 4);
        this.description = json.optString("description", "x");
    }

    @Override
    public JSONObject toJSON() {
        JSONObject Transaction = super.toJSON();
        Transaction.put("burnPercentage", burnPercentage);
        Transaction.put("description", description);

        return Transaction;

    }
}
