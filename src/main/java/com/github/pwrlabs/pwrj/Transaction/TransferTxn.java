package com.github.pwrlabs.pwrj.Transaction;

public class TransferTxn extends Transaction {
    private final long value;

    public TransferTxn(int transactionSize, int positionInBlock, long transactionFee, String type, String from, String to, String hash, long value) {
        super(transactionSize, positionInBlock, transactionFee, type, from, to, hash);

        this.value = value;
    }

    //Getters

    public long getValue() {
        return value;
    }
}


