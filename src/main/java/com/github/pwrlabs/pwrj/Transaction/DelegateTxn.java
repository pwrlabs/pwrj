package com.github.pwrlabs.pwrj.Transaction;

import org.json.JSONObject;

public class DelegateTxn extends Transaction {
    private final String validator;
    private final long amount;

    public DelegateTxn(int transactionSize, int positionInBlock, long transactionFee, String type, String from, String to, String nonceOrValidationHash, String hash, long amount) {
        super(transactionSize, positionInBlock, transactionFee, type, from, to, nonceOrValidationHash, hash);

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
