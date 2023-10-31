package com.github.pwrlabs.pwrj.Transaction;

public class TransferTxn extends Transaction {
    private final long value;

    public TransferTxn(int transactionSize, int positionInBlock, long transactionFee, String type, String from, String to, String nonceOrValidationHash, String hash, long value) {
        super(transactionSize, positionInBlock, transactionFee, type, from, to, nonceOrValidationHash, hash);

        this.value = value;
    }

    //Getters

    //javadoc of the below function
    /**
     * @return the value of the transaction
     */
    public long getValue() {
        return value;
    }
}


