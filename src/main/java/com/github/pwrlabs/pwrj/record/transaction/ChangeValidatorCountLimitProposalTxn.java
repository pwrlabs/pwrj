package com.github.pwrlabs.pwrj.record.transaction;

import lombok.Getter;
import lombok.experimental.SuperBuilder;
import org.json.JSONObject;

@SuperBuilder
@Getter
public class ChangeValidatorCountLimitProposalTxn extends Transaction {
    public static final String type = "Change Validator Count Limit Proposal";
    private int validatorCountLimit;
    private String description;

    public ChangeValidatorCountLimitProposalTxn(JSONObject json, long blockNumber, long timestamp, int positionInTheBlock) {
        super(json, blockNumber, timestamp, positionInTheBlock);
        this.validatorCountLimit = json.optInt("validatorCountLimit", 4);
        this.description = json.optString("description", "x");
    }

    @Override
    public JSONObject toJSON() {
        JSONObject Transaction = super.toJSON();
        Transaction.put("validatorCountLimit", validatorCountLimit);
        Transaction.put("description", description);

        return Transaction;
    }
}
