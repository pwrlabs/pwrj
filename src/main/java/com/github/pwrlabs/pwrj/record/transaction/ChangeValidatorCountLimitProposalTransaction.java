package com.github.pwrlabs.pwrj.record.transaction;

import lombok.Getter;
import lombok.experimental.SuperBuilder;
import org.json.JSONObject;

@SuperBuilder
@Getter
public class ChangeValidatorCountLimitProposalTransaction extends Transaction {
    public static final String type = "Change Validator Count Limit Proposal";
    private int validatorCountLimit;
    private String description;

    public ChangeValidatorCountLimitProposalTransaction(JSONObject json, long blockNumber, long timestamp, int positionInTheBlock) {
        super(json, blockNumber, timestamp, positionInTheBlock);
        this.validatorCountLimit = json.optInt("validatorCountLimit", 4);
        this.description = json.optString("description", "x");
    }

    @Override
    public JSONObject toJSON() {
        JSONObject data = super.toJSON();
        data.put("type", type);
        data.put("validatorCountLimit", validatorCountLimit);
        data.put("description", description);

        return data;
    }
}
