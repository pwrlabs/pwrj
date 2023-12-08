package com.github.pwrlabs.pwrj.Transaction;

public class ClaimVmIdTxn extends Transaction {
    private final long vmId;

    public ClaimVmIdTxn(int transactionSize, int positionInBlock, long transactionFee, String type, String from, String to, String nonceOrValidationHash, String hash, long vmId) {
        super(transactionSize, positionInBlock, transactionFee, type, from, to, nonceOrValidationHash, hash);

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
        return getFrom();
    }
}
