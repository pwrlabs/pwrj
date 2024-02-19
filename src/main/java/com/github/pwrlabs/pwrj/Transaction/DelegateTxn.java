package com.github.pwrlabs.pwrj.Transaction;

import org.json.JSONObject;

public class DelegateTxn extends Transaction {
    private final String validator;
    private final long amount;

    public DelegateTxn(int transactionSize, long blockNumber, int positionInBlock, long transactionFee, String type, String sender, String to, int nonce, String hash, long timestamp, long amount) {
        super(transactionSize, blockNumber, positionInBlock, transactionFee, type, sender, to, nonce, hash, timestamp);

        this.validator = to;
        this.amount = amount;
    }

    //Getters

    /**
     * @return the validator the user is delegating to
     */
    public String getValidator() {
        return validator;
    }

    /**
     * @return the amount of PWR the user is delegating
     */
    public long getAmount() {
        return amount;
    }

    @Override
    public long getValue() {
        return amount;
    }

    @Override
    public JSONObject toJSON() {
        JSONObject txn = super.toJSON();
        txn.put("validator", validator);
        txn.put("amount", amount);
        return txn;
    }
}
