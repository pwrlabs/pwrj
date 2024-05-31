package com.github.pwrlabs.pwrj.record.transaction;

import lombok.Getter;
import lombok.experimental.SuperBuilder;
import org.json.JSONObject;

@SuperBuilder
@Getter
public class ChangeValidatorJoiningFeeProposalTxn extends Transaction {
    public static final String type = "Change Validator Joining Fee Proposal";
    private long joiningFee;
    private String description;

    public ChangeValidatorJoiningFeeProposalTxn(JSONObject json, long blockNumber, long timestamp, int positionInTheBlock) {
        super(json, blockNumber, timestamp, positionInTheBlock);
        this.joiningFee = json.optLong("joiningFee", 8);
        this.description = json.optString("description", "x");
    }

    @Override
    public JSONObject toJSON() {
        JSONObject Transaction = super.toJSON();
        Transaction.put("joiningFee", joiningFee);
        Transaction.put("description", description);

        return Transaction;
    }
}
