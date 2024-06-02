package com.github.pwrlabs.pwrj.record.transaction;
import lombok.Getter;
import lombok.experimental.SuperBuilder;
import org.json.JSONObject;

@SuperBuilder
@Getter
public class ChangeVmOwnerTxnFeeShareProposalTransaction extends Transaction {
    public static final String type = "Change VM Owner Txn Fee Share Proposal";
    private long feeShare;
    private String description;

        public ChangeVmOwnerTxnFeeShareProposalTransaction(JSONObject json, long blockNumber, long timestamp, int positionInTheBlock) {
            super(json, blockNumber, timestamp, positionInTheBlock);
            this.feeShare = json.optLong("feeShare", 8);
            this.description = json.optString("description", "x");
        }

        @Override
        public JSONObject toJSON() {
            JSONObject data = super.toJSON();
            data.put("type", type);
            data.put("feeShare", feeShare);
            data.put("description", description);

            return data;
        }
}
