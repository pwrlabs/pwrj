package com.github.pwrlabs.pwrj.Transaction;

public class WithdrawTxn extends Transaction {
    private final String validator;
    private final long shares;

    public WithdrawTxn(int transactionSize, int positionInBlock, long transactionFee, String type, String from, String to, String nonceOrValidationHash, String hash, long shares) {
        super(transactionSize, positionInBlock, transactionFee, type, from, to, nonceOrValidationHash, hash);

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

}
