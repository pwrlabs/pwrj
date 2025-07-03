package com.github.pwrlabs.pwrj.interfaces;


import com.github.pwrlabs.pwrj.entities.FalconTransaction;

@FunctionalInterface
/**
 * VidaTransactionHandler class.
 */
public interface VidaTransactionHandler {
    void processIvaTransactions(FalconTransaction.PayableVidaDataTxn transaction);
}
