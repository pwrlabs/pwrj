package com.github.pwrlabs.pwrj.Transaction;

import org.json.JSONObject;

public class VmDataTxn extends Transaction {
    private final long vmId;
    private final String data;

    public VmDataTxn(int transactionSize, long blockNumber, int positionInBlock, long transactionFee, String type, String from, String to, String nonceOrValidationHash, String hash, long timestamp, long vmId, String data) {
        super(transactionSize, blockNumber, positionInBlock, transactionFee, type, from, to, nonceOrValidationHash, hash, timestamp);

        this.vmId = vmId;
        this.data = data;
    }

    public VmDataTxn(JSONObject txnObject) {
        super(txnObject.getInt("size"), txnObject.getLong("blockNumber"), txnObject.getInt("positionInTheBlock"), txnObject.getLong("fee"), txnObject.getString("type"), txnObject.getString("from"), txnObject.getString("to"), txnObject.getString("nonceOrValidationHash"), txnObject.getString("hash"), txnObject.getLong("timestamp"));

        this.vmId = txnObject.getLong("vmId");
        this.data = txnObject.getString("data");
    }

    //Getters

    /**
     * @return the id of the VM that this transaction is for
     */
    public long getVmId() {
        return vmId;
    }

    /**
     * @return the data of the transaction
     */
    public String getData() {
        return data;
    }

    @Override
    public JSONObject toJSON() {
        JSONObject txn = super.toJSON();
        txn.put("vmId", vmId);
        txn.put("data", data);
        txn.put("type", "VM Data");
        txn.put("size", getSize());
        txn.put("blockNumber", getBlockNumber());
        txn.put("positionInTheBlock", getPositionInTheBlock());
        txn.put("fee", getFee());
        txn.put("from", getFrom());
        txn.put("to", getTo());
        txn.put("nonceOrValidationHash", getNonceOrValidationHash());
        txn.put("hash", getHash());
        txn.put("timestamp", getTimestamp());
        return txn;
    }
}
