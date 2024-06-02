package com.github.pwrlabs.pwrj.record.transaction;

import lombok.Getter;
import lombok.experimental.SuperBuilder;
import org.json.JSONArray;
import org.json.JSONObject;

@SuperBuilder
@Getter
public class AddConduitsTransaction extends Transaction {
    public static final String type = "Add Conduits";

    private final long vmId;
    private final String[] conduits;

    public AddConduitsTransaction(JSONObject json, long blockNumber, long timestamp, int positionInTheBlock) {
        super(json, blockNumber, timestamp, positionInTheBlock);
        this.vmId = json.optLong("vmId", 0);
        this.conduits = json.getJSONArray("conduits").toList().toArray(new String[0]);
    }

    @Override
    public JSONObject toJSON() {
        JSONObject data = super.toJSON();
        data.put("vmId", vmId);
        data.put("type", type);

        JSONArray conduits = new JSONArray();
        for(String c : this.conduits) {
            conduits.put(c);
        }

        data.put("conduits", conduits);
        return data;
    }
}
