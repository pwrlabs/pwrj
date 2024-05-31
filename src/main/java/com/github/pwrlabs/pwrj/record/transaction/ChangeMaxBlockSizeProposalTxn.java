package com.github.pwrlabs.pwrj.record.transaction;

import lombok.Getter;
import lombok.experimental.SuperBuilder;
import org.json.JSONObject;

@SuperBuilder
@Getter
public class ChangeMaxBlockSizeProposalTxn extends Transaction {
    public static final String type = "Change Max Block Size Proposal";
    private int maxBlockSize;
    private String description;

    public ChangeMaxBlockSizeProposalTxn(JSONObject json, long blockNumber, long timestamp, int positionInTheBlock) {
        super(json, blockNumber, timestamp, positionInTheBlock);
        this.maxBlockSize = json.optInt("maxBlockSize" , 4);
        this.description = json.optString("description", "x");
    }

    @Override
    public JSONObject toJSON() {
        JSONObject Transaction = super.toJSON();
        Transaction.put("maxBlockSize", maxBlockSize);
        Transaction.put("description", description);

        return Transaction;
    }
}
