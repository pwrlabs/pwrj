package com.github.pwrlabs.pwrj.record.transaction.ecdsa;

import com.github.pwrlabs.pwrj.record.transaction.Interface.Transaction;
import lombok.Getter;
import lombok.experimental.SuperBuilder;
import org.json.JSONObject;

@SuperBuilder
@Getter
public class ChangeFeePerByteProposalTransaction extends Transaction {
    public static final String type = "Change Fee Per Byte Proposal";
    private long feePerByte;

    private String title; 
    private String description;

    public ChangeFeePerByteProposalTransaction(JSONObject json, long blockNumber, long timestamp, int positionInTheBlock) {
        super(json, blockNumber, timestamp, positionInTheBlock);
        this.feePerByte = json.optLong("feePerByte" , 8);
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
        data.put("feePerByte", feePerByte);
        data.put("description", description); 
data.put("title", title);

        return data;
    }
}

