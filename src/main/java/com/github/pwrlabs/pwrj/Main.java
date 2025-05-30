package com.github.pwrlabs.pwrj;

import com.github.pwrlabs.pwrj.protocol.PWRJ;
import com.github.pwrlabs.pwrj.record.response.Response;
import com.github.pwrlabs.pwrj.wallet.PWRFalconWallet;
import io.pwrlabs.util.encoders.Hex;
import org.web3j.crypto.Wallet;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Watchable;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

public class Main {

    public static void main(String[] args) throws Exception {
        spamThatBoy();
    }

    public static void spamThatBoy() throws Exception {
        PWRJ pwrj = new PWRJ("https://pwrrpc.pwrlabs.io");
        PWRFalconWallet wallet = new PWRFalconWallet("clarify wink august decrease visit bring glide poverty color turtle crush rocket", pwrj);

        int bytesPerSecond = 5000000;

        int bytesPerWallet = 1000000; // 10kB per wallet
        int walletsCount = bytesPerSecond / bytesPerWallet;
        System.out.println("Creating " + walletsCount + " wallets with " + bytesPerWallet + " bytes each...");
        List<PWRFalconWallet> wallets = new ArrayList<>();
        for (int i = 0; i < walletsCount * 3; ++i) {
            PWRFalconWallet dummyWallet = new PWRFalconWallet(12, pwrj);
            wallets.add(dummyWallet);
            Response r = wallet.transferPWR(dummyWallet.getByteaAddress(), 10000000000L, pwrj.getFeePerByte());
            if (!r.isSuccess()) {
                System.out.println("Transfer failed: " + r.getError());
            } else {
                System.out.println("Transfer successful to " + dummyWallet.getAddress());
            }
        }

        System.out.println("Waiting for wallets to be funded...");
        Thread.sleep(5000);

        System.out.println("Starting vida data submission...");
        AtomicInteger submittedCount = new AtomicInteger(0);
        while (true) {
            for (PWRFalconWallet w : wallets) {
                new Thread(() -> {
                    Response r = null;
                    try {
                        System.out.println("Submitting vida data for wallet: " + w.getAddress());
                        r = w.submitPayableVidaData(getRandomLong(), getRandomBytes(bytesPerWallet), 0, pwrj.getFeePerByte());
                    } catch (IOException e) {
                        e.printStackTrace();
                        throw new RuntimeException(e);
                    } finally {
                        int count = submittedCount.incrementAndGet();
                        System.out.println("Submitted " + count + " wallets so far.");
                    }
                    if (!r.isSuccess()) {
                        System.out.println("Vida data submission failed: " + r.getError());
                    } else {
                        System.out.println("Vida data submitted successfully for wallet: " + w.getAddress());
                    }
                }).start();
            }

            while (submittedCount.get() < walletsCount * 3) {
                Thread.sleep(100); // Wait until all submissions are done
            }

            Thread.sleep(1500);
        }
    }
    public static void delegateToOurBoys() {
        PWRJ pwrj = new PWRJ("https://pwrrpc.pwrlabs.io");

        List<byte[]> addressesToDelegateTo = new ArrayList<>();
        addressesToDelegateTo.add(Hex.decode("186CBF6E8C854469DD27297D07072E2B2C2CD76C")); // Validator 3
        addressesToDelegateTo.add(Hex.decode("F5FE6AE4BA7AA68C1AB340652D243B899859075B")); // 2
        addressesToDelegateTo.add(Hex.decode("8796F287962C5DE43B564F62D67314B7980738FC")); // 1

        PWRFalconWallet wallet = new PWRFalconWallet("clarify wink august decrease visit bring glide poverty color turtle crush rocket", pwrj);
        long amountToDelegate = 400000000000L; //400 PWR

        for (byte[] address : addressesToDelegateTo) {
            try {
                Response response = wallet.delegate(address, amountToDelegate, pwrj.getFeePerByte());
                if (response.isSuccess()) {
                    System.out.println("Delegated " + amountToDelegate + " PWR to " + Hex.toHexString(address));
                } else {
                    System.out.println("Failed to delegate to " + Hex.toHexString(address) + ": " + response.getError());
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

    }
    //get random long
    public static long getRandomLong() {
        return System.currentTimeMillis() + (long) (Math.random() * 1000000);
    }

    //get random x bytes array
    public static byte[] getRandomBytes(int length) {
        byte[] bytes = new byte[length];
        for (int i = 0; i < length; i++) {
            bytes[i] = (byte) (Math.random() * 256);
        }
        return bytes;
    }
}
