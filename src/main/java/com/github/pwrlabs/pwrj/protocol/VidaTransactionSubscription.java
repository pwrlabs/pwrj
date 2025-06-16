package com.github.pwrlabs.pwrj.protocol;

import com.github.pwrlabs.pwrj.interfaces.VidaTransactionHandler;
import com.github.pwrlabs.pwrj.entities.FalconTransaction;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.atomic.AtomicBoolean;

public class VidaTransactionSubscription {
    private static final Logger logger = LoggerFactory.getLogger(VidaTransactionSubscription.class);

    private PWRJ pwrj;
    private long vidaId;
    private long startingBlock;
    private long latestCheckedBlock;
    private VidaTransactionHandler handler;

    AtomicBoolean wantsToPause = new AtomicBoolean(false), stop = new AtomicBoolean(false);
    AtomicBoolean paused = new AtomicBoolean(false);

    public VidaTransactionSubscription(PWRJ pwrj, long vidaId, long startingBlock, VidaTransactionHandler handler, long pollInterval) {
        this.pwrj = pwrj;
        this.vidaId = vidaId;
        this.startingBlock = startingBlock;
        this.handler = handler;

        //add shutdown hook
        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            logger.info("Shutting down VidaTransactionSubscription for VIDA-ID: " + vidaId);
            pause();
            logger.info("VidaTransactionSubscription for VIDA-ID: " + vidaId + " has been stopped.");
        }));
    }

    AtomicBoolean running = new AtomicBoolean(false);
    public synchronized void start() {
        if (running.get()) {
            logger.error("IvaTransactionSubscription is already running");
            return;
        } else {
            running.set(true);
            wantsToPause.set(false);
            stop.set(false);
        }

        latestCheckedBlock = this.startingBlock - 1;
        Thread thread = new Thread(() -> {
            while (true && !stop.get()) {
                if(wantsToPause.get()) {
                    if(!paused.get()) paused.set(true);
                    continue;
                }
                try {
                    long latestBlock = pwrj.getLatestBlockNumber();
                    if(latestBlock == latestCheckedBlock) continue;

                    long maxBlockToCheck = Math.min(latestBlock, latestCheckedBlock + 1000);

                    FalconTransaction.PayableVidaDataTxn[] transactions = pwrj.getVidaDataTransactions(latestCheckedBlock + 1, maxBlockToCheck, vidaId);

                    for (FalconTransaction.PayableVidaDataTxn transaction : transactions) {
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

        thread.setName("IvaTransactionSubscription:IVA-ID-" + vidaId);
        thread.start();
    }

    public void pause() {
        wantsToPause.set(true);

        while (!paused.get()) {
            try {
                Thread.sleep(10); // Wait until paused is set to false
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt(); // Restore interrupted status
            }
        }
    }

    public void resume() {
        wantsToPause.set(false);
    }

    public void stop() {
        pause();
        stop.set(true);
    }

    public boolean isRunning() {
        return running.get();
    }

    public boolean isPaused() {
        return wantsToPause.get();
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

    public long getVidaId() {
        return vidaId;
    }

    public VidaTransactionHandler getHandler() {
        return handler;
    }

    public PWRJ getPwrj() {
        return pwrj;
    }

}
