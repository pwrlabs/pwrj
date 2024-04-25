package com.github.pwrlabs.pwrj;

import com.github.pwrlabs.pwrj.Utils.Hex;
import com.github.pwrlabs.pwrj.protocol.PWRJ;
import com.github.pwrlabs.pwrj.protocol.TransactionBuilder;
import com.github.pwrlabs.pwrj.record.response.Response;
import com.github.pwrlabs.pwrj.record.validator.Validator;
import com.github.pwrlabs.pwrj.wallet.PWRWallet;

import java.io.IOException;
import java.math.BigInteger;
import java.time.Instant;
import java.util.List;
import java.util.Random;

import static org.junit.Assert.*;

public class Main {
    private static final long ECDSA_VERIFICATION_FEE = 10000;
    private static final String RPC_URL = "http://localhost:8085/";
    private static final BigInteger PRIVATE_KEY_1 = new BigInteger("13441705239710856426490937717111545450041915423641316365679523930528732611559");
    private static final BigInteger PRIVATE_KEY_2 = new BigInteger("65667622470184592671268428677185924916315539718461627986432216206742674338707");

    public static final String VALIDATOR_ADDRESS_1 = "0x61Bd8fc1e30526Aaf1C4706Ada595d6d236d9883";
    public static final String VALIDATOR_ADDRESS_2 = "0x4dc619b41224d82d153fbc6389ca910f7f56de63";

    public static void main(String[] args) throws Exception {
        System.out.println(Long.MAX_VALUE);
        System.out.println(Long.MIN_VALUE);
        System.exit(0);
        removeValidator("0x995e5b77e3cdc413bb5562f24abceb725a6f92b8");
        Thread.sleep(1000);
        System.exit(0);
        PWRJ pwrj = new PWRJ(RPC_URL);
        PWRWallet wallet1 = new PWRWallet(PRIVATE_KEY_1, pwrj);
        PWRWallet wallet2 = new PWRWallet(PRIVATE_KEY_2, pwrj);

        System.out.println("Wallet 1 address: " + wallet1.getAddress());
        System.out.println("Wallet 2 address: " + wallet2.getAddress());

       // System.exit(0);
        //testTransferPWR(wallet1, wallet2, pwrj);
        //testDelegateAndWithdraw(wallet1, pwrj);
        //testClaimAndSendVmData(wallet1, pwrj);
        //testGuardianTransactions(wallet1, wallet2, pwrj);
        //testMoveStake(wallet1, pwrj);
       // testValidatorTransactions(wallet1, wallet2, pwrj);
        //testPayableVmDataTransaction(wallet1, wallet2, pwrj);
       // testConduitTransactions(wallet1, wallet2, pwrj);
        testTransferPWRFromVM(wallet1, wallet2, pwrj);
    }

    public static void removeValidator(String address) throws Exception {
        PWRJ pwrj = new PWRJ("https://pwrrpc.pwrlabs.io/");
        PWRWallet wallet = new PWRWallet(new BigInteger("03a5240936d67dc18dca348e793010a14c5eba86a73d0c9e45764681295a73df", 16), pwrj);

        Response r = wallet.sendValidatorRemoveTransaction(address, wallet.getNonce());

        if(r.isSuccess()) {
            System.out.println("Validator removed successfully");
            System.out.println(r.getTransactionHash());
            System.out.println(r.getError());
        } else {
            System.out.println("Failed to remove validator: " + r.getError());
        }
    }

    private static void testTransferPWR(PWRWallet sender, PWRWallet recipient, PWRJ pwrj) throws Exception {
        long initialSenderBalance = sender.getBalance();
        long initialRecipientBalance = recipient.getBalance();
        long transferAmount = 1000000000;

        byte[] signedTransaction = sender.getSignedTransferPWRTransaction(recipient.getAddress(), transferAmount, sender.getNonce());
        long transactionFee = signedTransaction.length * pwrj.getFeePerByte() + ECDSA_VERIFICATION_FEE;

        Response response = sender.transferPWR(recipient.getAddress(), transferAmount, sender.getNonce());
        assertTrue("Transfer PWR failed: " + response.getError(), response.isSuccess());

        // Wait for the transaction to be processed
        Thread.sleep(5000);

        long finalSenderBalance = sender.getBalance();
        long finalRecipientBalance = recipient.getBalance();

        assertEquals(initialSenderBalance - transferAmount - transactionFee, finalSenderBalance);
        assertEquals(initialRecipientBalance + transferAmount, finalRecipientBalance);

        System.out.println("Transfer PWR test successful");
    }

    private static void testDelegateAndWithdraw(PWRWallet delegator, PWRJ pwrj) throws Exception {
        String validatorAddress = "0x61Bd8fc1e30526Aaf1C4706Ada595d6d236d9883";
        long delegateAmount = 1000000000;

        long initialShares = pwrj.getSharesOfDelegator(delegator.getAddress(), validatorAddress);
        long initialDelegatedPWR = pwrj.getDelegatedPWR(delegator.getAddress(), validatorAddress);

        Response delegateResponse = delegator.delegate(validatorAddress, delegateAmount, delegator.getNonce());
        assertTrue("Delegate failed: " + delegateResponse.getError(), delegateResponse.isSuccess());

        // Wait for the delegation to be processed
        while (pwrj.getSharesOfDelegator(delegator.getAddress(), validatorAddress) == initialShares) {
            Thread.sleep(1000);
        }

        long sharesAfterDelegate = pwrj.getSharesOfDelegator(delegator.getAddress(), validatorAddress);
        assertTrue(sharesAfterDelegate > initialShares);
        long sharesDelegated = sharesAfterDelegate - initialShares;

        System.out.println("Delegate test successful");

        long balanceBeforeWithdraw = delegator.getBalance();
        byte[] signedWithdrawTransaction = delegator.getSignedWithdrawTransaction(validatorAddress, sharesDelegated, delegator.getNonce());
        long transactionFee = signedWithdrawTransaction.length * pwrj.getFeePerByte() + ECDSA_VERIFICATION_FEE;
        Response withdrawResponse = delegator.withdraw(validatorAddress, sharesDelegated, delegator.getNonce());
        assertTrue("Withdraw failed: " + withdrawResponse.getError(), withdrawResponse.isSuccess());

        // Wait for the withdrawal to be processed
        while (pwrj.getSharesOfDelegator(delegator.getAddress(), validatorAddress) >= sharesAfterDelegate) {
            Thread.sleep(1000);
        }

        long finalShares = pwrj.getSharesOfDelegator(delegator.getAddress(), validatorAddress);
        assertEquals(sharesAfterDelegate - sharesDelegated, finalShares);

        //We will not check for balance change, because withdrawal has a 7 day lock period
//        System.out.println("balance: " + delegator.getBalance());
//        System.out.println("Expected: " + (balanceBeforeWithdraw + delegateAmount - transactionFee));
//        assertTrue(delegator.getBalance() >= balanceBeforeWithdraw + delegateAmount - transactionFee);

        System.out.println("Withdraw test successful");
    }

    private static void testClaimAndSendVmData(PWRWallet wallet, PWRJ pwrj) throws Exception {
        long vmId = new Random().nextLong();

        Response claimResponse = wallet.claimVmId(vmId, wallet.getNonce());
        assertTrue("Claim VM ID failed: " + claimResponse.getError(), claimResponse.isSuccess());

        // Wait for the claim transaction to be processed
        Thread.sleep(5000);

        String vmOwner = pwrj.getOwnerOfVm(vmId);
        System.out.println("VM Owner: " + vmOwner);
        System.out.println("Wallet address: " + wallet.getAddress());
        assertEquals(wallet.getAddress().toLowerCase(), vmOwner.toLowerCase());

        String data = "Hello, World!";
        Response sendDataResponse = wallet.sendVmDataTransaction(vmId, data.getBytes(), wallet.getNonce());
        assertTrue("Send VM data failed: " + sendDataResponse.getError(), sendDataResponse.isSuccess());
        Thread.sleep(5000);
    }

    private static void testGuardianTransactions(PWRWallet user, PWRWallet guardian, PWRJ pwrj) throws Exception {
        long expiryDate = Instant.now().getEpochSecond() + (60 * 60 * 24);

        String userGuardian = pwrj.getGuardianOfAddress(user.getAddress());
        if (userGuardian == null) {
            Response setGuardianResponse = user.setGuardian(guardian.getAddress(), expiryDate, user.getNonce());
            assertTrue("Set guardian failed: " + setGuardianResponse.getError(), setGuardianResponse.isSuccess());
            System.out.println(setGuardianResponse.toString());
            System.out.println("Waiting for guardian to be set");
            // Wait for the set guardian transaction to be processed
            while (pwrj.getGuardianOfAddress(user.getAddress()) == null) {
                Thread.sleep(1000);
            }

            userGuardian = pwrj.getGuardianOfAddress(user.getAddress());
            assertEquals(guardian.getAddress().toLowerCase(), userGuardian.toLowerCase());

            System.out.println("Set guardian test successful");
        }

        String receiverAddress = "0x61Bd8fc1e30526Aaf1C4706Ada595d6d236d9855";
        long transferAmount = 1000000000;

        long initialSenderBalance = user.getBalance();
        long initialReceiverBalance = pwrj.getBalanceOfAddress(receiverAddress);

        byte[] transferTransaction = user.getSignedTransferPWRTransaction(receiverAddress, transferAmount, user.getNonce());
        List<byte[]> transactions = List.of(transferTransaction);

        Response guardianApprovalResponse = guardian.sendGuardianApprovalTransaction(transactions, guardian.getNonce());
        assertTrue("Guardian approval failed: " + guardianApprovalResponse.getError(), guardianApprovalResponse.isSuccess());

        // Wait for the guardian approval transaction to be processed
        Thread.sleep(5000);

        long finalSenderBalance = user.getBalance();
        long finalReceiverBalance = pwrj.getBalanceOfAddress(receiverAddress);

        //Transaction fee is paid by guardian
        //long transactionFee = transferTransaction.length * pwrj.getFeePerByte() + ECDSA_VERIFICATION_FEE;
        assertEquals(initialSenderBalance - transferAmount, finalSenderBalance);
        assertEquals(initialReceiverBalance + transferAmount, finalReceiverBalance);

        System.out.println("Guardian approval test successful");

        byte[] removeGuardianTransaction = user.getSignedRemoveGuardianTransaction(user.getNonce());
        Response removeGuardianResponse = guardian.sendGuardianApprovalTransaction(List.of(removeGuardianTransaction), guardian.getNonce());
        assertTrue("Remove guardian failed: " + removeGuardianResponse.getError(), removeGuardianResponse.isSuccess());

        // Wait for the remove guardian transaction to be processed
        while (pwrj.getGuardianOfAddress(user.getAddress()) != null) {
            Thread.sleep(1000);
        }

        assertNull(pwrj.getGuardianOfAddress(user.getAddress()));

        System.out.println("Remove guardian test successful");
    }
    private static void testMoveStake(PWRWallet wallet, PWRJ pwrj) throws Exception {
        String validator1Address = "0x61Bd8fc1e30526Aaf1C4706Ada595d6d236d9883";
        String validator2Address = "0x4dc619b41224d82d153fbc6389ca910f7f56de63";

        long delegateAmount = 1000000000;
        long moveShares = 100000000;

        Response delegateResponse = wallet.delegate(validator1Address, delegateAmount, wallet.getNonce());
        assertTrue("Delegate failed: " + delegateResponse.getError(), delegateResponse.isSuccess());

        while (pwrj.getDelegatedPWR(wallet.getAddress(), validator1Address) < delegateAmount) {
            Thread.sleep(1000);
        }

        long validator1InitialShares = pwrj.getSharesOfDelegator(wallet.getAddress(), validator1Address);
        long validator2InitialShares = pwrj.getSharesOfDelegator(wallet.getAddress(), validator2Address);

        Response moveResponse = wallet.moveStake(moveShares, validator1Address, validator2Address, wallet.getNonce());
        assertTrue("Move stake failed: " + moveResponse.getError(), moveResponse.isSuccess());

        while (pwrj.getSharesOfDelegator(wallet.getAddress(), validator2Address) == validator2InitialShares) {
            Thread.sleep(1000);
        }

        long validator1FinalShares = pwrj.getSharesOfDelegator(wallet.getAddress(), validator1Address);
        long validator2FinalShares = pwrj.getSharesOfDelegator(wallet.getAddress(), validator2Address);

        assertEquals(validator1InitialShares - moveShares, validator1FinalShares);
        assertTrue(validator2FinalShares > validator2InitialShares);

        System.out.println("Move stake test successful");
    }
    private static void testValidatorTransactions(PWRWallet wallet1, PWRWallet wallet2, PWRJ pwrj) throws Exception {
        if (!isValidator(wallet1.getAddress(), pwrj)) {
            Response joinResponse = wallet1.join("11.11.11.11", wallet1.getNonce());
            assertTrue("Join failed: " + joinResponse.getError(), joinResponse.isSuccess());

            while (!isValidator(wallet1.getAddress(), pwrj)) {
                Thread.sleep(1000);
            }
        }

        if (!isValidator(wallet2.getAddress(), pwrj)) {
            Response joinResponse = wallet2.join("22.22.22.22", wallet2.getNonce());
            assertTrue("Join failed: " + joinResponse.getError(), joinResponse.isSuccess());

            while (!isValidator(wallet2.getAddress(), pwrj)) {
                Thread.sleep(1000);
            }
        }

        Response claimResponse = wallet1.claimActiveNodeSpot(wallet1.getNonce());
        assertTrue("Claim active node spot failed: " + claimResponse.getError(), claimResponse.isSuccess());

        // Wait for the claim transaction to be processed
        Thread.sleep(5000);

        Validator validator1 = pwrj.getValidator(wallet1.getAddress());
        assertEquals("active", validator1.getStatus());

        Response removeResponse = wallet2.sendValidatorRemoveTransaction(wallet1.getAddress(), wallet2.getNonce());
        assertTrue("Remove validator failed: " + removeResponse.getError(), removeResponse.isSuccess());

        // Wait for the remove transaction to be processed
        while (pwrj.getValidator(wallet1.getAddress()).getStatus().equals("active")) {
            Thread.sleep(1000);
        }

        Validator removedValidator = pwrj.getValidator(wallet1.getAddress());
        assertNotEquals("active", removedValidator.getStatus());
    }

    private static void testPayableVmDataTransaction(PWRWallet vmOwner, PWRWallet sender, PWRJ pwrj) throws Exception {
        long vmId = new Random().nextLong();
        long value = 1000000000;
        String data = "Payable VM data";

        Response claimResponse = vmOwner.claimVmId(vmId, vmOwner.getNonce());
        assertTrue("Claim VM ID failed: " + claimResponse.getError(), claimResponse.isSuccess());

        System.out.println("Waiting for VM ID to be claimed");
        while (pwrj.getOwnerOfVm(vmId) == null) {
            Thread.sleep(1000);
        }

        assertEquals(vmOwner.getAddress().toLowerCase(), pwrj.getOwnerOfVm(vmId).toLowerCase());
        System.out.println("Successfully claimed VM ID");

        long initialOwnerBalance = vmOwner.getBalance();
        long initialSenderBalance = sender.getBalance();

        byte[] signedTransaction = sender.getSignedPayableVmDataTransaction(vmId, value, data.getBytes(), sender.getNonce());
        long transactionFee = signedTransaction.length * pwrj.getFeePerByte() + ECDSA_VERIFICATION_FEE;
        long ownerShareOfFees = (long) (transactionFee * 0.15);

        Response sendDataResponse = sender.sendPayableVmDataTransaction(vmId, value, data.getBytes(), sender.getNonce());
        assertTrue("Send payable VM data failed: " + sendDataResponse.getError(), sendDataResponse.isSuccess());

        // Wait for the payable VM data transaction to be processed
        Thread.sleep(5000);

        long finalOwnerBalance = vmOwner.getBalance();
        long finalSenderBalance = sender.getBalance();

        assertEquals(initialOwnerBalance + ownerShareOfFees, finalOwnerBalance);
        assertEquals(initialSenderBalance - value - transactionFee, finalSenderBalance);

        System.out.println("Payable VM data transaction test successful");
    }

    private static void testConduitTransactions(PWRWallet wallet1, PWRWallet wallet2, PWRJ pwrj) throws Exception {
        int wallet1Nonce = wallet1.getNonce();
        int wallet2Nonce = wallet2.getNonce();
        //Join wallets 1 and 2 as validators and delegate equal amounts of PWR
        if (!isValidator(wallet1.getAddress(), pwrj)) {
            System.out.println("Wallet 1 is not a validator. Joining as validator");
            Response joinResponse = wallet1.join("134.135.136.137", wallet1Nonce++);
            assertTrue("Join failed: " + joinResponse.getError(), joinResponse.isSuccess());

            while (!isValidator(wallet1.getAddress(), pwrj)) {
                Thread.sleep(1000);
            }

            Response delegateResponse = wallet1.delegate(wallet1.getAddress(), 1000000000, wallet1Nonce++);
            assertTrue("Delegate failed: " + delegateResponse.getError(), delegateResponse.isSuccess());

            while (pwrj.getDelegatedPWR(wallet1.getAddress(), wallet1.getAddress()) < 1000000000) {
                Thread.sleep(1000);
            }

            System.out.println("Wallet 1 is now a validator");
        } else {
            System.out.println("Wallet 1 is already a validator");
        }

        if (!isValidator(wallet2.getAddress(), pwrj)) {
            System.out.println("Wallet 2 is not a validator. Joining as validator");
            Response joinResponse = wallet2.join("124.125.126.127", wallet2Nonce++);
            assertTrue("Join failed: " + joinResponse.getError(), joinResponse.isSuccess());

            while (!isValidator(wallet2.getAddress(), pwrj)) {
                Thread.sleep(1000);
            }

            Response delegateResponse = wallet2.delegate(wallet2.getAddress(), 1000000000, wallet2Nonce++);
            assertTrue("Delegate failed: " + delegateResponse.getError(), delegateResponse.isSuccess());

            while (pwrj.getDelegatedPWR(wallet2.getAddress(), wallet2.getAddress()) < 1000000000) {
                Thread.sleep(1000);
            }

            System.out.println("Wallet 2 is now a validator");
        } else {
            System.out.println("Wallet 2 is already a validator");
        }


        System.out.println("Claiming VM ID");
        long vmId = new Random().nextLong();

        Response claimResponse = wallet1.claimVmId(vmId, wallet1Nonce++);
        assertTrue("Claim VM ID failed: " + claimResponse.getError(), claimResponse.isSuccess());

        while (pwrj.getOwnerOfVm(vmId) == null) {
            Thread.sleep(1000);
        }

        System.out.println("VM ID claimed by: " + pwrj.getOwnerOfVm(vmId));

        byte[] conduit1 = Hex.decode(wallet1.getAddress());
        List<byte[]> conduits = List.of(conduit1);

        System.out.println("Setting conduits");

        Response setConduitsResponse = wallet1.setConduits(vmId, conduits, wallet1Nonce++);
        assertTrue("Set conduits failed: " + setConduitsResponse.getError(), setConduitsResponse.isSuccess());

        while(pwrj.getConduitsOfVm(vmId).size() == 0) {
            Thread.sleep(1000);
        }

        System.out.println("Set conduits test successful");

        System.out.println("Adding conduit");

        byte[] conduit2 = Hex.decode(wallet2.getAddress());
        int vmNonce = pwrj.getNonceOfAddress(pwrj.getVmIdAddress(vmId));
        byte[] addConduitTxn = TransactionBuilder.getAddConduitsTransaction(vmId, List.of(conduit2), vmNonce, pwrj.getChainId());
        System.out.println("Local wallet 1 nonce: " + wallet1Nonce);
        System.out.println("CHain wallet 1 nonce: " + pwrj.getNonceOfAddress(wallet1.getAddress()));
        Response addConduitResponse = wallet1.conduitApprove(vmId, List.of(addConduitTxn), wallet1Nonce++);
        assertTrue("Add conduit failed: " + addConduitResponse.getError(), addConduitResponse.isSuccess());

        while(pwrj.getConduitsOfVm(vmId).size() == 1) {
            Thread.sleep(1000);
        }

        System.out.println("Add conduit test successful");

        System.out.println("Removing conduit");

        byte[] conduitRemovalTxn = TransactionBuilder.getRemoveConduitsTransaction(vmId, List.of(conduit2), vmNonce + 1, pwrj.getChainId());
        Response removeConduitResponse1 = wallet1.conduitApprove(vmId, List.of(conduitRemovalTxn), wallet1Nonce++);
        assertTrue("Remove conduit failed: " + removeConduitResponse1.getError(), removeConduitResponse1.isSuccess());

        Response removeConduitResponse2 = wallet2.conduitApprove(vmId, List.of(conduitRemovalTxn), wallet2Nonce++);
        assertTrue("Remove conduit failed: " + removeConduitResponse2.getError(), removeConduitResponse2.isSuccess());

        while(pwrj.getConduitsOfVm(vmId).size() == 2) {
            Thread.sleep(1000);
        }

        System.out.println("Remove conduit test successful");
    }
    private static void testTransferPWRFromVM(PWRWallet vmOwner, PWRWallet recipient, PWRJ pwrj) throws Exception {
        int vmOwnerNonce = vmOwner.getNonce();
        long vmId = new Random().nextLong();
        long transferAmount = 1000000000;
        long payableAmount = 10000000000L;

        System.out.println("Claiming VM ID");

        Response claimResponse = vmOwner.claimVmId(vmId, vmOwnerNonce++);
        assertTrue("Claim VM ID failed: " + claimResponse.getError(), claimResponse.isSuccess());

        while (pwrj.getOwnerOfVm(vmId) == null) {
            Thread.sleep(1000);
        }

        System.out.println("VM ID claimed by: " + pwrj.getOwnerOfVm(vmId));

        System.out.println("Setting conduits");

        byte[] conduit = Hex.decode(vmOwner.getAddress());
        List<byte[]> conduits = List.of(conduit);

        Response setConduitsResponse = vmOwner.setConduits(vmId, conduits, vmOwnerNonce++);
        assertTrue("Set conduits failed: " + setConduitsResponse.getError(), setConduitsResponse.isSuccess());

        while (pwrj.getConduitsOfVm(vmId).size() == 0) {
            Thread.sleep(1000);
        }

        System.out.println("Set conduits successful");

        long initialVMBalance = pwrj.getBalanceOfAddress(pwrj.getVmIdAddress(vmId));
        long initialRecipientBalance = recipient.getBalance();

        System.out.println("Sending payable VM data");

        // Send a payable VM transaction to transfer PWR to the VM
        Response payableVMResponse = vmOwner.sendPayableVmDataTransaction(vmId, payableAmount, "Payable VM Data".getBytes(), vmOwnerNonce++);
        assertTrue("Payable VM transaction failed: " + payableVMResponse.getError(), payableVMResponse.isSuccess());

        Thread.sleep(5000);

        System.out.println("Sending transfer PWR transaction from VM " + vmId + " to recipient " + recipient.getAddress() + " for amount " + transferAmount);

        int vmNonce = pwrj.getNonceOfAddress(pwrj.getVmIdAddress(vmId));
        byte[] transferTxn = TransactionBuilder.getTransferPWRTransaction(recipient.getAddress(), transferAmount, vmNonce, pwrj.getChainId());

        Response conduitApprovalResponse = vmOwner.conduitApprove(vmId, List.of(transferTxn), vmOwnerNonce++);
        assertTrue("Conduit approval failed: " + conduitApprovalResponse.getError(), conduitApprovalResponse.isSuccess());

        Thread.sleep(5000);

        long finalVMBalance = pwrj.getBalanceOfAddress(pwrj.getVmIdAddress(vmId));
        long finalRecipientBalance = recipient.getBalance();

        assertEquals(initialVMBalance + payableAmount - transferAmount, finalVMBalance);
        assertEquals(initialRecipientBalance + transferAmount, finalRecipientBalance);

        System.out.println("Transfer PWR from VM test successful");
    }

    private static boolean isValidator(String validatorAddress, PWRJ pwrj) throws Exception {
        List<Validator> validators = pwrj.getAllValidators();
        for (Validator v : validators) {
            if (v.getAddress().equalsIgnoreCase(validatorAddress)) {
                return true;
            }
        }
        return false;
    }
}