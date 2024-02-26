package com.github.pwrlabs.pwrj.Transaction;

import lombok.Getter;
import lombok.experimental.SuperBuilder;
import org.json.JSONObject;

@Getter
@SuperBuilder
public class RemoveGuardianTxn extends Transaction {
    public static final String type = "Remove Guardian";

    public RemoveGuardianTxn(JSONObject json) {
        super(json);
    }

}
