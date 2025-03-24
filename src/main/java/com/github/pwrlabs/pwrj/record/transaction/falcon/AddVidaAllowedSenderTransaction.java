package com.github.pwrlabs.pwrj.record.transaction.falcon;

import com.github.pwrlabs.pwrj.Utils.Hex;
import com.github.pwrlabs.pwrj.record.transaction.Interface.Transaction;
import org.json.JSONArray;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;

public class AddVidaAllowedSenderTransaction extends Transaction {
    private final List<byte[]> newAllowedSenders = new ArrayList<>();

    public AddVidaAllowedSenderTransaction(JSONObject json, long blockNumber, long timestamp, int positionInTheBlock) {
        super(json, blockNumber, timestamp, positionInTheBlock);

        JSONArray newAllowedSenders = json.getJSONArray("newAllowedSenders")
    }

}
