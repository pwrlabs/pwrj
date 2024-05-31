package com.github.pwrlabs.pwrj.record.transaction;

import lombok.Getter;
import lombok.experimental.SuperBuilder;
import org.json.JSONObject;

@SuperBuilder
@Getter
public class ChangeMaxTxnSizeProposalTxn extends Transaction{
    public static final String type = "Change Max Transaction Size Proposal";
    private int maxTxnSize;
    private String description;

        public ChangeMaxTxnSizeProposalTxn(JSONObject json, long blockNumber, long timestamp, int positionInTheBlock) {
            super(json, blockNumber, timestamp, positionInTheBlock);
            this.maxTxnSize = json.optInt("maxTxnSize", 8);
            this.description = json.optString("description", "x");
        }

        @Override
        public JSONObject toJSON() {
            JSONObject Transaction = super.toJSON();
            Transaction.put("maxTxnSize", maxTxnSize);
            Transaction.put("description", description);

            return Transaction;
        }
}
