package com.github.pwrlabs.pwrj.Transaction;

import org.json.JSONObject;

public class VmDataTxn extends Transaction {
    private final long vmId;
    private final String data;

    public VmDataTxn(int transactionSize, long blockNumber, int positionInBlock, long transactionFee, String type, String from, String to, String nonceOrValidationHash, String hash, long vmId, String data) {
        super(transactionSize, blockNumber, positionInBlock, transactionFee, type, from, to, nonceOrValidationHash, hash);

        this.vmId = vmId;
        this.data = data;
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
        return txn;
    }
}
