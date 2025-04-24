import com.github.pwrlabs.pwrj.Utils.Hex;
import com.github.pwrlabs.pwrj.entities.FalconTransaction;
import com.github.pwrlabs.pwrj.protocol.PWRJ;
import com.github.pwrlabs.pwrj.protocol.TransactionBuilder;
import com.github.pwrlabs.pwrj.record.response.Response;
import com.github.pwrlabs.pwrj.entities.Validator;
import com.github.pwrlabs.pwrj.wallet.PWRFalconWallet;
import io.pwrlabs.util.encoders.ByteArrayWrapper;

import java.util.*;

import static com.github.pwrlabs.pwrj.Utils.NewError.errorIf;

public class ConduitsTest {
    private static final PWRJ pwrj = new PWRJ("http://46.101.151.203:8085/");
    private static final long amountToSendToEachConduit = 10000000000L;

    public enum ConduitMode {
        STAKE_BASED,   // Voting power based on PWR Chain validator stake
        COUNT_BASED,   // Each conduit has 1 vote
        VIDA_BASED,    // Custom voting power managed by VIDA
        ACTIVE_VALIDATOR_ONLY // Only active PWR Chain validators can manage it with voting power equal to their PWR Chain voting power
    }

//    public static void main(String[] args) throws Exception {
//        PWRFalconWallet wallet1 = new PWRFalconWallet(12, pwrj);
//        PWRFalconWallet activeValidator = PWRFalconWallet.loadWallet(pwrj, "wallet");
//
//        System.out.println("Wallet 1: " + wallet1.getAddress());
//
//        // Wait until user clicks enter
//        System.out.println("Please supply wallet 1 with funds and press enter to continue...");
//        try {
//            System.in.read();
//        } catch (Exception e) {
//            e.printStackTrace();
//        }
//
//        testCountBasedConduits(wallet1);
//        testVidaBasedConduits(wallet1);
//        testValidatorBasedConduits(wallet1);
//        testActiveValidatorBasedConduits(wallet1, activeValidator);
//
//    }

    private static void testCountBasedConduits(PWRFalconWallet wallet1) throws Exception {
        List<PWRFalconWallet> conduits = new ArrayList<>();
        for (int i = 0; i < 10; i++) {
            PWRFalconWallet conduitWallet = new PWRFalconWallet(12, pwrj);

            Response r = wallet1.transferPWR(conduitWallet.getByteaAddress(), 1000000000, pwrj.getFeePerByte());
            if(!r.isSuccess()) throw new Exception("Failed to transfer PWR to conduit wallet: " + r.getError());

            System.out.println("Conduit wallet " + i + ": " + conduitWallet.getAddress());

            conduits.add(conduitWallet);
        }

        long vidaId = new Random().nextLong();
        System.out.println("Vida ID: " + vidaId);
        Response r = wallet1.claimVidaId(vidaId, pwrj.getFeePerByte());
        if(!r.isSuccess()) throw new Exception("Failed to claim vida id: " + r.getError());

        waitUntilTransactionsIsProcessed(r.getTransactionHash());

        String vidaOwner = pwrj.getOwnerOfVida(vidaId);
        if(!vidaOwner.startsWith("0x")) vidaOwner = "0x" + vidaOwner;
        if(vidaOwner == null || !vidaOwner.equalsIgnoreCase(wallet1.getAddress())) {
            throw new Exception("Failed to claim vida id. Vida owner: " + vidaOwner);
        }

        Set<byte[]> conduitsSet = new HashSet<>();
        for (PWRFalconWallet conduit : conduits) {
            conduitsSet.add(conduit.getByteaAddress());
        }

        r = wallet1.setConduitMode(vidaId, (byte) 1, 5100, conduitsSet, null, pwrj.getFeePerByte());
        if(!r.isSuccess()) throw new Exception("Failed to set conduits: " + r.getError());
        else {
            System.out.println("Conduits set txn sent: " + r.getTransactionHash());
        }

        waitUntilTransactionsIsProcessed(r.getTransactionHash());

        Map<ByteArrayWrapper, Long> conduitsVotingPower = pwrj.getConduitsOfVida(vidaId);
        if (conduitsVotingPower == null || conduitsVotingPower.isEmpty()) throw new Exception("Failed to get conduits of vida");

        //make sure all conduits are shown
        for (PWRFalconWallet conduit : conduits) {
            if (!conduitsVotingPower.containsKey(new ByteArrayWrapper(conduit.getByteaAddress()))) {
                throw new Exception("Failed to get conduits of vida");
            }
        }

        testIfConduitNodesAreDoingTheirJob(conduits, vidaId, wallet1);

        System.out.println("Count based conduits test passed");

    }

    private static void testVidaBasedConduits(PWRFalconWallet wallet) throws Exception {
        List<PWRFalconWallet> conduits = new ArrayList<>();
        for (int i = 0; i < 10; i++) {
            PWRFalconWallet conduitWallet = new PWRFalconWallet(12, pwrj);

            Response r = wallet.transferPWR(conduitWallet.getByteaAddress(), 1000000000, pwrj.getFeePerByte());
            if(!r.isSuccess()) throw new Exception("Failed to transfer PWR to conduit wallet: " + r.getError());

            System.out.println("Conduit wallet " + i + ": " + conduitWallet.getAddress());

            conduits.add(conduitWallet);
        }

        long vidaId = new Random().nextLong();
        System.out.println("Vida ID: " + vidaId);
        Response r = wallet.claimVidaId(vidaId, pwrj.getFeePerByte());
        if(!r.isSuccess()) throw new Exception("Failed to claim vida id: " + r.getError());
        waitUntilTransactionsIsProcessed(r.getTransactionHash());

        String vidaOwner = pwrj.getOwnerOfVida(vidaId);
        if(!vidaOwner.startsWith("0x")) vidaOwner = "0x" + vidaOwner;
        if(vidaOwner == null || !vidaOwner.equalsIgnoreCase(wallet.getAddress())) {
            throw new Exception("Failed to claim vida id. Vida owner: " + vidaOwner);
        }

        Map<ByteArrayWrapper, Long> cvp = new HashMap<>();
        int i = 0;
        for (PWRFalconWallet conduit : conduits) {
            cvp.put(new ByteArrayWrapper(conduit.getByteaAddress()), 1000L + i);
        }

        r = wallet.setConduitMode(vidaId, (byte) 2, 5100, null, cvp, pwrj.getFeePerByte());
        if(!r.isSuccess()) throw new Exception("Failed to set conduits: " + r.getError());
        else {
            System.out.println("Conduits set txn sent: " + r.getTransactionHash());
        }

        waitUntilTransactionsIsProcessed(r.getTransactionHash());

        Map<ByteArrayWrapper, Long> conduitsVotingPower = pwrj.getConduitsOfVida(vidaId);
        if (conduitsVotingPower == null || conduitsVotingPower.isEmpty()) throw new Exception("Failed to get conduits of vida");

        //make sure all conduits are shown and the voting power is the same
        for (PWRFalconWallet conduit : conduits) {
            if (!conduitsVotingPower.containsKey(new ByteArrayWrapper(conduit.getByteaAddress()))) {
                throw new Exception("Failed to get conduits of vida");
            }

            long expectedVotingPower = cvp.get(new ByteArrayWrapper(conduit.getByteaAddress()));
            long actualVotingPower = conduitsVotingPower.get(new ByteArrayWrapper(conduit.getByteaAddress()));
            if (actualVotingPower != expectedVotingPower) {
                throw new Exception("Failed to get conduits of vida. Expected " + expectedVotingPower + ", got " + actualVotingPower);
            }
            i++;
        }

        testIfConduitNodesAreDoingTheirJob(conduits, vidaId, wallet);
        System.out.println("Vida based conduits test passed");

    }

    private static void testValidatorBasedConduits(PWRFalconWallet wallet) throws Exception {
        List<PWRFalconWallet> conduits = new ArrayList<>();
        for (int i = 0; i < 10; i++) {
            PWRFalconWallet conduitWallet = new PWRFalconWallet(12, pwrj);

            Response r = wallet.transferPWR(conduitWallet.getByteaAddress(), amountToSendToEachConduit, pwrj.getFeePerByte());
            if(!r.isSuccess()) throw new Exception("Failed to transfer PWR to conduit wallet: " + r.getError());

            waitUntilTransactionsIsProcessed(r.getTransactionHash());

            System.out.println("Conduit wallet " + i + ": " + conduitWallet.getAddress());

            conduits.add(conduitWallet);
        }

        for (PWRFalconWallet conduit : conduits) {
            Response rr = conduit.joinAsValidator(pwrj.getFeePerByte(), generateRandomIP());
            if(!rr.isSuccess()) throw new Exception("Failed to join as validator: " + rr.getError());

            waitUntilTransactionsIsProcessed(rr.getTransactionHash());

            Validator v = pwrj.getValidator(conduit.getAddress());
            errorIf(v == null, "Failed to join as validator");

            rr = conduit.delegate(conduit.getByteaAddress(), amountToSendToEachConduit / 2, pwrj.getFeePerByte());
            if(!rr.isSuccess()) throw new Exception("Failed to delegate: " + rr.getError());

            waitUntilTransactionsIsProcessed(rr.getTransactionHash());

            long delegation = pwrj.getDelegatedPWR(conduit.getAddress(), conduit.getAddress());
            errorIf(delegation == 0, "Failed to delegate");
        }

        long vidaId = new Random().nextLong();
        claimVidaId(vidaId, wallet);

        Set<byte[]> conduitsSet = new HashSet<>();
        for (PWRFalconWallet conduit : conduits) {
            conduitsSet.add(conduit.getByteaAddress());
        }

        Response r = wallet.setConduitMode(vidaId, (byte) 0, 5100, conduitsSet, null, pwrj.getFeePerByte());

        if(!r.isSuccess()) throw new Exception("Failed to set conduits: " + r.getError());
        else {
            System.out.println("Conduits set txn sent: " + r.getTransactionHash());
        }

        waitUntilTransactionsIsProcessed(r.getTransactionHash());

        Map<ByteArrayWrapper, Long> conduitsVotingPower = pwrj.getConduitsOfVida(vidaId);
        if (conduitsVotingPower == null || conduitsVotingPower.isEmpty()) throw new Exception("Failed to get conduits of vida");
        //make sure all conduits are shown and the voting power is the same
        for (PWRFalconWallet conduit : conduits) {
            if (!conduitsVotingPower.containsKey(new ByteArrayWrapper(conduit.getByteaAddress()))) {
                throw new Exception("Failed to get conduits of vida");
            }

            long expectedVotingPower = pwrj.getValidator(conduit.getAddress()).getVotingPower();
            long actualVotingPower = conduitsVotingPower.get(new ByteArrayWrapper(conduit.getByteaAddress()));
            if (actualVotingPower != expectedVotingPower) {
                throw new Exception("Failed to get conduits of vida. Expected " + expectedVotingPower + ", got " + actualVotingPower);
            }
        }

        testIfConduitNodesAreDoingTheirJob(conduits, vidaId, wallet);

        System.out.println("Validator based conduits test passed");
    }

    private static void testActiveValidatorBasedConduits(PWRFalconWallet wallet, PWRFalconWallet activeValidator) throws Exception {
        long vidaId = new Random().nextLong();
        claimVidaId(vidaId, wallet);

        Set<byte[]> conduitsSet = new HashSet<>();
        conduitsSet.add(activeValidator.getByteaAddress());

        Response r = wallet.setConduitMode(vidaId, (byte) 3, 5100, conduitsSet, null, pwrj.getFeePerByte());
        if(!r.isSuccess()) throw new Exception("Failed to set conduits: " + r.getError());
        else {
            System.out.println("Conduits set txn sent: " + r.getTransactionHash());
        }

        waitUntilTransactionsIsProcessed(r.getTransactionHash());

        Map<ByteArrayWrapper, Long> conduitsVotingPower = pwrj.getConduitsOfVida(vidaId);
        if (conduitsVotingPower == null || conduitsVotingPower.isEmpty()) throw new Exception("Failed to get conduits of vida");

        //make sure all conduits are shown and the voting power is the same
        if (!conduitsVotingPower.containsKey(new ByteArrayWrapper(activeValidator.getByteaAddress()))) {
            throw new Exception("Failed to get conduits of vida");
        }

        long expectedVotingPower = pwrj.getValidator(activeValidator.getAddress()).getVotingPower();
        long actualVotingPower = conduitsVotingPower.get(new ByteArrayWrapper(activeValidator.getByteaAddress()));
        if (actualVotingPower != expectedVotingPower) {
            throw new Exception("Failed to get conduits of vida. Expected " + expectedVotingPower + ", got " + actualVotingPower);
        }

        testIfConduitNodesAreDoingTheirJob(List.of(activeValidator), vidaId, wallet);

        System.out.println("Active validator based conduits test passed");
    }

    private static void claimVidaId(long vidaId, PWRFalconWallet wallet) throws Exception {
        System.out.println("Vida ID: " + vidaId);

        Response r = wallet.claimVidaId(vidaId, pwrj.getFeePerByte());
        if(!r.isSuccess()) throw new Exception("Failed to claim vida id: " + r.getError());

        waitUntilTransactionsIsProcessed(r.getTransactionHash());

        String vidaOwner = pwrj.getOwnerOfVida(vidaId);
        if(!vidaOwner.startsWith("0x")) vidaOwner = "0x" + vidaOwner;
        if(vidaOwner == null || !vidaOwner.equalsIgnoreCase(wallet.getAddress())) {
            throw new Exception("Failed to claim vida id. Vida owner: " + vidaOwner);
        }
    }

    private static void testIfConduitNodesAreDoingTheirJob(List<PWRFalconWallet> conduits, long vidaId, PWRFalconWallet wallet1) throws Exception {
        //funding the vida
        Response r = wallet1.transferPWR(pwrj.getVidaIdAddressBytea(vidaId), 1000000000, pwrj.getFeePerByte());
        if(!r.isSuccess()) throw new Exception("Failed to fund vida: " + r.getError());

        waitUntilTransactionsIsProcessed(r.getTransactionHash());

        long vidaBalance = pwrj.getBalanceOfAddress(pwrj.getVidaIdAddress(vidaId));
        if(vidaBalance != 1000000000) throw new Exception("Failed to fund vida. Expected 1000000000, got " + vidaBalance);

        byte[] vidaAddress = pwrj.getVidaIdAddressBytea(vidaId);
        byte[] receiver = generateRandomBytes(20);
        long amount = 1000;
        byte[] txn = TransactionBuilder.getTransferTransaction(pwrj.getFeePerByte(), vidaAddress, receiver, amount, pwrj.getNonceOfAddress(Hex.toHexString(vidaAddress)), pwrj.getChainId());

        for (PWRFalconWallet conduit : conduits) {
            System.out.println("Sending conduit approval txn from node: " + conduit.getAddress());
            r = conduit.approveAsConduit(vidaId, List.of(txn), pwrj.getFeePerByte());
            if(!r.isSuccess()) throw new Exception("Failed to sign transaction: " + r.getError());
        }

        waitUntilTransactionsIsProcessed(r.getTransactionHash());

        long vidaBalanceAfter = pwrj.getBalanceOfAddress(pwrj.getVidaIdAddress(vidaId));
        long receiverBalance = pwrj.getBalanceOfAddress(Hex.toHexString(receiver));

        if(vidaBalanceAfter > vidaBalance - amount) throw new Exception("Failed to transfer vida balance. Expected " + (vidaBalance - amount) + ", got " + vidaBalanceAfter);
        if(receiverBalance != amount) throw new Exception("Failed to transfer vida balance. Expected " + amount + ", got " + receiverBalance);

        System.out.println("Them conduits did them job good");
    }

    private static byte[] generateRandomBytes(int x) {
        byte[] randomBytes = new byte[x];
        for (int i = 0; i < x; i++) {
            randomBytes[i] = (byte) (Math.random() * 255);
        }
        return randomBytes;
    }

    //function to generate random ip
    private static String generateRandomIP() {
        Random random = new Random();
        return random.nextInt(256) + "." + random.nextInt(256) + "." + random.nextInt(256) + "." + random.nextInt(256);
    }

    private static void waitUntilTransactionsIsProcessed(String txnHash) throws Exception {
        long maxTime = 10000;
        long timeNow = System.currentTimeMillis();

        while (System.currentTimeMillis() - timeNow < maxTime) {
            try {
                FalconTransaction txn = pwrj.getTransactionByHash(txnHash);
                if(txn != null) {
                    if(txn.isSuccess()) return;
                    else throw new Exception("Transaction failed: " + txn.getErrorMessage());
                }
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
