package com.github.pwrlabs.pwrj.record.transaction;

import org.json.JSONObject;
import lombok.Getter;
import lombok.experimental.SuperBuilder;

@SuperBuilder
@Getter
public class VoteOnProposalTxn extends Transaction {
    public static final String type = "Vote On Proposal";
    private int proposalId;
    private int vote;

    public VoteOnProposalTxn(JSONObject json, long blockNumber, long timestamp, int positionInTheBlock) {
        super(json, blockNumber, timestamp, positionInTheBlock);
        this.proposalId = json.optInt("proposalId", 1);
        this.vote = json.optInt("vote", 0);
    }

    @Override
    public JSONObject toJSON() {
        JSONObject Transaction = super.toJSON();
        Transaction.put("proposalId", proposalId);
        Transaction.put("vote", vote);

        return Transaction;
    }

}
