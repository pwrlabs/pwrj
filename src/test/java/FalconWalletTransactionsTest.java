import com.github.pwrlabs.pwrj.Utils.Hex;
import com.github.pwrlabs.pwrj.protocol.PWRJ;
import com.github.pwrlabs.pwrj.record.response.Response;
import com.github.pwrlabs.pwrj.wallet.PWRFalconWallet;

import java.io.IOException;
import java.util.Random;

import static com.github.pwrlabs.pwrj.Utils.NewError.errorIf;

//this class is for testing all transactions on PWR Chain
//It's focused on testing the transactions and the soundness of their execution
public class FalconWalletTransactionsTest {
    private static final PWRJ pwrj = new PWRJ("http://localhost:8085");
    private static final int SLEEP_TIME_AFTER_SENDING_TXNS = 10000;

    public static void main(String[] args) {
        PWRFalconWallet wallet1 = new PWRFalconWallet(pwrj);
        PWRFalconWallet wallet2 = new PWRFalconWallet(pwrj);

        byte[] wallet2Address = Hex.decode(wallet2.getAddress().startsWith("0x") ? wallet2.getAddress().substring(2) : wallet2.getAddress());

        System.out.println("Wallet 1: " + wallet1.getAddress());
        System.out.println("Wallet 2: " + wallet2.getAddress());

        // Wait until user clicks enter
        System.out.println("Please supply wallet 1 with funds and press enter to continue...");
        try {
            System.in.read();
        } catch (Exception e) {
            e.printStackTrace();
        }

        try {
            testTransfer(wallet1, wallet2Address, 1000000);
        } catch (IOException | InterruptedException e) {
            e.printStackTrace();
        }
    }

    private static void testTransfer(PWRFalconWallet sender, byte[] receiver, long amount) throws IOException, InterruptedException {
        System.out.println("Transfering " + amount + " from " + sender.getAddress() + " to " + Hex.toHexString(receiver));

        long senderNonce = pwrj.getNonceOfAddress(sender.getAddress());
        if(senderNonce == 0) ++senderNonce; //We increase the none because if it is zero then 2 txns will be sent (set public key first)
        long senderBalanceBefore = pwrj.getBalanceOfAddress(sender.getAddress());
        long receiverBalanceBefore = pwrj.getBalanceOfAddress(Hex.toHexString(receiver));

        try {
            Response r = sender.transferPWR(receiver, amount, pwrj.getFeePerByte());
            errorIf(!r.isSuccess(), "Transfer failed: " + r.getError());
        } catch (Exception e) {
            e.printStackTrace();
            System.exit(0);
        }

        Thread.sleep(SLEEP_TIME_AFTER_SENDING_TXNS);

        long senderBalanceAfter = pwrj.getBalanceOfAddress(sender.getAddress());
        long receiverBalanceAfter = pwrj.getBalanceOfAddress(Hex.toHexString(receiver));

        if(senderBalanceAfter > senderBalanceBefore - amount) {
            System.err.println("Sender balance incorrect");
            System.out.println("Expected: " + (senderBalanceBefore - amount) + " and less but got: " + senderBalanceAfter);
            errorIf(true, "Sender balance incorrect");
        }

        if(receiverBalanceAfter < receiverBalanceBefore + amount) {
            System.err.println("Receiver balance incorrect");
            System.out.println("Expected: " + (receiverBalanceBefore + amount) + " and more but got: " + receiverBalanceAfter);
            errorIf(true, "Receiver balance incorrect");
        }

        System.out.println("Transfer successful");
    }

    private static void testVidaDataTxm(PWRFalconWallet sender) throws Exception {
        byte[] data = generateRandomBytes(230);
        long vidaId = new Random().nextLong();
        long value = 100;
        long blockNow = pwrj.getBlockNumber();

        Response r = sender.submitPayableVidaData(vidaId, data, 1000000, pwrj.getFeePerByte());
        errorIf(!r.isSuccess(), "Vida data txn failed: " + r.getError());

        Thread.sleep(SLEEP_TIME_AFTER_SENDING_TXNS);

        pwrj.subscribeToVidaTransactions(vidaId, blockNow, (transcation) -> {
            
        });
    }

    //function to generate x random bytes
    private static byte[] generateRandomBytes(int x) {
        byte[] randomBytes = new byte[x];
        for (int i = 0; i < x; i++) {
            randomBytes[i] = (byte) (Math.random() * 255);
        }
        return randomBytes;
    }
}
