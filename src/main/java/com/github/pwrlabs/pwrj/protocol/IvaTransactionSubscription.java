package com.github.pwrlabs.pwrj.protocol;

import com.github.pwrlabs.pwrj.interfaces.IvaTransactionHandler;
import com.github.pwrlabs.pwrj.record.transaction.ecdsa.VmDataTransaction;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.atomic.AtomicBoolean;

public class IvaTransactionSubscription {
    private static final Logger logger = LoggerFactory.getLogger(IvaTransactionSubscription.class);

    private PWRJ pwrj;
    private long vmId;
    private long startingBlock;
    private long latestCheckedBlock;
    private IvaTransactionHandler handler;

    AtomicBoolean pause = new AtomicBoolean(false), stop = new AtomicBoolean(false);

    public IvaTransactionSubscription(PWRJ pwrj, long vmId, long startingBlock, IvaTransactionHandler handler, long pollInterval) {
        this.pwrj = pwrj;
        this.vmId = vmId;
        this.startingBlock = startingBlock;
        this.handler = handler;
    }

    AtomicBoolean running = new AtomicBoolean(false);
    public synchronized void start() {
        if (running.get()) {
            logger.error("IvaTransactionSubscription is already running");
            return;
        } else {
            running.set(true);
            pause.set(false);
            stop.set(false);
        }

        long startingBlock = this.startingBlock;
        Thread thread = new Thread(() -> {
            while (true && !stop.get()) {
                if(pause.get()) continue;
                try {
                    long latestBlock = pwrj.getLatestBlockNumber();
                    long maxBlockToCheck = Math.min(latestBlock, startingBlock + 1000);

                    VmDataTransaction[] transactions = pwrj.getVMDataTransactions(startingBlock, maxBlockToCheck, vmId);

                    for (VmDataTransaction transaction : transactions) {
                        handler.processIvaTransactions(transaction);
                    }

                    latestCheckedBlock = maxBlockToCheck;
                } catch (Exception e) {
                    e.printStackTrace();
                    logger.error("Failed to fetch and process VM data transactions: " + e.getMessage());
                    logger.error("Fetching and processing VM data transactions has stopped");
                    break;
                } finally {
                    try { Thread.sleep(100); } catch (Exception e) {}
                }
            }

            running.set(false);
        });

        thread.setName("IvaTransactionSubscription:IVA-ID-" + vmId);
        thread.start();
    }

    public void pause() {
        pause.set(true);
    }

    public void resume() {
        pause.set(false);
    }

    public void stop() {
        stop.set(true);
    }

    public boolean isRunning() {
        return running.get();
    }

    public boolean isPaused() {
        return pause.get();
    }

    public boolean isStopped() {
        return stop.get();
    }

    public long getLatestCheckedBlock() {
        return latestCheckedBlock;
    }

    public long getStartingBlock() {
        return startingBlock;
    }

    public long getVmId() {
        return vmId;
    }

    public IvaTransactionHandler getHandler() {
        return handler;
    }

    public PWRJ getPwrj() {
        return pwrj;
    }

}
