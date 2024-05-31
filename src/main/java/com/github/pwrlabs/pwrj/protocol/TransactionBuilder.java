package com.github.pwrlabs.pwrj.protocol;

import com.github.pwrlabs.pwrj.Utils.Hex;
import org.bouncycastle.jcajce.provider.digest.Keccak;
import org.bouncycastle.jce.provider.BouncyCastleProvider;
import org.bouncycastle.math.ec.ECPoint;
import org.bouncycastle.math.ec.FixedPointCombMultiplier;

import java.io.IOException;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.security.SecureRandom;
import java.security.Security;
import java.time.Instant;
import java.util.List;

public class TransactionBuilder {

    /**
     * Gets the base transaction bytes for a given identifier, nonce, and chainId.
     *
     * @param identifier The transaction identifier.
     * @param nonce      The transaction nonce.
     * @param chainId    The chain ID.
     * @return The base transaction bytes.
     * @throws IOException If an I/O error occurs.
     */
    private static byte[] getTransactionBase(byte identifier, int nonce, byte chainId) {
        ByteBuffer buffer = ByteBuffer.allocate(6);
        buffer.put(identifier);
        buffer.put(chainId);
        buffer.putInt(nonce);
        return buffer.array();
    }

    private static void assetAddressValidity(String address) {
        if (address == null || (address.length() != 40 && address.length() != 42))
            throw new RuntimeException("Invalid address");
    }

    /**
     * Returns the transaction of transferring PWR tokens to a specified address.
     *
     * @param to      The recipient address.
     * @param amount  The amount of PWR tokens to be transferred.
     * @param nonce   The transaction nonce.
     * @param chainId The chain ID.
     * @return A byte array that represents the transaction of this method.
     * @throws RuntimeException For various transaction-related validation issues.
     */
    public static byte[] getTransferPWRTransaction(String to, long amount, int nonce, byte chainId) {
        assetAddressValidity(to);

        if (amount < 0) {
            throw new RuntimeException("Amount cannot be negative");
        }
        if (nonce < 0) {
            throw new RuntimeException("Nonce cannot be negative");
        }

        byte[] TransactionBase = getTransactionBase((byte) 0, nonce, chainId);
        ByteBuffer buffer = ByteBuffer.allocate(TransactionBase.length + 8 + 20);
        buffer.put(TransactionBase);
        buffer.putLong(amount);
        buffer.put(Hex.decode(to));

        return buffer.array();
    }

    /**
     * Returns the transaction of joining the PWR network as a standby validator.
     *
     * @param ip      The IP address of the validator.
     * @param nonce   The transaction nonce.
     * @param chainId The chain ID.
     * @return A byte array that represents the transaction of this method.
     */
    public static byte[] getJoinTransaction(String ip, int nonce, byte chainId) {
        byte[] TransactionBase = getTransactionBase((byte) 1, nonce, chainId);
        byte[] ipBytes = ip.getBytes(StandardCharsets.UTF_8);

        ByteBuffer buffer = ByteBuffer.allocate(TransactionBase.length + ipBytes.length);
        buffer.put(TransactionBase);
        buffer.put(ip.getBytes(StandardCharsets.UTF_8));

        return buffer.array();
    }

    /**
     * Returns the transaction of claiming an active node spot on the PWR network.
     *
     * @param nonce   The transaction nonce.
     * @param chainId The chain ID.
     * @return A byte array that represents a signed transaction of the method.
     */
    public static byte[] getClaimActiveNodeSpotTransaction(int nonce, byte chainId) {
        byte[] TransactionBase = getTransactionBase((byte) 2, nonce, chainId);

        return TransactionBase;
    }

    /**
     * Returns the transaction of delegating PWR tokens to a specified validator.
     *
     * @param validator The validator address.
     * @param amount    The amount of PWR tokens to be delegated.
     * @param nonce     The transaction nonce.
     * @param chainId   The chain ID.
     * @return A byte array that represents the transaction of the method.
     * @throws RuntimeException For various transaction-related validation issues.
     */
    public static byte[] getDelegateTransaction(String validator, long amount, int nonce, byte chainId) {
        assetAddressValidity(validator);
        if (amount < 0) {
            throw new RuntimeException("Amount cannot be negative");
        }
        if (nonce < 0) {
            throw new RuntimeException("Nonce cannot be negative");
        }

        byte[] TransactionBase = getTransactionBase((byte) 3, nonce, chainId);
        ByteBuffer buffer = ByteBuffer.allocate(TransactionBase.length + 28);
        buffer.put(TransactionBase);
        buffer.putLong(amount);
        buffer.put(Hex.decode(validator));

        return buffer.array();
    }

    /**
     * Returns the transaction of withdrawing PWR tokens from a specified validator.
     *
     * @param validator    The validator address.
     * @param sharesAmount The amount of shares to be withdrawn.
     * @param nonce        The transaction nonce.
     * @param chainId      The chain ID.
     * @return A byte array that represents the transaction of this method.
     * @throws RuntimeException For various transaction-related validation issues.
     */
    public static byte[] getWithdrawTransaction(String validator, long sharesAmount, int nonce, byte chainId) {
        assetAddressValidity(validator);
        if (sharesAmount < 0) {
            throw new RuntimeException("Shares amount cannot be negative");
        }
        if (nonce < 0) {
            throw new RuntimeException("Nonce cannot be negative");
        }

        byte[] TransactionBase = getTransactionBase((byte) 4, nonce, chainId);
        ByteBuffer buffer = ByteBuffer.allocate(TransactionBase.length + 28);
        buffer.put(TransactionBase);
        buffer.putLong(sharesAmount);
        buffer.put(Hex.decode(validator));

        return buffer.array();
    }

    /**
     * Returns the transaction of sending data to a specified VM on the PWR network.
     *
     * @param vmId    The ID of the VM.
     * @param data    The data to be sent.
     * @param nonce   The transaction nonce.
     * @param chainId The chain ID.
     * @return A byte array that represents the transaction of this method.
     * @throws RuntimeException For various transaction-related validation issues.
     */
    public static byte[] getVmDataTransaction(long vmId, byte[] data, int nonce, byte chainId) {
        if (nonce < 0) {
            throw new RuntimeException("Nonce cannot be negative");
        }

        byte[] TransactionBase = getTransactionBase((byte) 5, nonce, chainId);
        ByteBuffer buffer = ByteBuffer.allocate(TransactionBase.length + 8 + data.length);
        buffer.put(TransactionBase);
        buffer.putLong(vmId);
        buffer.put(data);

        return buffer.array();
    }

    /**
     * Returns the transaction of sending a transaction to claim a Virtual Machine ID on the PWR network, ensuring its owner 15% revenue of all transaction fees paid when transacting with this VM.
     *
     * @param vmId    The ID of the VM.
     * @param nonce   The transaction nonce.
     * @param chainId The chain ID.
     * @return A byte array that represents the transaction of this method.
     */
    public static byte[] getClaimVmIdTransaction(long vmId, int nonce, byte chainId) {
        byte[] TransactionBase = getTransactionBase((byte) 6, nonce, chainId);
        ByteBuffer buffer = ByteBuffer.allocate(TransactionBase.length + 8);
        buffer.put(TransactionBase);
        buffer.putLong(vmId);

        return buffer.array();
    }

    /**
     * Returns a transaction of setting a guardian.
     *
     * @param guardian   The wallet address of the chosen guardian.
     * @param expiryDate The expiry date after which the guardian will have revoked privileges.
     * @param nonce      The transaction nonce.
     * @param chainId    The chain ID.
     * @return A byte array with the outcome of this transaction.
     * @throws RuntimeException For various transaction-related validation issues.
     */
    public static byte[] getSetGuardianTransaction(String guardian, long expiryDate, int nonce, byte chainId) {
        assetAddressValidity(guardian);

        if (nonce < 0) {
            throw new RuntimeException("Nonce cannot be negative");
        }
        if (expiryDate < 0) {
            throw new RuntimeException("Expiry date cannot be negative");
        }
        if (expiryDate < Instant.now().getEpochSecond()) {
            throw new RuntimeException("Expiry date cannot be in the past");
        }

        byte[] TransactionBase = getTransactionBase((byte) 8, nonce, chainId);
        ByteBuffer buffer = ByteBuffer.allocate(TransactionBase.length + 20 + 8);
        buffer.put(TransactionBase);
        buffer.putLong(expiryDate);
        buffer.put(Hex.decode(guardian));

        return buffer.array();
    }

    /**
     * Returns the transaction of removing/revoking a guardian wallet.
     *
     * @param nonce   The transaction nonce.
     * @param chainId The chain ID.
     * @return A byte array with the outcome of the transaction.
     */
    public static byte[] getRemoveGuardianTransaction(int nonce, byte chainId) {
        byte[] TransactionBase = getTransactionBase((byte) 9, nonce, chainId);

        return TransactionBase;
    }

    /**
     * Returns the transaction of approving a set of transactions for a guardian wallet.
     *
     * @param transactions The transactions to be approved.
     * @param nonce        The transaction nonce.
     * @param chainId      The chain ID.
     * @return A byte array representing the transaction of this method
     */
    public static byte[] getGuardianApprovalTransaction(List<byte[]> transactions, int nonce, byte chainId) {
        int totalLength = 0;
        for (byte[] Transaction : transactions) {
            totalLength += Transaction.length;
        }

        byte[] TransactionBase = getTransactionBase((byte) 10, nonce, chainId);
        ByteBuffer buffer = ByteBuffer.allocate(TransactionBase.length + (transactions.size() * 4) + totalLength);
        buffer.put(TransactionBase);

        for (byte[] Transaction : transactions) {
            buffer.putInt(Transaction.length);
            buffer.put(Transaction);
        }

        return buffer.array();
    }

    /**
     * Returns the transaction of sending payable data to a specified VM on the PWR network.
     *
     * @param vmId    The ID of the VM.
     * @param value   The amount of PWR tokens to be sent.
     * @param data    The data to be sent.
     * @param nonce   The transaction nonce.
     * @param chainId The chain ID.
     * @return A byte array that represents the transaction of this method.
     * @throws RuntimeException For various transaction-related validation issues.
     */
    public static byte[] getPayableVmDataTransaction(long vmId, long value, byte[] data, int nonce, byte chainId) {
        if (nonce < 0) {
            throw new RuntimeException("Nonce cannot be negative");
        }

        byte[] TransactionBase = getTransactionBase((byte) 11, nonce, chainId);
        ByteBuffer buffer = ByteBuffer.allocate(TransactionBase.length + 16 + data.length);
        buffer.put(TransactionBase);
        buffer.putLong(vmId);
        buffer.put(data);
        buffer.putLong(value);

        return buffer.array();
    }

    /**
     * Returns the transaction of sending the transaction to remove a validator/**
     * Returns the transaction of sending the transaction to remove a validator.
     *
     * @param validator The address of the validator to be removed.
     * @param nonce     The transaction nonce.
     * @param chainId   The chain ID.
     * @return A byte array representing the transaction of this method.
     * @throws RuntimeException If the validator address is invalid.
     */
    public static byte[] getValidatorRemoveTransaction(String validator, int nonce, byte chainId) {
        assetAddressValidity(validator);

        if (nonce < 0) {
            throw new RuntimeException("Nonce cannot be negative");
        }

        byte[] TransactionBase = getTransactionBase((byte) 7, nonce, chainId);
        ByteBuffer buffer = ByteBuffer.allocate(TransactionBase.length + 20);
        buffer.put(TransactionBase);
        buffer.put(Hex.decode(validator));

        return buffer.array();
    }

    /**
     * Returns the transaction for approving a set of transactions for a specific VM.
     *
     * @param vmId         The ID of the VM.
     * @param transactions The transactions to be approved.
     * @param nonce        The transaction nonce.
     * @param chainId      The chain ID.
     * @return A byte array representing the transaction of this method.
     * @throws RuntimeException If the nonce is negative or there are no transactions to approve.
     */
    public static byte[] getConduitApprovalTransaction(long vmId, List<byte[]> transactions, int nonce, byte chainId) {
        if (nonce < 0) {
            throw new RuntimeException("Nonce cannot be negative");
        }
        if (transactions.size() == 0) {
            throw new RuntimeException("No transactions to approve");
        }

        int totalTransactionsLength = 0;
        for (byte[] Transaction : transactions) {
            totalTransactionsLength += Transaction.length;
        }

        byte[] TransactionBase = getTransactionBase((byte) 12, nonce, chainId);
        ByteBuffer buffer = ByteBuffer.allocate(TransactionBase.length + 8 + (transactions.size() * 4) + totalTransactionsLength);
        buffer.put(TransactionBase);
        buffer.putLong(vmId);

        for (byte[] Transaction : transactions) {
            buffer.putInt(Transaction.length);
            buffer.put(Transaction);
        }

        return buffer.array();
    }

    /**
     * Returns the transaction for setting a list of conduits for a specific VM.
     *
     * @param vmId     The ID of the VM.
     * @param conduits The list of conduits to be set.
     * @param nonce    The transaction nonce.
     * @param chainId  The chain ID.
     * @return A byte array representing the transaction of this method.
     * @throws RuntimeException If the nonce is negative or there are no conduits to set.
     */
    public static byte[] getSetConduitsTransaction(long vmId, List<byte[]> conduits, int nonce, byte chainId) {
        if (nonce < 0) {
            throw new RuntimeException("Nonce cannot be negative");
        }
        if (conduits.size() == 0) {
            throw new RuntimeException("No transactions to approve");
        }

        int totalConduitLength = 0;
        for (byte[] conduit : conduits) {
            totalConduitLength += conduit.length;
        }

        byte[] TransactionBase = getTransactionBase((byte) 13, nonce, chainId);
        ByteBuffer buffer = ByteBuffer.allocate(TransactionBase.length + 8 + (conduits.size() * 4) + totalConduitLength);
        buffer.put(TransactionBase);
        buffer.putLong(vmId);

        for (byte[] conduit : conduits) {
            buffer.putInt(conduit.length);
            buffer.put(conduit);
        }

        return buffer.array();
    }

    /**
     * Returns the transaction for adding a list of conduits to a specific VM.
     *
     * @param vmId     The ID of the VM.
     * @param conduits The list of conduits to be added.
     * @param nonce    The transaction nonce.
     * @param chainId  The chain ID.
     * @return A byte array representing the transaction of this method.
     * @throws RuntimeException If the nonce is negative or there are no conduits to add.
     */
    public static byte[] getAddConduitsTransaction(long vmId, List<byte[]> conduits, int nonce, byte chainId) {
        if (nonce < 0) {
            throw new RuntimeException("Nonce cannot be negative");
        }
        if (conduits.size() == 0) {
            throw new RuntimeException("No conduits provided");
        }

        byte[] TransactionBase = getTransactionBase((byte) 14, nonce, chainId);
        ByteBuffer buffer = ByteBuffer.allocate(TransactionBase.length + 8 + (conduits.size() * 20));
        buffer.put(TransactionBase);
        buffer.putLong(vmId);

        for (byte[] conduit : conduits) {
            buffer.put(conduit);
        }

        return buffer.array();
    }

    /**
     * Returns the transaction for removing a list of conduits from a specific VM.
     *
     * @param vmId     The ID of the VM.
     * @param conduits The list of conduits to be removed.
     * @param nonce    The transaction nonce.
     * @param chainId  The chain ID.
     * @return A byte array representing the transaction of this method.
     * @throws RuntimeException If the nonce is negative or there are no conduits to remove.
     */
    public static byte[] getRemoveConduitsTransaction(long vmId, List<byte[]> conduits, int nonce, byte chainId) {
        if (nonce < 0) {
            throw new RuntimeException("Nonce cannot be negative");
        }
        if (conduits.size() == 0) {
            throw new RuntimeException("No conduits provided");
        }

        byte[] TransactionBase = getTransactionBase((byte) 15, nonce, chainId);
        ByteBuffer buffer = ByteBuffer.allocate(TransactionBase.length + 8 + (conduits.size() * 20));
        buffer.put(TransactionBase);
        buffer.putLong(vmId);

        for (byte[] conduit : conduits) {
            buffer.put(conduit);
        }

        return buffer.array();
    }

    /**
     * Returns the transaction for moving a specified amount of stake from one validator to another.
     *
     * @param sharesAmount  The amount of shares to be moved.
     * @param fromValidator The address of the validator to move stake from.
     * @param toValidator   The address of the validator to move stake to.
     * @param nonce         The transaction nonce.
     * @param chainId       The chain ID.
     * @return A byte array representing the transaction of this method.
     */
    public static byte[] getMoveStakeTransaction(long sharesAmount, String fromValidator, String toValidator, int nonce, byte chainId) {
        assetAddressValidity(fromValidator);
        assetAddressValidity(toValidator);

        byte[] TransactionBase = getTransactionBase((byte) 16, nonce, chainId);
        ByteBuffer buffer = ByteBuffer.allocate(TransactionBase.length + 48);
        buffer.put(TransactionBase);
        buffer.putLong(sharesAmount);
        buffer.put(Hex.decode(fromValidator));
        buffer.put(Hex.decode(toValidator));

        return buffer.array();
    }


    public static byte[] getChangeEarlyWithdrawPenaltyProposalTxn(long withdrawalPenaltyTime, long withdrawalPenalty, String description, int nonce, byte chainId) {
        byte[] TransactionBase = getTransactionBase((byte) 17, nonce, chainId);
        byte[] descriptionBytea = description.getBytes(StandardCharsets.UTF_8);

        ByteBuffer buffer = ByteBuffer.allocate(TransactionBase.length + 16 + descriptionBytea.length);
        buffer.put(TransactionBase);
        buffer.putLong(withdrawalPenaltyTime);
        buffer.putLong(withdrawalPenalty);
        buffer.put(description.getBytes(StandardCharsets.UTF_8));
        return buffer.array();
    }

    public static byte[] getChangeFeePerByteProposalTxn(long feePerByte, String description, int nonce, byte chainId) {

        byte[] TransactionBase = getTransactionBase((byte) 18, nonce, chainId);
        ByteBuffer buffer = ByteBuffer.allocate(TransactionBase.length + 48);
        buffer.put(TransactionBase);
        buffer.putLong(feePerByte);
        buffer.put(description.getBytes(StandardCharsets.UTF_8));

        return buffer.array();
    }

    public static byte[] getChangeMaxBlockSizeProposalTxn(int maxBlockSize, String description, int nonce, byte chainId) {

        byte[] TransactionBase = getTransactionBase((byte) 16, nonce, chainId);
        ByteBuffer buffer = ByteBuffer.allocate(TransactionBase.length + 48);
        buffer.put(TransactionBase);
        buffer.putInt(maxBlockSize);
        buffer.put(description.getBytes(StandardCharsets.UTF_8));

        return buffer.array();
    }

    public static byte[] getChangeMaxTxnSizeProposalTxn(int maxTxnSize, String description, int nonce, byte chainId) {

        byte[] TransactionBase = getTransactionBase((byte) 16, nonce, chainId);
        ByteBuffer buffer = ByteBuffer.allocate(TransactionBase.length + 48);
        buffer.put(TransactionBase);
        buffer.putInt(maxTxnSize);
        buffer.put(description.getBytes(StandardCharsets.UTF_8));

        return buffer.array();
    }

    public static byte[] getChangeOverallBurnPercentageProposalTxn(int burnPercentage, String description, int nonce, byte chainId) {

        byte[] TransactionBase = getTransactionBase((byte) 16, nonce, chainId);
        ByteBuffer buffer = ByteBuffer.allocate(TransactionBase.length + 48);
        buffer.put(TransactionBase);
        buffer.putInt(burnPercentage);
        buffer.put(description.getBytes(StandardCharsets.UTF_8));

        return buffer.array();
    }

    public static byte[] getChangeRewardPerYearProposalTxn(long rewardPerYear, String description, int nonce, byte chainId) {

        byte[] TransactionBase = getTransactionBase((byte) 16, nonce, chainId);
        ByteBuffer buffer = ByteBuffer.allocate(TransactionBase.length + 48);
        buffer.put(TransactionBase);
        buffer.putLong(rewardPerYear);
        buffer.put(description.getBytes(StandardCharsets.UTF_8));

        return buffer.array();
    }

    public static byte[] getChangeValidatorCountLimitProposalTxn(int validatorCountLimit, String description, int nonce, byte chainId) {

        byte[] TransactionBase = getTransactionBase((byte) 16, nonce, chainId);
        ByteBuffer buffer = ByteBuffer.allocate(TransactionBase.length + 48);
        buffer.put(TransactionBase);
        buffer.putLong(validatorCountLimit);
        buffer.put(description.getBytes(StandardCharsets.UTF_8));

        return buffer.array();
    }

    public static byte[] getChangeValidatorJoiningFeeProposalTxn(long joiningFee, String description, int nonce, byte chainId) {

        byte[] TransactionBase = getTransactionBase((byte) 16, nonce, chainId);
        ByteBuffer buffer = ByteBuffer.allocate(TransactionBase.length + 48);
        buffer.put(TransactionBase);
        buffer.putLong(joiningFee);
        buffer.put(description.getBytes(StandardCharsets.UTF_8));

        return buffer.array();
    }

    public static byte[] getChangeVmIdClaimingFeeProposalTxn(long claimingFee, String description, int nonce, byte chainId) {

        byte[] TransactionBase = getTransactionBase((byte) 16, nonce, chainId);
        ByteBuffer buffer = ByteBuffer.allocate(TransactionBase.length + 48);
        buffer.put(TransactionBase);
        buffer.putLong(claimingFee);
        buffer.put(description.getBytes(StandardCharsets.UTF_8));

        return buffer.array();
    }

    public static byte[] getChangeVmOwnerTxnFeeShareProposalTxn(long feeShare, String description, int nonce, byte chainId) {

        byte[] TransactionBase = getTransactionBase((byte) 16, nonce, chainId);
        ByteBuffer buffer = ByteBuffer.allocate(TransactionBase.length + 48);
        buffer.put(TransactionBase);
        buffer.putLong(feeShare);
        buffer.put(description.getBytes(StandardCharsets.UTF_8));

        return buffer.array();
    }

    public static byte[] getOtherProposalTxn(String description, int nonce, byte chainId) {

        byte[] TransactionBase = getTransactionBase((byte) 16, nonce, chainId);
        ByteBuffer buffer = ByteBuffer.allocate(TransactionBase.length + 48);
        buffer.put(TransactionBase);
        buffer.put(description.getBytes(StandardCharsets.UTF_8));

        return buffer.array();
    }

    public static byte[] getVoteOnProposalTxn(int proposalId, int vote, int nonce, byte chainId) {

        byte[] TransactionBase = getTransactionBase((byte) 16, nonce, chainId);
        ByteBuffer buffer = ByteBuffer.allocate(TransactionBase.length + 48);
        buffer.put(TransactionBase);
        buffer.putInt(proposalId);
        buffer.putInt(vote);

        return buffer.array();
    }

}