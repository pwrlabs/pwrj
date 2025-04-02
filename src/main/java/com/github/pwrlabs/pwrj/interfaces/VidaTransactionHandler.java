package com.github.pwrlabs.pwrj.interfaces;


import com.github.pwrlabs.pwrj.record.transaction.FalconTransaction;

@FunctionalInterface
public interface VidaTransactionHandler {
    void processIvaTransactions(FalconTransaction.PayableVidaDataTxn transaction);
}
