package com.github.pwrlabs.pwrj.interfaces;


import com.github.pwrlabs.entities.FalconTransaction;

@FunctionalInterface
public interface VidaTransactionHandler {
    void processIvaTransactions(FalconTransaction.PayableVidaDataTxn transaction);
}
