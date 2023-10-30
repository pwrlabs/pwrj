package com.github.pwrlabs.pwrj.Transaction;

public class VmDataTxn extends Transaction {
    private final long vmId;
    private final String data;

    public VmDataTxn(int transactionSize, int positionInBlock, long transactionFee, String type, String from, String to, String hash, long vmId, String data) {
        super(transactionSize, positionInBlock, transactionFee, type, from, to, hash);

        this.vmId = vmId;
        this.data = data;
    }

    //Getters

    public long getVmId() {
        return vmId;
    }

    public String getData() {
        return data;
    }
}
