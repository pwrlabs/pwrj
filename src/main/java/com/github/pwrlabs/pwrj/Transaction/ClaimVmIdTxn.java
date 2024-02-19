package com.github.pwrlabs.pwrj.Transaction;

import org.json.JSONObject;

public class ClaimVmIdTxn extends Transaction {
    private final long vmId;

    public ClaimVmIdTxn(int transactionSize, long blockNumber, int positionInBlock, long transactionFee, String type, String sender, String to, int nonce, String hash, long timestamp, long vmId) {
        super(transactionSize, blockNumber, positionInBlock, transactionFee, type, sender, to, nonce, hash, timestamp);

        this.vmId = vmId;
    }

    //Getters

    /**
     * @return the vmId the user is claiming
     */
    public long getVmId() {
        return vmId;
    }

    /**
     * @return the owner of the vmId
     */
    public String getOwner() {
        return getSender();
    }

    @Override
    public JSONObject toJSON() {
        JSONObject txn = super.toJSON();
        txn.put("vmId", vmId);
        return txn;
    }
}
