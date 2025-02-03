package com.github.pwrlabs.pwrj.record.transaction.ecdsa;

import com.github.pwrlabs.pwrj.record.transaction.Interface.Transaction;
import lombok.Getter;
import lombok.experimental.SuperBuilder;
import org.json.JSONObject;

@SuperBuilder
@Getter
public class ChangeVmIdClaimingFeeProposalTransaction extends Transaction {
    public static final String type = "Change Vm Id Claiming Fee Proposal";
    private long claimingFee;

    private String title; 
    private String description;    public ChangeVmIdClaimingFeeProposalTransaction(JSONObject json, long blockNumber, long timestamp, int positionInTheBlock) {
        super(json, blockNumber, timestamp, positionInTheBlock);
        this.claimingFee = json.optLong("vmIdClaimingFee", 8);
        this.description = json.optString("description" , "");
        this.title = json.optString("title" , "");
    }


    public String getType() {
        return type;
    }

    @Override
    public JSONObject toJSON() {
        JSONObject data = super.toJSON();
        data.put("type", type);
        data.put("claimingFee", claimingFee);
        data.put("description", description); 
data.put("title", title);

        return data;
    }
}
