package com.github.pwrlabs.pwrj.protocol;

import com.github.pwrlabs.pwrj.Utils.Falcon;
import com.github.pwrlabs.pwrj.Utils.Hex;
import com.github.pwrlabs.pwrj.Utils.ValidationException;
import org.bouncycastle.jcajce.provider.digest.Keccak;
import org.bouncycastle.jce.provider.BouncyCastleProvider;
import org.bouncycastle.jce.provider.PEMUtil;
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

import static com.github.pwrlabs.pwrj.Utils.NewError.errorIf;

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
    private static byte[] getTransactionBase(int identifier, int nonce, byte chainId) {
        ByteBuffer buffer = ByteBuffer.allocate(9);
        buffer.putInt(identifier);
        buffer.put(chainId);
        buffer.putInt(nonce);
        return buffer.array();
    }

    private static byte[] getFalconTransactionBase(int identifier, int nonce, byte chainId, long feePerByte, byte[] sender) {
        ByteBuffer buffer = ByteBuffer.allocate(37);
        buffer.putInt(identifier);
        buffer.put(chainId);
        buffer.putInt(nonce);
        buffer.putLong(feePerByte);
        buffer.put(sender);
        return buffer.array();
    }

    private static void assetAddressValidity(String address) {
        if (address == null || (address.length() != 40 && address.length() != 42))
            throw new RuntimeException("Invalid address");
    }

    //region - ECDSA Transactions

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

        byte[] transactionBase = getTransactionBase( 0, nonce, chainId);
        ByteBuffer buffer = ByteBuffer.allocate(transactionBase.length + 8 + 20);
        buffer.put(transactionBase);
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
        byte[] transactionBase = getTransactionBase( 1, nonce, chainId);
        byte[] ipBytes = ip.getBytes(StandardCharsets.UTF_8);

        ByteBuffer buffer = ByteBuffer.allocate(transactionBase.length + ipBytes.length);
        buffer.put(transactionBase);
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
        byte[] transactionBase = getTransactionBase( 2, nonce, chainId);

        return transactionBase;
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

        byte[] transactionBase = getTransactionBase( 3, nonce, chainId);
        ByteBuffer buffer = ByteBuffer.allocate(transactionBase.length + 28);
        buffer.put(transactionBase);
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

        byte[] transactionBase = getTransactionBase( 4, nonce, chainId);
        ByteBuffer buffer = ByteBuffer.allocate(transactionBase.length + 28);
        buffer.put(transactionBase);
        buffer.putLong(sharesAmount);
        buffer.put(Hex.decode(validator));

        return buffer.array();
    }

    /**
     * Returns the transaction of sending data to a specified VM on the PWR network.
     *
     * @param vidaId    The ID of the VM.
     * @param data    The data to be sent.
     * @param nonce   The transaction nonce.
     * @param chainId The chain ID.
     * @return A byte array that represents the transaction of this method.
     * @throws RuntimeException For various transaction-related validation issues.
     */
    public static byte[] getVmDataTransaction(long vidaId, byte[] data, int nonce, byte chainId) {
        if (nonce < 0) {
            throw new RuntimeException("Nonce cannot be negative");
        }

        byte[] transactionBase = getTransactionBase( 5, nonce, chainId);
        ByteBuffer buffer = ByteBuffer.allocate(transactionBase.length + 8 + 4 + data.length);
        buffer.put(transactionBase);
        buffer.putLong(vidaId);
        buffer.putInt(data.length);
        buffer.put(data);

        return buffer.array();
    }

    /**
     * Returns the transaction of sending a transaction to claim a Virtual Machine ID on the PWR network, ensuring its owner 15% revenue of all transaction fees paid when transacting with this VM.
     *
     * @param vidaId    The ID of the VM.
     * @param nonce   The transaction nonce.
     * @param chainId The chain ID.
     * @return A byte array that represents the transaction of this method.
     */
    public static byte[] getClaimVmIdTransaction(long vidaId, int nonce, byte chainId) {
        byte[] transactionBase = getTransactionBase( 6, nonce, chainId);
        ByteBuffer buffer = ByteBuffer.allocate(transactionBase.length + 8);
        buffer.put(transactionBase);
        buffer.putLong(vidaId);

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

        byte[] transactionBase = getTransactionBase( 8, nonce, chainId);
        ByteBuffer buffer = ByteBuffer.allocate(transactionBase.length + 20 + 8);
        buffer.put(transactionBase);
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
        byte[] transactionBase = getTransactionBase( 9, nonce, chainId);

        return transactionBase;
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

        byte[] transactionBase = getTransactionBase( 10, nonce, chainId);
        ByteBuffer buffer = ByteBuffer.allocate(transactionBase.length + (transactions.size() * 4) + totalLength);
        buffer.put(transactionBase);

        for (byte[] Transaction : transactions) {
            buffer.putInt(Transaction.length);
            buffer.put(Transaction);
        }

        return buffer.array();
    }

    /**
     * Returns the transaction of sending payable data to a specified VM on the PWR network.
     *
     * @param vidaId    The ID of the VM.
     * @param value   The amount of PWR tokens to be sent.
     * @param data    The data to be sent.
     * @param nonce   The transaction nonce.
     * @param chainId The chain ID.
     * @return A byte array that represents the transaction of this method.
     * @throws RuntimeException For various transaction-related validation issues.
     */
    public static byte[] getPayableVmDataTransaction(long vidaId, long value, byte[] data, int nonce, byte chainId) {
        if (nonce < 0) {
            throw new RuntimeException("Nonce cannot be negative");
        }

        byte[] transactionBase = getTransactionBase( 11, nonce, chainId);
        ByteBuffer buffer = ByteBuffer.allocate(transactionBase.length + 16 + 4 + data.length);
        buffer.put(transactionBase);
        buffer.putLong(vidaId);
        buffer.putInt(data.length);
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

        byte[] transactionBase = getTransactionBase( 7, nonce, chainId);
        ByteBuffer buffer = ByteBuffer.allocate(transactionBase.length + 20);
        buffer.put(transactionBase);
        buffer.put(Hex.decode(validator));

        return buffer.array();
    }

    /**
     * Returns the transaction for approving a set of transactions for a specific VM.
     *
     * @param vidaId         The ID of the VM.
     * @param transactions The transactions to be approved.
     * @param nonce        The transaction nonce.
     * @param chainId      The chain ID.
     * @return A byte array representing the transaction of this method.
     * @throws RuntimeException If the nonce is negative or there are no transactions to approve.
     */
    public static byte[] getConduitApprovalTransaction(long vidaId, List<byte[]> transactions, int nonce, byte chainId) {
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

        byte[] transactionBase = getTransactionBase( 12, nonce, chainId);
        ByteBuffer buffer = ByteBuffer.allocate(transactionBase.length + 8 + (transactions.size() * 4) + totalTransactionsLength);
        buffer.put(transactionBase);
        buffer.putLong(vidaId);

        for (byte[] Transaction : transactions) {
            buffer.putInt(Transaction.length);
            buffer.put(Transaction);
        }

        return buffer.array();
    }

    /**
     * Returns the transaction for setting a list of conduits for a specific VM.
     *
     * @param vidaId     The ID of the VM.
     * @param conduits The list of conduits to be set.
     * @param nonce    The transaction nonce.
     * @param chainId  The chain ID.
     * @return A byte array representing the transaction of this method.
     * @throws RuntimeException If the nonce is negative or there are no conduits to set.
     */
    public static byte[] getSetConduitsTransaction(long vidaId, List<byte[]> conduits, int nonce, byte chainId) {
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

        byte[] transactionBase = getTransactionBase( 13, nonce, chainId);
        ByteBuffer buffer = ByteBuffer.allocate(transactionBase.length + 8 + (conduits.size() * 4) + totalConduitLength);
        buffer.put(transactionBase);
        buffer.putLong(vidaId);

        for (byte[] conduit : conduits) {
            buffer.putInt(conduit.length);
            buffer.put(conduit);
        }

        return buffer.array();
    }

    /**
     * Returns the transaction for adding a list of conduits to a specific VM.
     *
     * @param vidaId     The ID of the VM.
     * @param conduits The list of conduits to be added.
     * @param nonce    The transaction nonce.
     * @param chainId  The chain ID.
     * @return A byte array representing the transaction of this method.
     * @throws RuntimeException If the nonce is negative or there are no conduits to add.
     */
    public static byte[] getAddConduitsTransaction(long vidaId, List<byte[]> conduits, int nonce, byte chainId) {
        if (nonce < 0) {
            throw new RuntimeException("Nonce cannot be negative");
        }
        if (conduits.size() == 0) {
            throw new RuntimeException("No conduits provided");
        }

        byte[] transactionBase = getTransactionBase( 14, nonce, chainId);
        ByteBuffer buffer = ByteBuffer.allocate(transactionBase.length + 8 + (conduits.size() * 20));
        buffer.put(transactionBase);
        buffer.putLong(vidaId);

        for (byte[] conduit : conduits) {
            buffer.put(conduit);
        }

        return buffer.array();
    }

    /**
     * Returns the transaction for removing a list of conduits from a specific VM.
     *
     * @param vidaId     The ID of the VM.
     * @param conduits The list of conduits to be removed.
     * @param nonce    The transaction nonce.
     * @param chainId  The chain ID.
     * @return A byte array representing the transaction of this method.
     * @throws RuntimeException If the nonce is negative or there are no conduits to remove.
     */
    public static byte[] getRemoveConduitsTransaction(long vidaId, List<byte[]> conduits, int nonce, byte chainId) {
        if (nonce < 0) {
            throw new RuntimeException("Nonce cannot be negative");
        }
        if (conduits.size() == 0) {
            throw new RuntimeException("No conduits provided");
        }

        byte[] transactionBase = getTransactionBase( 15, nonce, chainId);
        ByteBuffer buffer = ByteBuffer.allocate(transactionBase.length + 8 + (conduits.size() * 20));
        buffer.put(transactionBase);
        buffer.putLong(vidaId);

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

        byte[] transactionBase = getTransactionBase( 16, nonce, chainId);
        ByteBuffer buffer = ByteBuffer.allocate(transactionBase.length + 48);
        buffer.put(transactionBase);
        buffer.putLong(sharesAmount);
        buffer.put(Hex.decode(fromValidator));
        buffer.put(Hex.decode(toValidator));

        return buffer.array();
    }


    public static byte[] getChangeEarlyWithdrawPenaltyProposalTxn(long withdrawalPenaltyTime, int withdrawalPenalty, String title, String description, int nonce, byte chainId) {
        byte[] transactionBase = getTransactionBase( 17, nonce, chainId);
        byte[] descriptionBytea = description.getBytes(StandardCharsets.UTF_8);
        byte[] titleBytea = title.getBytes(StandardCharsets.UTF_8);

        ByteBuffer buffer = ByteBuffer.allocate(transactionBase.length + 12 + 4 + titleBytea.length + descriptionBytea.length);
        buffer.put(transactionBase);
        buffer.putInt(titleBytea.length);
        buffer.put(titleBytea);
        buffer.putLong(withdrawalPenaltyTime);
        buffer.putInt(withdrawalPenalty);
        buffer.put(descriptionBytea);
        return buffer.array();
    }

    public static byte[] getChangeFeePerByteProposalTxn(long feePerByte, String title, String description, int nonce, byte chainId) {
        byte[] transactionBase = getTransactionBase( 18, nonce, chainId);
        byte[] descriptionBytea = description.getBytes(StandardCharsets.UTF_8);
        byte[] titleBytea = title.getBytes(StandardCharsets.UTF_8);

        ByteBuffer buffer = ByteBuffer.allocate(transactionBase.length + 8 + 4 + titleBytea.length + descriptionBytea.length);
        buffer.put(transactionBase);
        buffer.putInt(titleBytea.length);
        buffer.put(titleBytea);
        buffer.putLong(feePerByte);
        buffer.put(descriptionBytea);

        return buffer.array();
    }

    public static byte[] getChangeMaxBlockSizeProposalTxn(int maxBlockSize, String title, String description, int nonce, byte chainId) {
        byte[] transactionBase = getTransactionBase( 19, nonce, chainId);
        byte[] descriptionBytea = description.getBytes(StandardCharsets.UTF_8);
        byte[] titleBytea = title.getBytes(StandardCharsets.UTF_8);

        ByteBuffer buffer = ByteBuffer.allocate(transactionBase.length + 4 + 4 + titleBytea.length + descriptionBytea.length);
        buffer.put(transactionBase);
        buffer.putInt(titleBytea.length);
        buffer.put(titleBytea);
        buffer.putInt(maxBlockSize);
        buffer.put(descriptionBytea);

        return buffer.array();
    }

    public static byte[] getChangeMaxTxnSizeProposalTxn(int maxTxnSize, String title, String description, int nonce, byte chainId) {
        byte[] transactionBase = getTransactionBase( 20, nonce, chainId);
        byte[] descriptionBytea = description.getBytes(StandardCharsets.UTF_8);
        byte[] titleBytea = title.getBytes(StandardCharsets.UTF_8);

        ByteBuffer buffer = ByteBuffer.allocate(transactionBase.length + 4 + 4 + titleBytea.length + descriptionBytea.length);
        buffer.put(transactionBase);
        buffer.putInt(titleBytea.length);
        buffer.put(titleBytea);
        buffer.putInt(maxTxnSize);
        buffer.put(descriptionBytea);

        return buffer.array();
    }

    public static byte[] getChangeOverallBurnPercentageProposalTxn(int burnPercentage, String title, String description, int nonce, byte chainId) {
        byte[] transactionBase = getTransactionBase( 21, nonce, chainId);
        byte[] descriptionBytea = description.getBytes(StandardCharsets.UTF_8);
        byte[] titleBytea = title.getBytes(StandardCharsets.UTF_8);

        ByteBuffer buffer = ByteBuffer.allocate(transactionBase.length + 4 + 4 + titleBytea.length + descriptionBytea.length);
        buffer.put(transactionBase);
        buffer.putInt(titleBytea.length);
        buffer.put(titleBytea);
        buffer.putInt(burnPercentage);
        buffer.put(descriptionBytea);

        return buffer.array();
    }

    public static byte[] getChangeRewardPerYearProposalTxn(long rewardPerYear, String title, String description, int nonce, byte chainId) {
        byte[] transactionBase = getTransactionBase( 22, nonce, chainId);
        byte[] descriptionBytea = description.getBytes(StandardCharsets.UTF_8);
        byte[] titleBytea = title.getBytes(StandardCharsets.UTF_8);

        ByteBuffer buffer = ByteBuffer.allocate(transactionBase.length + 8 + 4 + titleBytea.length + descriptionBytea.length);
        buffer.put(transactionBase);
        buffer.putInt(titleBytea.length);
        buffer.put(titleBytea);
        buffer.putLong(rewardPerYear);
        buffer.put(description.getBytes(StandardCharsets.UTF_8));

        return buffer.array();
    }

    public static byte[] getChangeValidatorCountLimitProposalTxn(int validatorCountLimit, String title, String description, int nonce, byte chainId) {
        byte[] transactionBase = getTransactionBase( 23, nonce, chainId);
        byte[] descriptionBytea = description.getBytes(StandardCharsets.UTF_8);
        byte[] titleBytea = title.getBytes(StandardCharsets.UTF_8);

        ByteBuffer buffer = ByteBuffer.allocate(transactionBase.length + 4 + 4 + titleBytea.length + descriptionBytea.length);
        buffer.put(transactionBase);
        buffer.putInt(titleBytea.length);
        buffer.put(titleBytea);
        buffer.putInt(validatorCountLimit);
        buffer.put(descriptionBytea);

        return buffer.array();
    }

    public static byte[] getChangeValidatorJoiningFeeProposalTxn(long joiningFee, String title, String description, int nonce, byte chainId) {
        byte[] transactionBase = getTransactionBase( 24, nonce, chainId);
        byte[] descriptionBytea = description.getBytes(StandardCharsets.UTF_8);
        byte[] titleBytea = title.getBytes(StandardCharsets.UTF_8);

        ByteBuffer buffer = ByteBuffer.allocate(transactionBase.length + 8 + 4 + titleBytea.length + descriptionBytea.length);
        buffer.put(transactionBase);
        buffer.putInt(titleBytea.length);
        buffer.put(titleBytea);
        buffer.putLong(joiningFee);
        buffer.put(descriptionBytea);

        return buffer.array();
    }

    public static byte[] getChangeVmIdClaimingFeeProposalTxn(long claimingFee, String title, String description, int nonce, byte chainId) {
        byte[] transactionBase = getTransactionBase( 25, nonce, chainId);
        byte[] descriptionBytea = description.getBytes(StandardCharsets.UTF_8);
        byte[] titleBytea = title.getBytes(StandardCharsets.UTF_8);

        ByteBuffer buffer = ByteBuffer.allocate(transactionBase.length + 8 + 4 + titleBytea.length + descriptionBytea.length);
        buffer.put(transactionBase);
        buffer.putInt(titleBytea.length);
        buffer.put(titleBytea);
        buffer.putLong(claimingFee);
        buffer.put(descriptionBytea);

        return buffer.array();
    }

    public static byte[] getChangeVmOwnerTxnFeeShareProposalTxn(int feeShare, String title, String description, int nonce, byte chainId) {
        byte[] transactionBase = getTransactionBase( 26, nonce, chainId);
        byte[] descriptionBytea = description.getBytes(StandardCharsets.UTF_8);
        byte[] titleBytea = title.getBytes(StandardCharsets.UTF_8);

        ByteBuffer buffer = ByteBuffer.allocate(transactionBase.length + 4 + 4 + titleBytea.length + descriptionBytea.length);
        buffer.put(transactionBase);
        buffer.putInt(titleBytea.length);
        buffer.put(titleBytea);
        buffer.putInt(feeShare);
        buffer.put(descriptionBytea);

        return buffer.array();
    }

    public static byte[] getOtherProposalTxn(String title, String description, int nonce, byte chainId) {
        byte[] transactionBase = getTransactionBase( 27, nonce, chainId);
        byte[] descriptionBytea = description.getBytes(StandardCharsets.UTF_8);
        byte[] titleBytea = title.getBytes(StandardCharsets.UTF_8);

        ByteBuffer buffer = ByteBuffer.allocate(transactionBase.length + 4 + titleBytea.length + descriptionBytea.length);
        buffer.put(transactionBase);
        buffer.putInt(titleBytea.length);
        buffer.put(titleBytea);
        buffer.put(descriptionBytea);

        return buffer.array();
    }

    public static byte[] getVoteOnProposalTxn(String proposalHash, byte vote, int nonce, byte chainId) {
        byte[] transactionBase = getTransactionBase( 28, nonce, chainId);
        byte[] proposalHashBytes = Hex.decode(proposalHash);
        errorIf(proposalHashBytes.length != 32, "Invalid proposal hash");

        ByteBuffer buffer = ByteBuffer.allocate(39);
        buffer.put(transactionBase);
        buffer.put(proposalHashBytes);
        buffer.put(vote);

        return buffer.array();
    }

    public static byte[] getChangeIpTxn(String newIp, int nonce, byte chainId) {
        errorIf(newIp == null || newIp.isEmpty() || newIp.length() < 7 || newIp.length() > 15, "Invalid IP address");

        byte[] transactionBase = getTransactionBase( 29, nonce, chainId);
        ByteBuffer buffer = ByteBuffer.allocate(transactionBase.length + newIp.length());
        buffer.put(transactionBase);
        buffer.put(newIp.getBytes(StandardCharsets.UTF_8));

        return buffer.array();
    }

    //endregion

    //region - Falcon Transactions
    public static byte[] getSetPublicKeyTransaction(long feePerByte, byte[] publicKey, byte[] sender, int nonce, byte chainId) {
        byte[] transactionBase = getFalconTransactionBase(1001, nonce, chainId, feePerByte, sender);
        ByteBuffer buffer = ByteBuffer.allocate(transactionBase.length + 2 + publicKey.length);
        buffer.put(transactionBase);
        buffer.putShort((short) publicKey.length);
        buffer.put(publicKey);

        return buffer.array();
    }

    public static byte[] getFalconJoinAsValidatorTransaction(long feePerByte, byte[] sender, String ip, int nonce, byte chainId) {
        byte[] transactionBase = getFalconTransactionBase(1002, nonce, chainId, feePerByte, sender);
        byte[] ipBytes = ip.getBytes(StandardCharsets.UTF_8);

        ByteBuffer buffer = ByteBuffer.allocate(transactionBase.length + 2 + ipBytes.length);
        buffer.put(transactionBase);
        buffer.putShort((short) ipBytes.length);
        buffer.put(ipBytes);

        return buffer.array();
    }

    public static byte[] getFalconDelegateTransaction(long feePerByte, byte[] sender, byte[] validator, long pwrAmount, int nonce, byte chainId) {
        byte[] transactionBase = getFalconTransactionBase(1003, nonce, chainId, feePerByte, sender);
        ByteBuffer buffer = ByteBuffer.allocate(transactionBase.length + 20 + 8);
        buffer.put(transactionBase);
        buffer.put(validator);
        buffer.putLong(pwrAmount);

        return buffer.array();
    }

    public static byte[] getFalconChangeIpTransaction(long feePerByte, byte[] sender, String newIp, int nonce, byte chainId) {
        byte[] transactionBase = getFalconTransactionBase(1004, nonce, chainId, feePerByte, sender);
        byte[] ipBytes = newIp.getBytes(StandardCharsets.UTF_8);

        ByteBuffer buffer = ByteBuffer.allocate(transactionBase.length + 2 + ipBytes.length);
        buffer.put(transactionBase);
        buffer.putShort((short) ipBytes.length);
        buffer.put(ipBytes);

        return buffer.array();
    }

    public static byte[] getFalconClaimActiveNodeSpotTransaction(long feePerByte, byte[] sender, int nonce, byte chainId) {
        byte[] transactionBase = getFalconTransactionBase(1005, nonce, chainId, feePerByte, sender);
        return transactionBase;
    }

    public static byte[] getFalconTransferTransaction(long feePerByte, byte[] sender, byte[] receiver, long amount, int nonce, byte chainId) {
        byte[] transactionBase = getFalconTransactionBase(1006, nonce, chainId, feePerByte, sender);
        ByteBuffer buffer = ByteBuffer.allocate(transactionBase.length + 20 + 8);
        buffer.put(transactionBase);
        buffer.put(receiver);
        buffer.putLong(amount);

        return buffer.array();
    }

    public static byte[] getFalconVmDataTransaction(long feePerByte, byte[] sender, long vidaId, byte[] data, int nonce, byte chainId) {
        byte[] transactionBase = getFalconTransactionBase(1007, nonce, chainId, feePerByte, sender);
        ByteBuffer buffer = ByteBuffer.allocate(transactionBase.length + 8 + 4 + data.length);
        buffer.put(transactionBase);
        buffer.putLong(vidaId);
        buffer.putInt(data.length);
        buffer.put(data);

        return buffer.array();
    }
    //endregion
}