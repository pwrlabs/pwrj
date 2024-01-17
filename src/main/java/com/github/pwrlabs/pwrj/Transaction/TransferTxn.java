package com.github.pwrlabs.pwrj.Transaction;

import org.json.JSONObject;

public class TransferTxn extends Transaction {
    private final long value;

    public TransferTxn(int transactionSize, long blockNumber, int positionInBlock, long transactionFee, String type, String from, String to, String nonceOrValidationHash, String hash, long timestamp, long value) {
        super(transactionSize, blockNumber, positionInBlock, transactionFee, type, from, to, nonceOrValidationHash, hash, timestamp);

        this.value = value;
    }

    //Getters

    //javadoc of the below function
    /**
     * @return the value of the transaction
     */
    @Override
    public long getValue() {
        return value;
    }

    @Override
    public JSONObject toJSON() {
        JSONObject txn = super.toJSON();
        txn.put("value", value);
        return txn;
    }
}


