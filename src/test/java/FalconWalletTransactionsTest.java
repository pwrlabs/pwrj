import com.github.pwrlabs.pwrj.Utils.Hex;
import com.github.pwrlabs.pwrj.protocol.PWRJ;
import com.github.pwrlabs.pwrj.record.response.Response;
import com.github.pwrlabs.pwrj.wallet.PWRFalconWallet;

import java.io.IOException;

import static com.github.pwrlabs.pwrj.Utils.NewError.errorIf;

//this class is for testing all transactions on PWR Chain
//It's focused on testing the transactions and the soundness of their execution
public class FalconWalletTransactionsTest {
    private static final PWRJ pwrj = new PWRJ("http://localhost:8085");
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
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private static void testTransfer(PWRFalconWallet sender, byte[] receiver, long amount) throws IOException {
        System.out.println("Transfering " + amount + " from " + sender.getAddress() + " to " + Hex.toHexString(receiver));

        long senderNonce = pwrj.getNonceOfAddress(sender.getAddress());
        long senderBalanceBefore = pwrj.getBalanceOfAddress(sender.getAddress());
        long receiverBalanceBefore = pwrj.getBalanceOfAddress(Hex.toHexString(receiver));

        try {
            Response r = sender.transferPWR(receiver, amount, pwrj.getFeePerByte());
            errorIf(!r.isSuccess(), "Transfer failed: " + r.getError());
        } catch (Exception e) {
            e.printStackTrace();
            System.exit(0);
        }

        long startTime = System.currentTimeMillis();
        while (pwrj.getNonceOfAddress(sender.getAddress()) <= senderNonce && System.currentTimeMillis() - startTime < 10000) {
            try {
                Thread.sleep(1000);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }

        System.out.println("Transfer complete. Time taken: " + (System.currentTimeMillis() - startTime) + "ms");

        long senderBalanceAfter = pwrj.getBalanceOfAddress(sender.getAddress());
        long receiverBalanceAfter = pwrj.getBalanceOfAddress(Hex.toHexString(receiver));

        System.out.println("Sender balance before: " + senderBalanceBefore);
        System.out.println("Sender balance after: " + senderBalanceAfter);

        System.out.println("Receiver balance before: " + receiverBalanceBefore);
        System.out.println("Receiver balance after: " + receiverBalanceAfter);

        errorIf(senderBalanceAfter > senderBalanceBefore - amount, "Sender balance incorrect");
        errorIf(receiverBalanceAfter < receiverBalanceBefore + amount, "Receiver balance incorrect");
    }
}
