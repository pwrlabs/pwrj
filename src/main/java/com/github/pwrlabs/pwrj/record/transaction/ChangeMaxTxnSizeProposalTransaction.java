package com.github.pwrlabs.pwrj.record.transaction;

import lombok.Getter;
import lombok.experimental.SuperBuilder;
import org.json.JSONObject;

@SuperBuilder
@Getter
public class ChangeMaxTxnSizeProposalTransaction extends Transaction {
    public static final String type = "Change Max Txn Size Proposal";
    private int maxTxnSize;
    private String description;

        public ChangeMaxTxnSizeProposalTransaction(JSONObject json, long blockNumber, long timestamp, int positionInTheBlock) {
            super(json, blockNumber, timestamp, positionInTheBlock);
            this.maxTxnSize = json.optInt("maxTxnSize", 8);
            this.description = json.optString("description", "x");
        }

        @Override
        public JSONObject toJSON() {
            JSONObject data = super.toJSON();
            data.put("type", type);
            data.put("maxTxnSize", maxTxnSize);
            data.put("description", description);

            return data;
        }
}
