package com.github.pwrlabs.pwrj;

import com.github.pwrlabs.pwrj.Utils.Response;
import com.github.pwrlabs.pwrj.protocol.PWRJ;
import com.github.pwrlabs.pwrj.wallet.PWRWallet;

import java.math.BigInteger;

public class Main {
    public static void main(String[] args) {
        System.out.println("hi");
        System.exit(0);
        //Test for all the functions
        try {
            PWRJ pwrj = new PWRJ("https://pwrrpc.pwrlabs.io/");

            //Change the value if testing from another tool
            PWRWallet user = new PWRWallet(new BigInteger("19025338099182849188500822369817708178555441129124871592504836170414925188819"), pwrj);

            //Change the value if testing from another tool
            PWRWallet guardian = new PWRWallet(new BigInteger("19025338099182849188500822369817708178555441129124871592504836170414925188851"), pwrj);
//
//            guardianTest(user, guardian, pwrj);
//            System.exit(0);

            //Change the value if testing from another tool
            long vmId = 897435;
            System.out.println("User Wallet Address: " + user.getAddress());
            System.out.println("Guardian Wallet Address: " + guardian.getAddress());
            //System.exit(0);

            int nonce = user.getNonce();

            Response r = user.transferPWR("0x8953f1c3B53Bd9739F78dc8B0CD5DB9686C40b09", 1000000000, nonce);
            System.out.println("Transfer PWR success: " + r.isSuccess());
            System.out.println("Transfer PWR txn hash: " + r.getTxnHash());
            System.out.println("Transfer PWR error: " + r.getError());
            System.out.println();

            ++nonce;
            r = user.delegate("0x61Bd8fc1e30526Aaf1C4706Ada595d6d236d9883", 1000000000, nonce);
            System.out.println("Delegate success: " + r.isSuccess());
            System.out.println("Delegate txn hash: " + r.getTxnHash());
            System.out.println("Delegate error: " + r.getError());
            System.out.println();

            ++nonce;
            r = user.claimVmId(vmId, nonce);
            System.out.println("Claim VM ID success: " + r.isSuccess());
            System.out.println("Claim VM ID txn hash: " + r.getTxnHash());
            System.out.println("Claim VM ID error: " + r.getError());
            System.out.println();

            ++nonce;
            r = user.sendVmDataTxn(vmId, "Hello World".getBytes(), nonce);
            System.out.println("Send VM Data success: " + r.isSuccess());
            System.out.println("Send VM Data txn hash: " + r.getTxnHash());
            System.out.println("Send VM Data error: " + r.getError());
            System.out.println();

            ++nonce;
            r = user.sendPayableVmDataTxn(vmId, 10, "Hello World".getBytes(), nonce);
            System.out.println("Send Payable VM Data success: " + r.isSuccess());
            System.out.println("Send Payable VM Data txn hash: " + r.getTxnHash());
            System.out.println("Send Payable VM Data error: " + r.getError());
            System.out.println();

            while(pwrj.getDelegatedPWR(user.getAddress(), "0x61Bd8fc1e30526Aaf1C4706Ada595d6d236d9883") == 0) {
                Thread.sleep(1000);
            }

            //TODO send this after the delegation has been completed
            ++nonce;
            r = user.withdraw("0x61Bd8fc1e30526Aaf1C4706Ada595d6d236d9883", 10, nonce);
            System.out.println("Withdraw success: " + r.isSuccess());
            System.out.println("Withdraw txn hash: " + r.getTxnHash());
            System.out.println("Withdraw error: " + r.getError());
            System.out.println();

            //TODO send this after the delegation has been completed
            ++nonce;
            r = user.withdrawPWR("0x61Bd8fc1e30526Aaf1C4706Ada595d6d236d9883", 10000000, nonce);
            System.out.println("Withdraw PWR success: " + r.isSuccess());
            System.out.println("Withdraw PWR txn hash: " + r.getTxnHash());
            System.out.println("Withdraw PWR error: " + r.getError());

            Thread.sleep(10000);
            guardianTest(user, guardian, pwrj);
        } catch (Exception e) {

        }
    }

    public static void guardianTest(PWRWallet user, PWRWallet guardian, PWRJ pwrj) {
        try {
            int nonce = user.getNonce();
//            Response r = user.setGuardian("0x61Bd8fc1e30526Aaf1C4706Ada595d6d236d9883", 1000000000, nonce);
//            System.out.println("Set Guardian success: " + r.isSuccess());
//            System.out.println("Set Guardian txn hash: " + r.getTxnHash());
//            System.out.println("Set Guardian error: " + r.getError());

            while(pwrj.getGuardianOfAddress(user.getAddress()) == null) {
                Thread.sleep(1000);
            }

            nonce = user.getNonce();
            byte[] transferTxn = user.getSignedTransferPWRTxn("0x61Bd8fc1e30526Aaf1C4706Ada595d6d236d9883", 1000, nonce);

            int guardianNonce = guardian.getNonce();
            Response r = guardian.sendGuardianWrappedTransaction(transferTxn, guardianNonce);
            System.out.println("Send Guardian Wrapped Transaction success: " + r.isSuccess());
            System.out.println("Send Guardian Wrapped Transaction txn hash: " + r.getTxnHash());
            System.out.println("Send Guardian Wrapped Transaction error: " + r.getError());

            ++nonce;
            byte[] removeGuardianTxn = user.getSignedRemoveGuardianTxn(nonce);

            ++guardianNonce;
            r = guardian.sendGuardianWrappedTransaction(removeGuardianTxn, guardianNonce);
            System.out.println("Remove Guardian success: " + r.isSuccess());
            System.out.println("Remove Guardian txn hash: " + r.getTxnHash());
            System.out.println("Remove Guardian error: " + r.getError());

        } catch (Exception e) {
            System.out.println("Guardian test failed: " + e.getMessage());
        }
    }
}
