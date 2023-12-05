package com.github.pwrlabs.pwrj.Transaction;

public class DelegateTxn extends Transaction {
    private final String validator;
    private final long amount;

    public DelegateTxn(int transactionSize, int positionInBlock, long transactionFee, String type, String from, String to, String nonceOrValidationHash, String hash, String validator, long amount) {
        super(transactionSize, positionInBlock, transactionFee, type, from, to, nonceOrValidationHash, hash);

        this.validator = validator;
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
}
