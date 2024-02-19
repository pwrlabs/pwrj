package com.github.pwrlabs.pwrj.Transaction;

import org.json.JSONObject;

public class JoinTxn extends Transaction {
    private final String validator;

    public JoinTxn(int transactionSize, long blockNumber, int positionInBlock, long transactionFee, String type, String from, String to, int nonce, String hash, long timestamp) {
        super(transactionSize, blockNumber, positionInBlock, transactionFee, type, from, to, nonce, hash, timestamp);

        this.validator = from;
    }

    //Getters

    /**
     * @return the validator the user is delegating to
     */
    public String getValidator() {
        return validator;
    }

    @Override
    public JSONObject toJSON() {
        JSONObject txn = super.toJSON();
        txn.put("validator", validator);
        return txn;
    }
}
