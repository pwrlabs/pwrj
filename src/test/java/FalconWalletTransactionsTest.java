import java.io.IOException;
import java.util.Arrays;
import java.util.List;
import java.util.Random;
import java.util.Set;
import java.util.concurrent.atomic.AtomicBoolean;

import com.github.pwrlabs.pwrj.Utils.Hex;
import static com.github.pwrlabs.pwrj.Utils.NewError.errorIf;
import com.github.pwrlabs.pwrj.protocol.PWRJ;
import com.github.pwrlabs.pwrj.protocol.VidaTransactionSubscription;
import com.github.pwrlabs.pwrj.record.response.Response;
import com.github.pwrlabs.pwrj.wallet.PWRFalconWallet;
import org.w3c.dom.ls.LSInput;

//this class is for testing all transactions on PWR Chain
//It's focused on testing the transactions and the soundness of their execution
public class FalconWalletTransactionsTest {
    private static final PWRJ pwrj = new PWRJ("http://localhost:8085");
    private static final int SLEEP_TIME_AFTER_SENDING_TXNS = 6000;

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
//            testTransfer(wallet1, wallet2Address, 1000000000);
//            testVidaDataTxn(wallet1);
            testVidaOperations(wallet1, wallet2);
            testSetVidaPrivate(wallet1, wallet2);
        } catch (Exception e) {
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

    private static void testVidaDataTxn(PWRFalconWallet sender, long vidaId, long value) throws IOException, InterruptedException {
        byte[] data = generateRandomBytes(230);
        long blockNow = pwrj.getBlockNumber();

        Response r = sender.submitPayableVidaData(vidaId, data, value, pwrj.getFeePerByte());
        errorIf(!r.isSuccess(), "Vida data txn failed: " + r.getError());

        Thread.sleep(SLEEP_TIME_AFTER_SENDING_TXNS);

        AtomicBoolean found = new AtomicBoolean(false);
        VidaTransactionSubscription sub = pwrj.subscribeToVidaTransactions(pwrj, vidaId, blockNow, (transcation) -> {
            byte[] byteData = transcation.getData();

            if (Arrays.equals(byteData, data)) {
                if(transcation.getValue() == value) {
                    System.out.println("Vida data txn successful");
                    found.set(true);
                } else {
                    System.err.println("Vida data txn failed");
                    System.out.println("Expected: " + value + " but got: " + transcation.getValue());
                }
            } else {
                System.err.println("Vida data txn failed");
                System.out.println("Expected: " + Arrays.toString(data) + " but got: " + Arrays.toString(byteData));
            }

        });

        long startTime = System.currentTimeMillis();
        while (!found.get()) {
            Thread.sleep(1000);

            //Sleep for only 10 seconds
            if (System.currentTimeMillis() - startTime > 10000) {
                break;
            }
        }

        sub.stop();

        if(!found.get()) {
            System.err.println("Vida data txn failed");
            errorIf(true, "Vida data txn failed");
        }
    }

    private static void testVidaOperations(PWRFalconWallet wallet, PWRFalconWallet wallet2) throws Exception{
        long vidaId = new Random().nextLong();
        System.out.println("Vida ID: " + vidaId);

        //region - VIDA ID Claiming test
        Response r = wallet.claimVidaId(vidaId, pwrj.getFeePerByte());
        if(!r.isSuccess()) {
            System.err.println("Claim vida id failed: " + r.getError());
            throw new Exception("Claim vida id failed: " + r.getError());
        }

        long startTime = System.currentTimeMillis();
        while (true) {
            String owner = pwrj.getOwnerOfVida(vidaId);
            if(owner == null || owner.equals("0x")) {
                Thread.sleep(1000);
                if(System.currentTimeMillis() - startTime > SLEEP_TIME_AFTER_SENDING_TXNS) {
                    System.err.println("Claim vida id failed");
                    throw new Exception("Claim vida id failed");
                }
            } else {
                String walletAddress = wallet.getAddress();

                if (owner.startsWith("0x")) owner = owner.substring(2);
                if(walletAddress.startsWith("0x")) walletAddress = walletAddress.substring(2);

                if(!owner.equals(walletAddress)) {
                    System.err.println("Claim vida id failed");
                    System.out.println("Expected: " + walletAddress + " but got: " + owner);
                    throw new Exception("Claim vida id failed");
                }

                System.out.println("Claim vida id successful");
                break;
            }
        }
        //endregion

        //region - Add vida sponsored addresses test
        Set<byte[]> sponsoredAddresses = Set.of(wallet.getByteaAddress(), wallet2.getByteaAddress());
        r = wallet.addVidaSponsoredAddresses(vidaId, sponsoredAddresses, pwrj.getFeePerByte());
        if(!r.isSuccess()) {
            System.err.println("Add vida sponsored addresses failed: " + r.getError());
            throw new Exception("Add vida sponsored addresses failed: " + r.getError());
        }

        startTime = System.currentTimeMillis();
        while (true) {
            List<String> addresses = pwrj.getVidaSponsoredAddresses(vidaId);
            if(addresses == null || addresses.isEmpty()) {
                Thread.sleep(1000);
                if(System.currentTimeMillis() - startTime > SLEEP_TIME_AFTER_SENDING_TXNS) {
                    System.err.println("Add vida sponsored addresses failed");
                    throw new Exception("Add vida sponsored addresses failed");
                }
            } else {
                System.out.println("Add vida sponsored addresses successful");
                break;
            }
        }
        //endregion

        //region - Sending txn from sponsored addresses test
        testTransfer(wallet, pwrj.getVidaIdAddressBytea(vidaId), 1000000000);
        testTransfer(wallet, wallet2.getByteaAddress(), 1000000000);

        r = wallet2.makeSurePublicKeyIsSet(pwrj.getFeePerByte());
        if(r !=null && !r.isSuccess()) {
            throw new RuntimeException("Error: " + r.getError());
        }

        long walletBalanceBefore = pwrj.getBalanceOfAddress(wallet2.getAddress());
        long vidaIdBalanceBefore = pwrj.getBalanceOfAddress(pwrj.getVidaIdAddress(vidaId));
        testVidaDataTxn(wallet2, vidaId, 0);
        long walletBalanceAfter = pwrj.getBalanceOfAddress(wallet2.getAddress());
        long vidaIdBalanceAfter = pwrj.getBalanceOfAddress(pwrj.getVidaIdAddress(vidaId));
        errorIf(walletBalanceAfter != walletBalanceBefore, "User wallet balance incorrect. Wallet balance was expected to stay the same");
        errorIf(!(vidaIdBalanceAfter < vidaIdBalanceBefore), "Vida ID balance incorrect. Vida ID balance was expected to decrease");

        //endregion

        //region - Remove vida sponsored addresses test
        r = wallet.removeVidaSponsoredAddresses(vidaId, Set.of(wallet.getByteaAddress()), pwrj.getFeePerByte());
        if(!r.isSuccess()) {
            System.err.println("Remove vida sponsored addresses failed: " + r.getError());
            throw new Exception("Remove vida sponsored addresses failed: " + r.getError());
        }

        Thread.sleep(SLEEP_TIME_AFTER_SENDING_TXNS);

        List<String> addresses = pwrj.getVidaSponsoredAddresses(vidaId);
        if(addresses == null || addresses.isEmpty()) {
            System.out.println("Remove vida sponsored addresses error, it ended up removing all addresses");
        } else if(!addresses.contains(wallet.getAddress())) {
            System.out.println("Remove vida sponsored addresses successful");
        }
        //endregion

    }

    private static void testSetVidaPrivate(PWRFalconWallet wallet, PWRFalconWallet wallet2) throws Exception {
        long vidaId = new Random().nextLong();

        //region - Claim VIDA ID
        Response r = wallet.claimVidaId(vidaId, pwrj.getFeePerByte());
        if(!r.isSuccess()) {
            System.err.println("Claim vida id failed: " + r.getError());
            throw new Exception("Claim vida id failed: " + r.getError());
        }

        long startTime = System.currentTimeMillis();
        while (true) {
            String owner = pwrj.getOwnerOfVida(vidaId);
            if(owner == null || owner.equals("0x")) {
                Thread.sleep(1000);
                if(System.currentTimeMillis() - startTime > SLEEP_TIME_AFTER_SENDING_TXNS) {
                    System.err.println("Claim vida id failed");
                    throw new Exception("Claim vida id failed");
                }
            } else {
                String walletAddress = wallet.getAddress();

                if (owner.startsWith("0x")) owner = owner.substring(2);
                if(walletAddress.startsWith("0x")) walletAddress = walletAddress.substring(2);

                if(!owner.equals(walletAddress)) {
                    System.err.println("Claim vida id failed");
                    System.out.println("Expected: " + walletAddress + " but got: " + owner);
                    throw new Exception("Claim vida id failed");
                }

                System.out.println("Claim vida id successful");
                break;
            }
        }
        //endregion

        //region - Set vida as private test
        r = wallet.setVidaPrivateState(vidaId, true, pwrj.getFeePerByte());
        if(!r.isSuccess()) {
            System.err.println("Set vida private failed: " + r.getError());
            throw new Exception("Set vida private failed: " + r.getError());
        }

        startTime = System.currentTimeMillis();
        while (true) {
            Boolean isPrivate = pwrj.isVidaPrivate(vidaId);
            if(isPrivate == null || !isPrivate) {
                Thread.sleep(1000);
                if(System.currentTimeMillis() - startTime > SLEEP_TIME_AFTER_SENDING_TXNS) {
                    System.err.println("Set vida private failed");
                    throw new Exception("Set vida private failed");
                }
            } else {
                System.out.println("Set vida private successful");
                break;
            }
        }
        //endregion

        //region - Add allowed senders test
        Set<byte[]> allowedSenders = Set.of(wallet.getByteaAddress(), wallet2.getByteaAddress());
        r = wallet.addVidaAllowedSenders(vidaId, allowedSenders, pwrj.getFeePerByte());
        if(!r.isSuccess()) {
            System.err.println("Add vida allowed senders failed: " + r.getError());
            throw new Exception("Add vida allowed senders failed: " + r.getError());
        }

        startTime = System.currentTimeMillis();
        while (true) {
            List<String> senders = pwrj.getVidaAllowedSenders(vidaId);
            if(senders == null || senders.isEmpty()) {
                Thread.sleep(1000);
                if(System.currentTimeMillis() - startTime > SLEEP_TIME_AFTER_SENDING_TXNS) {
                    System.err.println("Add vida allowed senders failed");
                    throw new Exception("Add vida allowed senders failed");
                }
            } else {
                if(senders.contains(wallet.getAddress())) {
                    System.out.println("Add vida allowed senders successful");
                    break;
                } else {
                    System.err.println("Add vida allowed senders failed");
                    throw new Exception("Add vida allowed senders failed");
                }
            }
        }
        //endregion

        //region - Test sending from allowed sender
        testVidaDataTxn(wallet, vidaId, pwrj.getFeePerByte());
        //endregion

        //region - Test removing an allowed sender
        r = wallet.removeVidaAllowedSenders(vidaId, Set.of(wallet.getByteaAddress()), pwrj.getFeePerByte());
        if(!r.isSuccess()) {
            System.err.println("Remove vida allowed senders failed: " + r.getError());
            throw new Exception("Remove vida allowed senders failed: " + r.getError());
        }

        Thread.sleep(SLEEP_TIME_AFTER_SENDING_TXNS);
        List<String> senders = pwrj.getVidaAllowedSenders(vidaId);
        if(senders == null || senders.isEmpty()) {
            System.out.println("Remove vida allowed senders error, it ended up removing all addresses");
        } else if(!senders.contains(wallet.getAddress())) {
            System.out.println("Remove vida allowed senders successful");
        }
        //endregion
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
