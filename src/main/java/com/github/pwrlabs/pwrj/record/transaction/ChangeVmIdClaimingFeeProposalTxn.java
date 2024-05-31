package com.github.pwrlabs.pwrj.record.transaction;

import lombok.Getter;
import lombok.experimental.SuperBuilder;
import org.json.JSONObject;

@SuperBuilder
@Getter
public class ChangeVmIdClaimingFeeProposalTxn extends Transaction {
    public static final String type = "Change VM ID Claiming Fee Proposal";
    private long claimingFee;
    private String description;

    public ChangeVmIdClaimingFeeProposalTxn(JSONObject json, long blockNumber, long timestamp, int positionInTheBlock) {
        super(json, blockNumber, timestamp, positionInTheBlock);
        this.claimingFee = json.optLong("claimingFee", 8);
        this.description = json.optString("description", "x");
    }

    @Override
    public JSONObject toJSON() {
        JSONObject Transaction = super.toJSON();
        Transaction.put("claimingFee", claimingFee);
        Transaction.put("description", description);

        return Transaction;
    }
}
