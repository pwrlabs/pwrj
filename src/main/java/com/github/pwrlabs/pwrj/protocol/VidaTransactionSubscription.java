package com.github.pwrlabs.pwrj.protocol;

import com.github.pwrlabs.pwrj.interfaces.VidaTransactionHandler;
import com.github.pwrlabs.pwrj.entities.FalconTransaction;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.Function;

/**
 * VidaTransactionSubscription class.
 */
public class VidaTransactionSubscription {
    private static final Logger logger = LoggerFactory.getLogger(VidaTransactionSubscription.class);

    private PWRJ pwrj;
    private long vidaId;
    private long startingBlock;
    private long pollInterval;
    private AtomicLong latestCheckedBlock = new AtomicLong(1);
    private VidaTransactionHandler handler;
    private final Function<Long /*Block Number*/, Void> blockSaver; //Used to save the latest block number to a database or file

    AtomicBoolean wantsToPause = new AtomicBoolean(false), stop = new AtomicBoolean(false);
    AtomicBoolean paused = new AtomicBoolean(false);

    public VidaTransactionSubscription(PWRJ pwrj, long vidaId, long startingBlock, VidaTransactionHandler handler, long pollInterval, Function<Long, Void> blockSaver) {
        this.pwrj = pwrj;
        this.vidaId = vidaId;
        this.startingBlock = startingBlock;
        this.pollInterval = pollInterval;
        this.handler = handler;
        this.blockSaver = blockSaver;

        //add shutdown hook
        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            logger.info("Shutting down VidaTransactionSubscription for VIDA-ID: " + vidaId);
            pause();
            logger.info("VidaTransactionSubscription for VIDA-ID: " + vidaId + " has been stopped.");
        }));
    }

    AtomicBoolean running = new AtomicBoolean(false);

/**
 * start method.
 */
    public synchronized void start() {
        if (running.get()) {
            logger.error("IvaTransactionSubscription is already running");
            return;
        } else {
            running.set(true);
            wantsToPause.set(false);
            stop.set(false);
        }

        latestCheckedBlock.set(this.startingBlock - 1);
        Thread thread = new Thread(() -> {
            while (true && !stop.get()) {
                if(wantsToPause.get()) {
                    if(!paused.get()) paused.set(true);
                    continue;
                }
                try {
                    long latestBlock = pwrj.getLatestBlockNumber();
                    if(latestBlock == latestCheckedBlock.get()) continue;

                    long maxBlockToCheck = Math.min(latestBlock, latestCheckedBlock.get() + 1000);

                    FalconTransaction.PayableVidaDataTxn[] transactions = pwrj.getVidaDataTransactions(latestCheckedBlock.get() + 1, maxBlockToCheck, vidaId);

                    for (FalconTransaction.PayableVidaDataTxn transaction : transactions) {
                        try {
                            handler.processIvaTransactions(transaction);
                        } catch (Exception e) {
                            logger.error("Failed to process VIDA transaction: " + transaction.getTransactionHash() + " - " + e.getMessage());
                            e.printStackTrace();
                        }
                    }

                    latestCheckedBlock.set(maxBlockToCheck);
                    if(blockSaver != null) {
                        try {
                            blockSaver.apply(latestCheckedBlock.get());
                        } catch (Exception e) {
                            logger.error("Failed to save latest checked block: " + latestCheckedBlock + " - " + e.getMessage());
                            e.printStackTrace();
                        }
                    }
                } catch (IOException e) {
                    logger.error("Failed to fetch VIDA transactions: " + e.getMessage());
                    e.printStackTrace();
                } catch (Exception e) {
                    logger.error("Unexpected error in VidaTransactionSubscription: " + e.getMessage());
                    e.printStackTrace();
                } finally {
                    try { Thread.sleep(pollInterval); } catch (Exception e) {}
                }
            }

            running.set(false);
        });

        thread.setName("IvaTransactionSubscription:IVA-ID-" + vidaId);
        thread.start();
    }

/**
 * setLatestCheckedBlock method.
 * @param blockNumber parameter
 */
    public void setLatestCheckedBlock(long blockNumber) {
        latestCheckedBlock.set(blockNumber);
    }

/**
 * pause method.
 */
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

/**
 * resume method.
 */
    public void resume() {
        wantsToPause.set(false);
    }

/**
 * stop method.
 */
    public void stop() {
        pause();
        stop.set(true);
    }

/**
 * isRunning method.
 * @return value
 */
    public boolean isRunning() {
        return running.get();
    }

/**
 * isPaused method.
 * @return value
 */
    public boolean isPaused() {
        return wantsToPause.get();
    }

/**
 * isStopped method.
 * @return value
 */
    public boolean isStopped() {
        return stop.get();
    }

/**
 * getLatestCheckedBlock method.
 * @return value
 */
    public long getLatestCheckedBlock() {
        return latestCheckedBlock.get();
    }

/**
 * getStartingBlock method.
 * @return value
 */
    public long getStartingBlock() {
        return startingBlock;
    }

/**
 * getVidaId method.
 * @return value
 */
    public long getVidaId() {
        return vidaId;
    }

/**
 * getHandler method.
 * @return value
 */
    public VidaTransactionHandler getHandler() {
        return handler;
    }

/**
 * getPwrj method.
 * @return value
 */
    public PWRJ getPwrj() {
        return pwrj;
    }

}
