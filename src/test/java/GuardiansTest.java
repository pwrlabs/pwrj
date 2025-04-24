import com.github.pwrlabs.pwrj.entities.FalconTransaction;
import com.github.pwrlabs.pwrj.protocol.PWRJ;
import com.github.pwrlabs.pwrj.record.response.Response;
import com.github.pwrlabs.pwrj.wallet.PWRFalconWallet;
import io.pwrlabs.util.encoders.BiResult;

import java.io.IOException;
import java.util.List;

public class GuardiansTest {
    private static final PWRJ pwrj = new PWRJ("http://46.101.151.203:8085");

    public static void main(String[] args) {
        System.out.println(Long.MAX_VALUE);
        PWRFalconWallet wallet1 = new PWRFalconWallet(12, pwrj);
        PWRFalconWallet guardianWallet = new PWRFalconWallet(12, pwrj);

        System.out.println("Wallet 1: " + wallet1.getAddress());
        System.out.println("Guardian: " + guardianWallet.getAddress());

        System.out.println("Please supply wallet 1 and guardian with funds and press enter to continue...");
        try {
            System.in.read();
        } catch (Exception e) {
            e.printStackTrace();
        }

        try {
            testSettingGuardian(wallet1, guardianWallet);
            testSendingAGuardedTransaction(wallet1, guardianWallet);
            testRemoveGuardianAddress(wallet1, guardianWallet);
            System.out.println("Test completed successfully");
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
    private static void testSettingGuardian(PWRFalconWallet wallet1, PWRFalconWallet guardianWallet) {
        try {
            long timeNow = System.currentTimeMillis();
            long expiryDate = timeNow + 1000000;
            System.out.println("Time now: " + timeNow);
            System.out.println("ExpiryDate: " + expiryDate);
            Response r = wallet1.setGuardian(expiryDate, guardianWallet.getByteaAddress(), pwrj.getFeePerByte());
            if(!r.isSuccess()) {
                System.out.println("Failed to set guardian: " + r.getError());
            }

            waitUntilTransactionsIsProcessed(r.getTransactionHash());

            BiResult<String, Long> guardian = pwrj.getGuardianOfAddress(wallet1.getAddress());

            if(guardian == null) {
                System.out.println("Failed to set guardian: " + r.getError());
                throw new RuntimeException("Failed to set guardian");
            }

            if(!guardian.getFirst().equalsIgnoreCase(guardianWallet.getAddress())) {
                System.out.println("Unexpected guardian address. Expected: " + guardianWallet.getAddress() + " got: " + guardian.getFirst());
                throw new RuntimeException("Unexpected guardian address. Expected: " + guardianWallet.getAddress() + " got: " + guardian.getFirst());
            }

            if(guardian.getSecond() != expiryDate) {
                System.out.println("Not expected expiry date. Expected: " + expiryDate + ". Got: " + guardian.getSecond() );
                throw new RuntimeException("Not expected expiry date. Expected: " + expiryDate + ". Got: " + guardian.getSecond());
            }

            System.out.println("Guardian set successfully");
        } catch (Exception e) {

        }
    }

    private static void testSendingAGuardedTransaction(PWRFalconWallet wallet1, PWRFalconWallet guardianWallet) throws IOException, InterruptedException {
        PWRFalconWallet receiverWallet = new PWRFalconWallet(12, pwrj);
        long amount = 1000;
        byte[] signedTxn = wallet1.getSignedTransferTransaction(receiverWallet.getByteaAddress(), amount, pwrj.getFeePerByte());

        Response r = guardianWallet.approveAsGuardian(List.of(signedTxn), pwrj.getFeePerByte());
        if(!r.isSuccess()) {
            throw new RuntimeException(r.getError());
        }

        long timeNow = System.currentTimeMillis();
        while (pwrj.getBalanceOfAddress(receiverWallet.getAddress()) == 0 && System.currentTimeMillis() - timeNow < 10000) {
            Thread.sleep(1000);
        }

        if(pwrj.getBalanceOfAddress(receiverWallet.getAddress()) != amount) {
            throw new RuntimeException("Failed to send guarded transaction. Expected: " + amount + ". Got: " + pwrj.getBalanceOfAddress(receiverWallet.getAddress()));
        }
    }

    public static void testRemoveGuardianAddress(PWRFalconWallet wallet1, PWRFalconWallet guardianWallet) throws Exception {
        byte[] txn = wallet1.getSignedRemoveGuardianTransaction(pwrj.getFeePerByte());

        Response r = guardianWallet.approveAsGuardian(List.of(txn), pwrj.getFeePerByte());
        if(!r.isSuccess()) {
            throw new RuntimeException(r.getError());
        }

        waitUntilTransactionsIsProcessed(r.getTransactionHash());

        BiResult<String, Long> result = pwrj.getGuardianOfAddress(wallet1.getAddress());

        if (result != null) {
            throw new RuntimeException("Guardian was not removed");
        }

        System.out.println("Guardian removed succesfully");
    }

    private static void waitUntilTransactionsIsProcessed(String txnHash) throws Exception {
        long maxTime = 5000; // 1 minute
        long timeNow = System.currentTimeMillis();

        while (System.currentTimeMillis() - timeNow < maxTime) {
            try {
                FalconTransaction txn = pwrj.getTransactionByHash(txnHash);
                if(txn != null) return;
            } catch (Exception e) {

            }

            try {
                Thread.sleep(100);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }

        throw new Exception("Transaction not processed in time");
    }
}
