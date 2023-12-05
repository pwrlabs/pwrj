package com.github.pwrlabs.pwrj.Transaction;

public class JoinTxn extends Transaction {
    private final String validator;

    public JoinTxn(int transactionSize, int positionInBlock, long transactionFee, String type, String from, String to, String nonceOrValidationHash, String hash) {
        super(transactionSize, positionInBlock, transactionFee, type, from, to, nonceOrValidationHash, hash);

        this.validator = from;
    }

    //Getters

    /**
     * @return the validator the user is delegating to
     */
    public String getValidator() {
        return validator;
    }
}
