import com.github.pwrlabs.pwrj.entities.FalconTransaction;
import com.github.pwrlabs.pwrj.protocol.PWRJ;
import com.github.pwrlabs.pwrj.entities.WithdrawalOrder;
import com.github.pwrlabs.pwrj.entities.Validator;
import com.github.pwrlabs.pwrj.wallet.PWRFalconWallet;
import com.github.pwrlabs.pwrj.record.response.Response;
import org.bouncycastle.util.encoders.Hex;

import java.io.IOException;
import java.math.BigInteger;
import java.util.List;

public class ValidatorAndDelegationTest {
    private static final PWRJ pwrj = new PWRJ("http://localhost:8085");
    private static long amountToDelegate;

    public static void main(String[] args) {
        try {
            // Create or load wallets
            PWRFalconWallet validator = new PWRFalconWallet(pwrj);
            PWRFalconWallet validator2 = new PWRFalconWallet(pwrj);
            PWRFalconWallet delegator = new PWRFalconWallet(pwrj);

            System.out.println("Validator Address: " + validator.getAddress());
            System.out.println("Validator2 Address: " + validator2.getAddress());
            System.out.println("Delegator Address: " + delegator.getAddress());

            System.out.println("Please supply wallets with funds and press enter to continue...");
            try {
                System.in.read();
            } catch (Exception e) {
                e.printStackTrace();
            }

            amountToDelegate = pwrj.getMinimumDelegatingAmount() * 2;

            // Run the test methods
            joinAsValidator(validator);
            joinAsValidator(validator2);
            changeIp(validator);
            delegate(delegator, validator);
            moveDelegation(delegator, Hex.decode(validator.getAddress().substring(2)),
                    Hex.decode(validator2.getAddress().substring(2)));
            withdraw(delegator, validator);
            claimActiveNodeSpot(validator);

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private static void joinAsValidator(PWRFalconWallet validator) throws Exception {
            // Get validator's balance
            long balance = pwrj.getBalanceOfAddress(validator.getAddress());

            // Check if validator has enough balance for joining fee
            long joiningFee = pwrj.getValidatorJoiningFee();

            if (balance < joiningFee) {
                throw new RuntimeException("Not enough balance to join as validator. Need at least: " + joiningFee);
            }

            // Join as validator with IP address
            String ipAddress = generateRandomIp(); // Example IP
            Response response = validator.joinAsValidator(pwrj.getFeePerByte(), ipAddress);

            if (!response.isSuccess()) {
                throw new RuntimeException("Failed to join as validator: " + response.getError());
            }

            waitUntilTransactionsIsProcessed(response.getTransactionHash());

            Validator v = pwrj.getValidator(validator.getAddress());
            if (v == null) {
                throw new RuntimeException("Validator not found");
            }

            if (!v.getIp().equalsIgnoreCase(ipAddress)) {
                throw new RuntimeException("Validator IP mismatch. Expected: " + ipAddress + ", got: " + v.getIp());
            }

            System.out.println("Validator join success");
    }

    private static void changeIp(PWRFalconWallet validator) {
        try {
            // Change validator's IP
            String newIp = generateRandomIp(); // New IP address
            Response response = validator.changeIp(newIp, pwrj.getFeePerByte());

            if (!response.isSuccess()) {
                throw new RuntimeException("Failed to change IP: " + response.getError());
            }

            waitUntilTransactionsIsProcessed(response.getTransactionHash());

            Validator v = pwrj.getValidator(validator.getAddress());
            if (v == null) {
                throw new RuntimeException("Validator not found");
            }

            if (!v.getIp().equalsIgnoreCase(newIp)) {
                throw new RuntimeException("Validator IP mismatch. Expected: " + newIp + ", got: " + v.getIp());
            }

            System.out.println("Validator IP change success");
        } catch (Exception e) {
            throw new RuntimeException("Error changing IP: " + e.getMessage());
        }
    }

    private static void delegate(PWRFalconWallet delegator, PWRFalconWallet validator) throws Exception {
        // Get delegator's balance
        long balance = pwrj.getBalanceOfAddress(delegator.getAddress());
        System.out.println("Delegator Balance: " + balance);

        if (balance < amountToDelegate) {
            throw new RuntimeException("Not enough balance to delegate. Need at least: " + amountToDelegate);
        }

        byte[] validatorAddress = Hex.decode(validator.getAddress().substring(2)); // Remove 0x prefix

        Response response = delegator.delegate(validatorAddress, amountToDelegate, pwrj.getFeePerByte());

        if (!response.isSuccess()) {
            throw new RuntimeException("Failed to delegate: " + response.getError());
        } else {
            System.out.println("Delegate transaction hash: " + response.getTransactionHash());
        }

        waitUntilTransactionsIsProcessed(response.getTransactionHash()); // Wait for transaction to be processed

        Thread.sleep(5000);
        // Verify delegation
        long delegatedAmount = pwrj.getDelegatedPWR(delegator.getAddress(), validator.getAddress());

        if (delegatedAmount <= 0) {
            throw new RuntimeException("Delegation not found or amount is zero: " + delegatedAmount);
        }

        System.out.println("Delegated PWR amount: " + delegatedAmount);
    }

    private static void moveDelegation(PWRFalconWallet delegator, byte[] fromValidator, byte[] toValidator) {
        try {
            // Get shares amount from current validator
            String fromValidatorAddress = "0x" + Hex.toHexString(fromValidator);
            long sharesAmount = pwrj.getSharesOfDelegator(delegator.getAddress(), fromValidatorAddress);

            if (sharesAmount <= 0) {
                throw new RuntimeException("No shares to move from validator " + fromValidatorAddress);
            }

            // Move stake to new validator (all shares)
            BigInteger sharesToMove = BigInteger.valueOf(sharesAmount).divide(BigInteger.valueOf(4));
            Response response = delegator.moveStake(sharesToMove, fromValidator, toValidator, pwrj.getFeePerByte());

            if (!response.isSuccess()) {
                throw new RuntimeException("Failed to move delegation: " + response.getError());
            } else {
                System.out.println("Move delegation transaction hash: " + response.getTransactionHash());
            }
            // Verify the move
            try {
                waitUntilTransactionsIsProcessed(response.getTransactionHash()); // Wait for transaction to be processed
                String toValidatorAddress = "0x" + Hex.toHexString(toValidator);
                long newDelegatedAmount = pwrj.getDelegatedPWR(delegator.getAddress(), toValidatorAddress);

                if (newDelegatedAmount <= 0) {
                    throw new RuntimeException("Delegation not found or amount is zero");
                }

            } catch (Exception e) {
                throw new RuntimeException("Error verifying delegation move: " + e.getMessage());
            }

        } catch (IOException e) {
            throw new RuntimeException("Error moving delegation: " + e.getMessage());
        }
    }

    private static void withdraw(PWRFalconWallet delegator, PWRFalconWallet validator) {
        try {
            // Get shares amount
            long sharesAmount = pwrj.getSharesOfDelegator(delegator.getAddress(), validator.getAddress());

            if (sharesAmount <= 0) {
                throw new RuntimeException("No shares to withdraw from validator " + validator.getAddress());
            }

            System.out.println("Withdrawing " + sharesAmount + " shares from validator " + validator.getAddress());

            // Withdraw all shares
            byte[] validatorAddress = Hex.decode(validator.getAddress().substring(2));
            Response response = delegator.withdraw(BigInteger.valueOf(sharesAmount).divide(BigInteger.valueOf(4)), validatorAddress, pwrj.getFeePerByte());

            if (!response.isSuccess()) {
                throw new RuntimeException("Failed to withdraw: " + response.getError());
            }

            System.out.println("Withdrawal transaction hash: " + response.getTransactionHash());

            waitUntilTransactionsIsProcessed(response.getTransactionHash());

            byte[] txnHash = Hex.decode(response.getTransactionHash().startsWith("0x") ? response.getTransactionHash().substring(2) : response.getTransactionHash());
            WithdrawalOrder withdrawalOrder = pwrj.getWithdrawalOrder(txnHash);

            if (withdrawalOrder == null) {
                throw new RuntimeException("Withdrawal order not found");
            }

            System.out.println("Withdrawal order details: " + withdrawalOrder.toJson().toString());

        } catch (Exception e) {
            System.err.println("Error withdrawing delegation: " + e.getMessage());
        }
    }

    private static void claimActiveNodeSpot(PWRFalconWallet validator) {
        try {
            // Claim active node spot
            Response response = validator.claimActiveNodeSpot(pwrj.getFeePerByte());

            if (!response.isSuccess()) {
                throw new RuntimeException("Failed to claim active node spot: " + response.getError());
            }

            waitUntilTransactionsIsProcessed(response.getTransactionHash());

            List<Validator> activeValidators = pwrj.getActiveValidators();

            boolean isActive = false;
            for (Validator v : activeValidators) {
                String address = v.getAddress().startsWith("0x") ? v.getAddress() : "0x" + v.getAddress();
                if (address.equalsIgnoreCase(validator.getAddress())) {
                    isActive = true;
                    break;
                }
            }

            if (!isActive) {
                throw new RuntimeException("Validator is not active");
            }

            System.out.println("Validator is now active");
        } catch (Exception e) {
            throw new RuntimeException("Error claiming active node spot: " + e.getMessage());
        }
    }

    //function to generate random ip address
    private static String generateRandomIp() {
        int octet1 = (int) (Math.random() * 256);
        int octet2 = (int) (Math.random() * 256);
        int octet3 = (int) (Math.random() * 256);
        int octet4 = (int) (Math.random() * 256);
        return octet1 + "." + octet2 + "." + octet3 + "." + octet4;
    }

    private static void waitUntilTransactionsIsProcessed(String txnHash) throws Exception {
        long maxTime = 10000;
        long timeNow = System.currentTimeMillis();

        while (System.currentTimeMillis() - timeNow < maxTime) {
            try {
                FalconTransaction txn = pwrj.getTransactionByHash(txnHash);
                if (txn != null) {
                    if (txn.isSuccess()) return;
                    else {
                        throw new Exception("Transaction failed: " + txn.getErrorMessage());
                    }
                }
            } catch (Exception e) {

            }

            try {
                Thread.sleep(100);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }

        throw  new Exception("Transaction not processed in time");
    }
}