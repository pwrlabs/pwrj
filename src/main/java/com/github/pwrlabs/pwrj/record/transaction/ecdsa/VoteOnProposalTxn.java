package com.github.pwrlabs.pwrj.record.transaction.ecdsa;

import com.github.pwrlabs.pwrj.record.transaction.Interface.Transaction;
import org.json.JSONObject;
import lombok.Getter;
import lombok.experimental.SuperBuilder;

@SuperBuilder
@Getter
public class VoteOnProposalTxn extends Transaction {
    public static final String type = "Vote On Proposal";
    private String proposalHash;
    private int vote;
    private String proposalStatus;

    public VoteOnProposalTxn(JSONObject json, long blockNumber, long timestamp, int positionInTheBlock) {
        super(json, blockNumber, timestamp, positionInTheBlock);
        this.proposalHash = json.optString("proposalHash", "0x");
        this.vote = json.optInt("vote", 0);
        this.proposalStatus = json.optString("proposalStatus", "ongoing");
    }

    public String getType() {
        return type;
    }

    @Override
    public JSONObject toJSON() {
        JSONObject data = super.toJSON();
        data.put("proposalHash", proposalHash);
        data.put("vote", vote);

        return data;
    }

}
