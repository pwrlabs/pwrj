package com.github.pwrlabs.pwrj.protocol;

import io.pwrlabs.util.encoders.ByteArrayWrapper;

import java.math.BigInteger;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static com.github.pwrlabs.pwrj.Utils.NewError.errorIf;

public class TransactionBuilder {

    private static byte[] getFalconTransactionBase(int identifier, int nonce, byte chainId, long feePerByte, byte[] sender) {
        ByteBuffer buffer = ByteBuffer.allocate(37);
        buffer.putInt(identifier);
        buffer.put(chainId);
        buffer.putInt(nonce);
        buffer.putLong(feePerByte);
        buffer.put(sender);
        return buffer.array();
    }

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

    public static byte[] getTransferTransaction(long feePerByte, byte[] sender, byte[] receiver, long amount, int nonce, byte chainId) {
        byte[] transactionBase = getFalconTransactionBase(1006, nonce, chainId, feePerByte, sender);
        ByteBuffer buffer = ByteBuffer.allocate(transactionBase.length + 20 + 8);
        buffer.put(transactionBase);
        buffer.put(receiver);
        buffer.putLong(amount);

        return buffer.array();
    }

    // Governance Proposal Transactions

    public static byte[] getChangeEarlyWithdrawPenaltyProposalTransaction(long feePerByte, byte[] sender, String title,
                                                                          String description, long earlyWithdrawalTime,
                                                                          int withdrawalPenalty, int nonce, byte chainId) {
        byte[] transactionBase = getFalconTransactionBase(1009, nonce, chainId, feePerByte, sender);
        byte[] titleBytes = title.getBytes(StandardCharsets.UTF_8);
        byte[] descriptionBytes = description.getBytes(StandardCharsets.UTF_8);

        ByteBuffer buffer = ByteBuffer.allocate(transactionBase.length + 4 + titleBytes.length + 8 + 4 + descriptionBytes.length);
        buffer.put(transactionBase);
        buffer.putInt(titleBytes.length);
        buffer.put(titleBytes);
        buffer.putLong(earlyWithdrawalTime);
        buffer.putInt(withdrawalPenalty);
        buffer.put(descriptionBytes);

        return buffer.array();
    }

    public static byte[] getChangeFeePerByteProposalTransaction(long feePerByte, byte[] sender, String title,
                                                                String description, long newFeePerByte,
                                                                int nonce, byte chainId) {
        byte[] transactionBase = getFalconTransactionBase(1010, nonce, chainId, feePerByte, sender);
        byte[] titleBytes = title.getBytes(StandardCharsets.UTF_8);
        byte[] descriptionBytes = description.getBytes(StandardCharsets.UTF_8);

        ByteBuffer buffer = ByteBuffer.allocate(transactionBase.length + 4 + titleBytes.length + 8 + descriptionBytes.length);
        buffer.put(transactionBase);
        buffer.putInt(titleBytes.length);
        buffer.put(titleBytes);
        buffer.putLong(newFeePerByte);
        buffer.put(descriptionBytes);

        return buffer.array();
    }

    public static byte[] getChangeMaxBlockSizeProposalTransaction(long feePerByte, byte[] sender, String title,
                                                                  String description, int maxBlockSize,
                                                                  int nonce, byte chainId) {
        byte[] transactionBase = getFalconTransactionBase(1011, nonce, chainId, feePerByte, sender);
        byte[] titleBytes = title.getBytes(StandardCharsets.UTF_8);
        byte[] descriptionBytes = description.getBytes(StandardCharsets.UTF_8);

        ByteBuffer buffer = ByteBuffer.allocate(transactionBase.length + 4 + titleBytes.length + 4 + descriptionBytes.length);
        buffer.put(transactionBase);
        buffer.putInt(titleBytes.length);
        buffer.put(titleBytes);
        buffer.putInt(maxBlockSize);
        buffer.put(descriptionBytes);

        return buffer.array();
    }

    public static byte[] getChangeMaxTxnSizeProposalTransaction(long feePerByte, byte[] sender, String title,
                                                                String description, int maxTxnSize,
                                                                int nonce, byte chainId) {
        byte[] transactionBase = getFalconTransactionBase(1012, nonce, chainId, feePerByte, sender);
        byte[] titleBytes = title.getBytes(StandardCharsets.UTF_8);
        byte[] descriptionBytes = description.getBytes(StandardCharsets.UTF_8);

        ByteBuffer buffer = ByteBuffer.allocate(transactionBase.length + 4 + titleBytes.length + 4 + descriptionBytes.length);
        buffer.put(transactionBase);
        buffer.putInt(titleBytes.length);
        buffer.put(titleBytes);
        buffer.putInt(maxTxnSize);
        buffer.put(descriptionBytes);

        return buffer.array();
    }

    public static byte[] getChangeOverallBurnPercentageProposalTransaction(long feePerByte, byte[] sender, String title,
                                                                           String description, int burnPercentage,
                                                                           int nonce, byte chainId) {
        byte[] transactionBase = getFalconTransactionBase(1013, nonce, chainId, feePerByte, sender);
        byte[] titleBytes = title.getBytes(StandardCharsets.UTF_8);
        byte[] descriptionBytes = description.getBytes(StandardCharsets.UTF_8);

        ByteBuffer buffer = ByteBuffer.allocate(transactionBase.length + 4 + titleBytes.length + 4 + descriptionBytes.length);
        buffer.put(transactionBase);
        buffer.putInt(titleBytes.length);
        buffer.put(titleBytes);
        buffer.putInt(burnPercentage);
        buffer.put(descriptionBytes);

        return buffer.array();
    }

    public static byte[] getChangeRewardPerYearProposalTransaction(long feePerByte, byte[] sender, String title,
                                                                   String description, long rewardPerYear,
                                                                   int nonce, byte chainId) {
        byte[] transactionBase = getFalconTransactionBase(1014, nonce, chainId, feePerByte, sender);
        byte[] titleBytes = title.getBytes(StandardCharsets.UTF_8);
        byte[] descriptionBytes = description.getBytes(StandardCharsets.UTF_8);

        ByteBuffer buffer = ByteBuffer.allocate(transactionBase.length + 4 + titleBytes.length + 8 + descriptionBytes.length);
        buffer.put(transactionBase);
        buffer.putInt(titleBytes.length);
        buffer.put(titleBytes);
        buffer.putLong(rewardPerYear);
        buffer.put(descriptionBytes);

        return buffer.array();
    }

    public static byte[] getChangeValidatorCountLimitProposalTransaction(long feePerByte, byte[] sender, String title,
                                                                         String description, int validatorCountLimit,
                                                                         int nonce, byte chainId) {
        byte[] transactionBase = getFalconTransactionBase(1015, nonce, chainId, feePerByte, sender);
        byte[] titleBytes = title.getBytes(StandardCharsets.UTF_8);
        byte[] descriptionBytes = description.getBytes(StandardCharsets.UTF_8);

        ByteBuffer buffer = ByteBuffer.allocate(transactionBase.length + 4 + titleBytes.length + 4 + descriptionBytes.length);
        buffer.put(transactionBase);
        buffer.putInt(titleBytes.length);
        buffer.put(titleBytes);
        buffer.putInt(validatorCountLimit);
        buffer.put(descriptionBytes);

        return buffer.array();
    }

    public static byte[] getChangeValidatorJoiningFeeProposalTransaction(long feePerByte, byte[] sender, String title,
                                                                         String description, long joiningFee,
                                                                         int nonce, byte chainId) {
        byte[] transactionBase = getFalconTransactionBase(1016, nonce, chainId, feePerByte, sender);
        byte[] titleBytes = title.getBytes(StandardCharsets.UTF_8);
        byte[] descriptionBytes = description.getBytes(StandardCharsets.UTF_8);

        ByteBuffer buffer = ByteBuffer.allocate(transactionBase.length + 4 + titleBytes.length + 8 + descriptionBytes.length);
        buffer.put(transactionBase);
        buffer.putInt(titleBytes.length);
        buffer.put(titleBytes);
        buffer.putLong(joiningFee);
        buffer.put(descriptionBytes);

        return buffer.array();
    }

    public static byte[] getChangeVidaIdClaimingFeeProposalTransaction(long feePerByte, byte[] sender, String title,
                                                                       String description, long vidaIdClaimingFee,
                                                                       int nonce, byte chainId) {
        byte[] transactionBase = getFalconTransactionBase(1017, nonce, chainId, feePerByte, sender);
        byte[] titleBytes = title.getBytes(StandardCharsets.UTF_8);
        byte[] descriptionBytes = description.getBytes(StandardCharsets.UTF_8);

        ByteBuffer buffer = ByteBuffer.allocate(transactionBase.length + 4 + titleBytes.length + 8 + descriptionBytes.length);
        buffer.put(transactionBase);
        buffer.putInt(titleBytes.length);
        buffer.put(titleBytes);
        buffer.putLong(vidaIdClaimingFee);
        buffer.put(descriptionBytes);

        return buffer.array();
    }

    public static byte[] getChangeVmOwnerTxnFeeShareProposalTransaction(long feePerByte, byte[] sender, String title,
                                                                        String description, int vmOwnerTxnFeeShare,
                                                                        int nonce, byte chainId) {
        byte[] transactionBase = getFalconTransactionBase(1018, nonce, chainId, feePerByte, sender);
        byte[] titleBytes = title.getBytes(StandardCharsets.UTF_8);
        byte[] descriptionBytes = description.getBytes(StandardCharsets.UTF_8);

        ByteBuffer buffer = ByteBuffer.allocate(transactionBase.length + 4 + titleBytes.length + 4 + descriptionBytes.length);
        buffer.put(transactionBase);
        buffer.putInt(titleBytes.length);
        buffer.put(titleBytes);
        buffer.putInt(vmOwnerTxnFeeShare);
        buffer.put(descriptionBytes);

        return buffer.array();
    }

    public static byte[] getOtherProposalTransaction(long feePerByte, byte[] sender, String title,
                                                     String description, int nonce, byte chainId) {
        byte[] transactionBase = getFalconTransactionBase(1019, nonce, chainId, feePerByte, sender);
        byte[] titleBytes = title.getBytes(StandardCharsets.UTF_8);
        byte[] descriptionBytes = description.getBytes(StandardCharsets.UTF_8);

        ByteBuffer buffer = ByteBuffer.allocate(transactionBase.length + 4 + titleBytes.length + descriptionBytes.length);
        buffer.put(transactionBase);
        buffer.putInt(titleBytes.length);
        buffer.put(titleBytes);
        buffer.put(descriptionBytes);

        return buffer.array();
    }

    public static byte[] getVoteOnProposalTransaction(long feePerByte, byte[] sender, byte[] proposalHash,
                                                      byte vote, int nonce, byte chainId) {
        byte[] transactionBase = getFalconTransactionBase(1020, nonce, chainId, feePerByte, sender);

        ByteBuffer buffer = ByteBuffer.allocate(transactionBase.length + 32 + 1);
        buffer.put(transactionBase);
        buffer.put(proposalHash);
        buffer.put(vote);

        return buffer.array();
    }

    // Guardian Transactions

    public static byte[] getGuardianApprovalTransaction(long feePerByte, byte[] sender, List<byte[]> wrappedTxns,
                                                        int nonce, byte chainId) {
        byte[] transactionBase = getFalconTransactionBase(1021, nonce, chainId, feePerByte, sender);

        // Calculate size needed for all wrapped transactions
        int totalWrappedSize = 0;
        for (byte[] wrappedTxn : wrappedTxns) {
            totalWrappedSize += 4 + wrappedTxn.length; // 4 bytes for length + txn size
        }

        ByteBuffer buffer = ByteBuffer.allocate(4 + transactionBase.length + totalWrappedSize);
        buffer.put(transactionBase);

        buffer.putInt(wrappedTxns.size());

        // Add each wrapped transaction with its length prefix
        for (byte[] wrappedTxn : wrappedTxns) {
            buffer.putInt(wrappedTxn.length);
            buffer.put(wrappedTxn);
        }

        return buffer.array();
    }

    public static byte[] getRemoveGuardianTransaction(long feePerByte, byte[] sender, int nonce, byte chainId) {
        byte[] transactionBase = getFalconTransactionBase(1022, nonce, chainId, feePerByte, sender);
        return transactionBase;
    }

    public static byte[] getSetGuardianTransaction(long feePerByte, byte[] sender, long expiryDate,
                                                   byte[] guardianAddress, int nonce, byte chainId) {
        byte[] transactionBase = getFalconTransactionBase(1023, nonce, chainId, feePerByte, sender);

        ByteBuffer buffer = ByteBuffer.allocate(transactionBase.length + 8 + 20);
        buffer.put(transactionBase);
        buffer.putLong(expiryDate);
        buffer.put(guardianAddress);

        return buffer.array();
    }

    // Staking Transactions

    public static byte[] getMoveStakeTxnTransaction(long feePerByte, byte[] sender, BigInteger sharesAmount,
                                                    byte[] fromValidator, byte[] toValidator,
                                                    int nonce, byte chainId) {
        byte[] transactionBase = getFalconTransactionBase(1024, nonce, chainId, feePerByte, sender);
        byte[] sharesAmountBytes = sharesAmount.toByteArray();

        ByteBuffer buffer = ByteBuffer.allocate(transactionBase.length + 2 + sharesAmountBytes.length + 20 + 20);
        buffer.put(transactionBase);
        buffer.putShort((short) sharesAmountBytes.length);
        buffer.put(sharesAmountBytes);
        buffer.put(fromValidator);
        buffer.put(toValidator);

        return buffer.array();
    }

    public static byte[] getRemoveValidatorTransaction(long feePerByte, byte[] sender, byte[] validatorAddress,
                                                       int nonce, byte chainId) {
        byte[] transactionBase = getFalconTransactionBase(1025, nonce, chainId, feePerByte, sender);

        ByteBuffer buffer = ByteBuffer.allocate(transactionBase.length + 20);
        buffer.put(transactionBase);
        buffer.put(validatorAddress);

        return buffer.array();
    }

    public static byte[] getWithdrawTransaction(long feePerByte, byte[] sender, BigInteger sharesAmount,
                                                byte[] validator, int nonce, byte chainId) {
        byte[] transactionBase = getFalconTransactionBase(1026, nonce, chainId, feePerByte, sender);
        byte[] sharesAmountBytes = sharesAmount.toByteArray();

        ByteBuffer buffer = ByteBuffer.allocate(transactionBase.length + 2 + sharesAmountBytes.length + 20);
        buffer.put(transactionBase);
        buffer.putShort((short) sharesAmountBytes.length);
        buffer.put(sharesAmountBytes);
        buffer.put(validator);

        return buffer.array();
    }

    // VIDA Transactions

    public static byte[] getClaimVidaIdTransaction(long feePerByte, byte[] sender, long vidaId,
                                                   int nonce, byte chainId) {
        byte[] transactionBase = getFalconTransactionBase(1028, nonce, chainId, feePerByte, sender);

        ByteBuffer buffer = ByteBuffer.allocate(transactionBase.length + 8);
        buffer.put(transactionBase);
        buffer.putLong(vidaId);

        return buffer.array();
    }

    public static byte[] getConduitApprovalTransaction(long feePerByte, byte[] sender, long vidaId,
                                                       List<byte[]> wrappedTxns, int nonce, byte chainId) {
        byte[] transactionBase = getFalconTransactionBase(1029, nonce, chainId, feePerByte, sender);

        // Calculate size needed for vidaId (8) and all wrapped transactions
        int totalWrappedSize = 8; // 8 bytes for vidaId
        for (byte[] wrappedTxn : wrappedTxns) {
            totalWrappedSize += 4 + wrappedTxn.length; // 4 bytes for length + txn size
        }

        ByteBuffer buffer = ByteBuffer.allocate(transactionBase.length + 4 + totalWrappedSize);
        buffer.put(transactionBase);
        buffer.putLong(vidaId);
        buffer.putInt(wrappedTxns.size());

        // Add each wrapped transaction with its length prefix
        for (byte[] wrappedTxn : wrappedTxns) {
            buffer.putInt(wrappedTxn.length);
            buffer.put(wrappedTxn);
        }

        return buffer.array();
    }

    public static byte[] getPayableVidaDataTransaction(long feePerByte, byte[] sender, long vidaId,
                                                       byte[] data, long value, int nonce, byte chainId) {
        byte[] transactionBase = getFalconTransactionBase(1030, nonce, chainId, feePerByte, sender);

        ByteBuffer buffer = ByteBuffer.allocate(transactionBase.length + 8 + 4 + data.length + 8);
        buffer.put(transactionBase);
        buffer.putLong(vidaId);
        buffer.putInt(data.length);
        buffer.put(data);
        buffer.putLong(value);

        return buffer.array();
    }

    public static byte[] getRemoveConduitsTransaction(long feePerByte, byte[] sender, long vidaId,
                                                      List<byte[]> conduits, int nonce, byte chainId) {
        byte[] transactionBase = getFalconTransactionBase(1031, nonce, chainId, feePerByte, sender);

        // Calculate total size needed: base + vidaId(8) + (conduits * 20)
        int totalSize = transactionBase.length + 8 + (conduits.size() * 20);

        ByteBuffer buffer = ByteBuffer.allocate(totalSize);
        buffer.put(transactionBase);
        buffer.putLong(vidaId);

        // Add each conduit address
        for (byte[] conduit : conduits) {
            buffer.put(conduit);
        }

        return buffer.array();
    }

    public static byte[] getSetConduitModeTransaction(long feePerByte, byte[] sender, long vidaId, byte mode,
                                                      int conduitThreshold, Set<byte[]> conduits, Map<ByteArrayWrapper, Long> conduitsWithVotingPower,
                                                      int nonce, byte chainId) {
        errorIf(conduits != null && conduitsWithVotingPower != null, "Conduits and conduitsWithVotingPower cannot both be sent in the same txn");

        byte[] transactionBase = getFalconTransactionBase(1033, nonce, chainId, feePerByte, sender);

        // Calculate size: base + vidaId(8) + mode(1) + threshold(4) + (conduits * 20)
        int totalSize = transactionBase.length + 8 + 1 + 4 + 4;
        totalSize += (conduits != null ? conduits.size() * 20 : 0);
        totalSize += (conduitsWithVotingPower != null ? conduitsWithVotingPower.size() * 28 : 0); // 20 bytes address + 8 bytes voting power

        ByteBuffer buffer = ByteBuffer.allocate(totalSize);
        buffer.put(transactionBase);
        buffer.putLong(vidaId);
        buffer.put(mode);
        buffer.putInt(conduitThreshold);

        // Add conduit addresses if provided
        if (conduits != null) {
            buffer.putInt(conduits.size());
            for (byte[] conduit : conduits) {
                buffer.put(conduit);
            }
        }

        if(conduitsWithVotingPower != null && !conduitsWithVotingPower.isEmpty()) {
            buffer.putInt(conduitsWithVotingPower.size());
            for (Map.Entry<ByteArrayWrapper, Long> entry : conduitsWithVotingPower.entrySet()) {
                buffer.put(entry.getKey().data());
                buffer.putLong(entry.getValue());
            }
        }

        if((conduits == null || conduits.isEmpty()) && (conduitsWithVotingPower == null || conduitsWithVotingPower.isEmpty())) {
            buffer.putInt(0);
        }

        return buffer.array();
    }

    public static byte[] getSetConduitModeWithVidaBasedTransaction(long feePerByte, byte[] sender, long vidaId, byte mode,
                                                                   int conduitThreshold, List<byte[]> conduits,
                                                                   List<Long> stakingPowers, int nonce, byte chainId) {
        byte[] transactionBase = getFalconTransactionBase(1033, nonce, chainId, feePerByte, sender);

        // Calculate size: base + vidaId(8) + mode(1) + threshold(4) + (conduits * (20+8))
        int totalSize = transactionBase.length + 8 + 1 + 4;
        totalSize += (conduits != null ? conduits.size() * 28 : 0); // 20 bytes address + 8 bytes staking power

        ByteBuffer buffer = ByteBuffer.allocate(totalSize);
        buffer.put(transactionBase);
        buffer.putLong(vidaId);
        buffer.put(mode);
        buffer.putInt(conduitThreshold);

        // Add conduit addresses and staking powers if provided
        if (conduits != null && stakingPowers != null && conduits.size() == stakingPowers.size()) {
            for (int i = 0; i < conduits.size(); i++) {
                buffer.put(conduits.get(i));
                buffer.putLong(stakingPowers.get(i));
            }
        }

        return buffer.array();
    }

    public static byte[] getSetVidaPrivateStateTransaction(long feePerByte, byte[] sender, long vidaId,
                                                           boolean privateState, int nonce, byte chainId) {
        byte[] transactionBase = getFalconTransactionBase(1034, nonce, chainId, feePerByte, sender);

        ByteBuffer buffer = ByteBuffer.allocate(transactionBase.length + 8 + 1);
        buffer.put(transactionBase);
        buffer.putLong(vidaId);
        buffer.put((byte) (privateState ? 1 : 0));

        return buffer.array();
    }

    public static byte[] getSetVidaToAbsolutePublicTransaction(long feePerByte, byte[] sender, long vidaId,
                                                               int nonce, byte chainId) {
        byte[] transactionBase = getFalconTransactionBase(1035, nonce, chainId, feePerByte, sender);

        ByteBuffer buffer = ByteBuffer.allocate(transactionBase.length + 8);
        buffer.put(transactionBase);
        buffer.putLong(vidaId);

        return buffer.array();
    }

    public static byte[] getAddVidaSponsoredAddressesTransaction(long feePerByte, byte[] sender, long vidaId,
                                                                 Set<byte[]> sponsoredAddresses, int nonce, byte chainId) {
        byte[] transactionBase = getFalconTransactionBase(1036, nonce, chainId, feePerByte, sender);

        // Calculate total size needed: base + vidaId(8) + (sponsoredAddresses * 20)
        int totalSize = transactionBase.length + 8 + (sponsoredAddresses.size() * 20);

        ByteBuffer buffer = ByteBuffer.allocate(totalSize);
        buffer.put(transactionBase);
        buffer.putLong(vidaId);

        // Add each sponsored address
        for (byte[] sponsoredAddress : sponsoredAddresses) {
            buffer.put(sponsoredAddress);
        }

        return buffer.array();
    }

    public static byte[] getAddVidaAllowedSendersTransaction(long feePerByte, byte[] sender, long vidaId,
                                                             Set<byte[]> allowedSenders, int nonce, byte chainId) {
        byte[] transactionBase = getFalconTransactionBase(1037, nonce, chainId, feePerByte, sender);

        // Calculate total size needed: base + vidaId(8) + (allowedSenders * 20)
        int totalSize = transactionBase.length + 8 + (allowedSenders.size() * 20);

        ByteBuffer buffer = ByteBuffer.allocate(totalSize);
        buffer.put(transactionBase);
        buffer.putLong(vidaId);

        // Add each allowed sender address
        for (byte[] allowedSender : allowedSenders) {
            buffer.put(allowedSender);
        }

        return buffer.array();
    }

    public static byte[] getRemoveVidaAllowedSendersTransaction(long feePerByte, byte[] sender, long vidaId,
                                                                Set<byte[]> allowedSenders, int nonce, byte chainId) {
        byte[] transactionBase = getFalconTransactionBase(1038, nonce, chainId, feePerByte, sender);

        // Calculate total size needed: base + vidaId(8) + (allowedSenders * 20)
        int totalSize = transactionBase.length + 8 + (allowedSenders.size() * 20);

        ByteBuffer buffer = ByteBuffer.allocate(totalSize);
        buffer.put(transactionBase);
        buffer.putLong(vidaId);

        // Add each allowed sender address to remove
        for (byte[] allowedSender : allowedSenders) {
            buffer.put(allowedSender);
        }

        return buffer.array();
    }

    public static byte[] getRemoveSponsoredAddressesTransaction(long feePerByte, byte[] sender, long vidaId,
                                                                Set<byte[]> sponsoredAddresses, int nonce, byte chainId) {
        byte[] transactionBase = getFalconTransactionBase(1039, nonce, chainId, feePerByte, sender);

        // Calculate total size needed: base + vidaId(8) + (sponsoredAddresses * 20)
        int totalSize = transactionBase.length + 8 + (sponsoredAddresses.size() * 20);

        ByteBuffer buffer = ByteBuffer.allocate(totalSize);
        buffer.put(transactionBase);
        buffer.putLong(vidaId);

        // Add each sponsored address to remove
        for (byte[] sponsoredAddress : sponsoredAddresses) {
            buffer.put(sponsoredAddress);
        }

        return buffer.array();
    }

    public static byte[] getSetPWRTransferRightsTransaction(long feePerByte, byte[] sender, long vidaId,
                                                            boolean ownerCanTransferPWR, int nonce, byte chainId) {
        byte[] transactionBase = getFalconTransactionBase(1040, nonce, chainId, feePerByte, sender);

        // Calculate total size needed: base + vidaId(8) + (allowedSenders * 20)
        int totalSize = transactionBase.length + 8 + 1 + 1;

        ByteBuffer buffer = ByteBuffer.allocate(totalSize);
        buffer.put(transactionBase);
        buffer.putLong(vidaId);
        buffer.put((byte) (ownerCanTransferPWR ? 1 : 0));

        return buffer.array();
    }

    public static byte[] getTransferPWRFromVidaTransaction(long feePerByte, byte[] sender, long vidaId,
                                                                byte[] receiver, long amount, int nonce, byte chainId) {
        byte[] transactionBase = getFalconTransactionBase(1041, nonce, chainId, feePerByte, sender);

        ByteBuffer buffer = ByteBuffer.allocate(transactionBase.length + 8 + 20 + 8);
        buffer.put(transactionBase);
        buffer.putLong(vidaId);
        buffer.put(receiver);
        buffer.putLong(amount);

        return buffer.array();
    }
    //endregion
}