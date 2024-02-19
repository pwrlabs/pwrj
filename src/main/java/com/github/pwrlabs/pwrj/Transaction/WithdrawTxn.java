package com.github.pwrlabs.pwrj.Transaction;

import org.json.JSONObject;

public class WithdrawTxn extends Transaction {
    private final String validator;
    private final long shares;

    public WithdrawTxn(int transactionSize, long blockNumber, int positionInBlock, long transactionFee, String type, String from, String to, int nonce, String hash, long timestamp, long shares) {
        super(transactionSize, blockNumber, positionInBlock, transactionFee, type, from, to, nonce, hash, timestamp);

        this.validator = to;
        this.shares = shares;
    }

    //Getters

    /**
     * @return the validator the user is withdrawing from
     */
    public String getValidator() {
        return validator;
    }

    /**
     * @return the amount of PWR the user is withdrawing
     */
    public long getShares() {
        return shares;
    }

    @Override
    public JSONObject toJSON() {
        JSONObject txn = super.toJSON();
        txn.put("validator", validator);
        txn.put("shares", shares);
        return txn;
    }

}
