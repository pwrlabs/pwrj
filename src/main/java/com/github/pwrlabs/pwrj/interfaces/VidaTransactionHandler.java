package com.github.pwrlabs.pwrj.interfaces;

import com.github.pwrlabs.pwrj.record.transaction.ecdsa.VmDataTransaction;

@FunctionalInterface
public interface VidaTransactionHandler {
    void processIvaTransactions(VmDataTransaction transaction);
}
