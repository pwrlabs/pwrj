package com.github.pwrlabs.pwrj.protocol;

import com.github.pwrlabs.pwrj.Utils.Hex;
import com.github.pwrlabs.pwrj.record.transaction.Interface.Transaction;
import com.github.pwrlabs.pwrj.record.transaction.ecdsa.*;
import org.web3j.crypto.Keys;
import org.web3j.crypto.Sign;

import java.math.BigInteger;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.security.SignatureException;
import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;

public class TransactionDecoder {

    public static Transaction decode(byte[] txn) {
        byte[] sender = getSender(txn);
        return decode(txn, sender);
    }

    public static Transaction decode(byte[] txn, byte[] sender) {
        ByteBuffer buffer = ByteBuffer.wrap(txn);
        buffer.position(1);
        int nonce = buffer.getInt();
        byte chainId = buffer.get();

        switch (txn[0]) {
            case 0:
                return decodeTransfer(txn, sender, nonce);
            case 1:
                return decodeJoin(txn, sender, nonce);
            case 2:
                return decodeClaimSpot(txn, sender, nonce);
            case 3:
                return decodeDelegate(txn, sender, nonce);
            case 4:
                return decodeWithdraw(txn, sender, nonce);
            case 5:
                return decodeVmDataTxn(txn, sender, nonce);
            case 6:
                return decodeClaimVmId(txn, sender, nonce);
            case 8:
                return decodeSetGuardianTxn(txn, sender, nonce);
            case 9:
                return decodeRemoveGuardianTxn(txn, sender, nonce);
            case 10:
                return decodeGuardianApprovalTxn(txn, sender, nonce);
            case 11:
                return decodePayableVmDataTxn(txn, sender, nonce);
            case 12:
                return decodeConduitApprovalTxn(txn, sender, nonce);
            case 13:
                return decodeSetConduitsTxn(txn, sender, nonce);
            case 14:
                return decodeAddConduitsTxn(txn, sender, nonce);
            case 15:
                return decodeRemoveConduitsTxn(txn, sender, nonce);
            case 16:
                return decodeMoveStakeTxn(txn, sender, nonce);
            case 17:
                return decodeChangeEarlyWithdrawPenaltyProposalTxn(txn, sender, nonce);
            case 18:
                return decodeChangeFeePerByteProposalTxn(txn, sender, nonce);
            case 19:
                return decodeChangeMaxBlockSizeProposalTxn(txn, sender, nonce);
            case 20:
                return decodeChangeMaxTxnSizeProposalTxn(txn, sender, nonce);
            case 21:
                return decodeChangeOverallBurnPercentageProposalTxn(txn, sender, nonce);
            case 22:
                return decodeChangeRewardPerYearProposalTxn(txn, sender, nonce);
            case 23:
                return decodeChangeValidatorCountLimitProposalTxn(txn, sender, nonce);
            case 24:
                return decodeChangeValidatorJoiningFeeProposalTxn(txn, sender, nonce);
            case 25:
                return decodeChangeVmIdClaimingFeeProposalTxn(txn, sender, nonce);
            case 26:
                return decodeChangeVmOwnerTxnFeeShareProposalTxn(txn, sender, nonce);
            case 27:
                return decodeOtherProposalTxn(txn, sender, nonce);
            case 28:
                return decodeVoteOnProposalTxn(txn, sender, nonce);
            case 29:
                return decodeChangeIpTxn(txn, sender, nonce);

            default: {
                throw new RuntimeException("Invalid txn identifier: " + txn[0]);
            }
        }
    }

    private static Transaction decodeTransfer(byte[] txn, byte[] sender, int nonce) {
        if (txn.length != 34 /*Without Signature*/ && txn.length != 99 /*With Signature*/) {
            throw new RuntimeException("Invalid txn length for transfer txn");
        }

        // Layout:
        // Identifier - 1
        // chain id - 1
        // nonce - 4 
        // amount - 8
        // recipient - 20
        // signature - 65

        ByteBuffer buffer = ByteBuffer.wrap(txn);
        buffer.position(6);

        long amount = buffer.getLong();
        byte[] recipientByte = new byte[20];
        buffer.get(recipientByte);

        return TransferTransaction.builder()
                .sender("0x" + Hex.toHexString(sender))
                .receiver("0x" + Hex.toHexString(recipientByte))
                .value(amount)
                .nonce(nonce)
                .size(txn.length)
                .rawTransaction(txn)
                .chainId(txn[1])
                .build();
    }

    private static Transaction decodeJoin(byte[] txn, byte[] sender, int nonce) {
        if (txn.length < 79 || txn.length > 87) {
            throw new RuntimeException("Invalid length for join txn");
        }

        /*
         * Identifier - 1
         * chain id - 1
         * nonce - 4
              ip - X
         * signature - 65*/

        ByteBuffer buffer = ByteBuffer.wrap(txn);
        buffer.position(6);

        byte[] ipByteArray = new byte[txn.length - 71];
        buffer.get(ipByteArray);

        return JoinTransaction.builder()
                .sender("0x" + Hex.toHexString(sender))
                .nonce(nonce)
                .size(txn.length)
                .ip(new String(ipByteArray, StandardCharsets.UTF_8))
                .rawTransaction(txn)
                .chainId(txn[1])
                .build();
    }

    private static Transaction decodeClaimSpot(byte[] txn, byte[] sender, int nonce) {
        if (txn.length != 71) throw new RuntimeException("Invalid length for claim spot txn");

        /*
         * Identifier - 1
         * chain id - 1
         * nonce - 4
         * signature - 65
         * */

        return ClaimSpotTransaction.builder()
                .sender("0x" + Hex.toHexString(sender))
                .nonce(nonce)
                .size(txn.length)
                .rawTransaction(txn)
                .chainId(txn[1])
                .build();
    }

    private static Transaction decodeDelegate(byte[] txn, byte[] sender, int nonce) {
        if (txn.length != 34 /*Without Signature*/ && txn.length != 99 /*With Signature*/)
            throw new RuntimeException("Invalid length for delegate txn");

        /*
         * Identifier - 1
         * chain id - 1
         * nonce - 4           
         * amount - 8
         * validator - 20
         * signature - 65*/

        ByteBuffer buffer = ByteBuffer.wrap(txn);
        buffer.position(6);

        long amount = buffer.getLong();
        byte[] validator = new byte[20];
        buffer.get(validator);

        return DelegateTransaction.builder()
                .sender("0x" + Hex.toHexString(sender))
                .validator("0x" + Hex.toHexString(validator))
                .value(amount)
                .nonce(nonce)
                .size(txn.length)
                .rawTransaction(txn)
                .chainId(txn[1])
                .build();
    }

    private static Transaction decodeWithdraw(byte[] txn, byte[] sender, int nonce) {
        if (txn.length != 34 /*Without Signature*/ && txn.length != 99 /*With Signature*/)
            throw new RuntimeException("Invalid length for withdraw txn");

        /*
         * Identifier - 1
         * chain id - 1
         * nonce - 4           
         * amount - 8
         * validator - 20
         * signature - 65*/

        ByteBuffer buffer = ByteBuffer.wrap(txn);
        buffer.position(6);

        long sharesAmount = buffer.getLong();
        byte[] validator = new byte[20];
        buffer.get(validator);

        return WithdrawTransaction.builder()
                .sender("0x" + Hex.toHexString(sender))
                .validator("0x" + Hex.toHexString(validator))
                .value(sharesAmount)
                .nonce(nonce)
                .size(txn.length)
                .rawTransaction(txn)
                .chainId(txn[1])
                .build();
    }

    private static Transaction decodeVmDataTxn(byte[] txn, byte[] sender, int nonce) {
        if (txn.length < 14) throw new RuntimeException("Invalid length for VM Data txn");

        ByteBuffer buffer = ByteBuffer.wrap(txn);
        buffer.position(6);

        long externalVmId = buffer.getLong();

        /*
         * Identifier - 1
         * chain id - 1
         * nonce - 4           
         * External VM ID - 8
         * Data - x
         * signature - 65
         * */

        int dataLength;
        if (PWRJ.isVmAddress(Hex.toHexString(sender))) dataLength = txn.length - 14;
        else dataLength = txn.length - 79;

        byte[] data = new byte[dataLength];
        buffer.get(data);

        return VmDataTransaction.builder()
                .sender("0x" + Hex.toHexString(sender))
                .nonce(nonce)
                .size(txn.length)
                .vmId(externalVmId)
                .data("0x" + Hex.toHexString(data))
                .rawTransaction(txn)
                .chainId(txn[1])
                .build();
    }

    private static Transaction decodeClaimVmId(byte[] txn, byte[] sender, int nonce) {
        if (txn.length != 14 /*Without Signature*/ && txn.length != 79 /*With Signature*/)
            throw new RuntimeException("Invalid length for claim vm id txn");
        ;

        ByteBuffer buffer = ByteBuffer.wrap(txn);
        buffer.position(6);

        long vmId = buffer.getLong();

        /*
         * Identifier - 1
         * chain id - 1
         * nonce - 4           
         * External VM ID - 8
         * signature - 65
         * */

        return ClaimVmIdTransaction.builder()
                .sender("0x" + Hex.toHexString(sender))
                .nonce(nonce)
                .size(txn.length)
                .vmId(vmId)
                .rawTransaction(txn)
                .chainId(txn[1])
                .build();
    }

    private static Transaction decodeSetGuardianTxn(byte[] txn, byte[] sender, int nonce) {
        if (txn.length != 99) throw new RuntimeException("Invalid length for set guardian txn");
        ;

        ByteBuffer buffer = ByteBuffer.wrap(txn);
        buffer.position(6);

        long expiryDate = buffer.getLong();
        byte[] guardianAddress = new byte[20];
        buffer.get(guardianAddress);

        /*
         * Identifier - 1
         * chain id - 1
         * nonce - 4           
         * Long - 8
         * address - 20
         * signature - 65
         * */

        return SetGuardianTransaction.builder()
                .sender("0x" + Hex.toHexString(sender))
                .nonce(nonce)
                .size(txn.length)
                .expiryDate(expiryDate)
                .guardian("0x" + Hex.toHexString(guardianAddress))
                .rawTransaction(txn)
                .chainId(txn[1])
                .build();
    }

    private static Transaction decodeRemoveGuardianTxn(byte[] txn, byte[] sender, int nonce) {
        if (txn.length != 71) throw new RuntimeException("Invalid length for remove guardian txn");
        ;

        /*
         * Identifier - 1
         * chain id - 1
         * nonce - 4           
         * signature - 65
         * */

        return RemoveGuardianTransaction.builder()
                .sender("0x" + Hex.toHexString(sender))
                .nonce(nonce)
                .size(txn.length)
                .rawTransaction(txn)
                .chainId(txn[1])
                .build();
    }

    private static Transaction decodeGuardianApprovalTxn(byte[] txn, byte[] sender, int nonce) {
        ByteBuffer buffer = ByteBuffer.wrap(txn);
        buffer.position(6);

        List<byte[]> wrappedTxns = new LinkedList<>();

        while (buffer.remaining() > 65) {
            int txnLength = buffer.getInt();
            byte[] wrappedTxn = new byte[txnLength];
            buffer.get(wrappedTxn);
            wrappedTxns.add(wrappedTxn);
        }

        List<Transaction> txns = new LinkedList<>();
        for (byte[] wrappedTxn : wrappedTxns) {
            Transaction t = decode(wrappedTxn);
            txns.add(t);
        }

        /*
         * Identifier - 1
         * chain id - 1
         * nonce - 4           
         * Txn - x
         * signature - 65
         * */

        return GuardianApprovalTransaction.builder()
                .sender("0x" + Hex.toHexString(sender))
                .nonce(nonce)
                .size(txn.length)
                .transactions(txns)
                .rawTransaction(txn)
                .chainId(txn[1])
                .build();
    }

    private static Transaction decodePayableVmDataTxn(byte[] txn, byte[] sender, int nonce) {
        if (txn.length < 22) throw new RuntimeException("Invalid length for payable VM Data txn");

        ByteBuffer buffer = ByteBuffer.wrap(txn);
        buffer.position(6);

        long externalVmId = buffer.getLong();

        /*
         * Identifier - 1
         * chain id - 1
         * nonce - 4           
         * External VM ID - 8
         * Data - x
         * Value: 8
         * signature - 65
         * */


        int dataLength;
        if (PWRJ.isVmAddress(Hex.toHexString(sender))) dataLength = txn.length - 22;
        else dataLength = txn.length - 87;

        byte[] data = new byte[dataLength];

        buffer.get(data);
        long value = buffer.getLong();

        return PayableVmDataTransaction.builder()
                .sender("0x" + Hex.toHexString(sender))
                .nonce(nonce)
                .size(txn.length)
                .vmId(externalVmId)
                .data("0x" + Hex.toHexString(data))
                .value(value)
                .rawTransaction(txn)
                .chainId(txn[1])
                .build();
    }

    private static Transaction decodeConduitApprovalTxn(byte[] txn, byte[] sender, int nonce) {
        /*
         * Identifier - 1
         * chain id - 1
         * nonce - 4           
         * External VM ID - 8 //ID of VM the conduit node is sending the txn on behalf of
         * Wrapped Transaction - x [size identifier - 4 && Txn - y]
         * signature - 65
         * */

        ByteBuffer buffer = ByteBuffer.wrap(txn);
        buffer.position(6);

        long vmId = buffer.getLong();

        List<byte[]> wrappedTxns = new LinkedList<>();

        while (buffer.remaining() > 65) {
            int txnLength = buffer.getInt();
            byte[] wrappedTxn = new byte[txnLength];
            buffer.get(wrappedTxn);
            wrappedTxns.add(wrappedTxn);
        }

        byte[] vmAddress = Hex.decode(PWRJ.getVmIdAddress(vmId));
        List<Transaction> txns = new LinkedList<>();
        for (byte[] wrappedTxn : wrappedTxns) {
            Transaction t = decode(wrappedTxn, vmAddress);
            txns.add(t);
        }

        return ConduitApprovalTransaction.builder()
                .sender("0x" + Hex.toHexString(sender))
                .nonce(nonce)
                .size(txn.length)
                .transactions(txns)
                .rawTransaction(txn)
                .chainId(txn[1])
                .build();
    }

    private static Transaction decodeSetConduitsTxn(byte[] txn, byte[] sender, int nonce) {
        /*
         * Identifier - 1
         * chain id - 1
         * nonce - 4           
         * External VM ID - 8 //ID of VM the conduit node is sending the txn on behalf of
         * Conduits - x [20 - 20 - 20 - etc...]
         * signature - 65
         * */

        ByteBuffer buffer = ByteBuffer.wrap(txn);


        buffer.position(6);

        long vmId = buffer.getLong();

        List<String> conduits = new LinkedList<>();

        while (buffer.remaining() > 65) {
            int txnLength = buffer.getInt();
            byte[] conduit = new byte[txnLength];
            buffer.get(conduit);
            conduits.add("0x" + Hex.toHexString(conduit));
        }

        if (buffer.remaining() != 65) {
            throw new RuntimeException("Invalid remaining  length for set conduits txn. Remaining: " + buffer.remaining() + " Expected: 65");
        }

        return SetConduitsTransactions.builder()
                .sender("0x" + Hex.toHexString(sender))
                .nonce(nonce)
                .size(txn.length)
                .vmId(vmId)
                .conduits(conduits.toArray(new String[0]))
                .rawTransaction(txn)
                .chainId(txn[1])
                .build();
    }

    private static Transaction decodeAddConduitsTxn(byte[] txn, byte[] sender, int nonce) {
        /*
         * Identifier - 1
         * chain id - 1
         * nonce - 4           
         * External VM ID - 8 //ID of VM the conduit node is sending the txn on behalf of
         * Conduits - x [20 - 20 - 20 - etc...]
         * */

        ByteBuffer buffer = ByteBuffer.wrap(txn);
        buffer.position(6);

        long vmId = buffer.getLong();

        List<String> conduits = new LinkedList<>();

        while (buffer.hasRemaining()) {
            byte[] conduit = new byte[20];
            buffer.get(conduit);
            conduits.add("0x" + Hex.toHexString(conduit));
        }

        return AddConduitsTransaction.builder()
                .sender("0x" + Hex.toHexString(sender))
                .nonce(nonce)
                .size(txn.length)
                .vmId(vmId)
                .conduits(conduits.toArray(new String[0]))
                .rawTransaction(txn)
                .chainId(txn[1])
                .build();
    }

    private static Transaction decodeRemoveConduitsTxn(byte[] txn, byte[] sender, int nonce) {
        /*
         * Identifier - 1
         * chain id - 1
         * nonce - 4           
         * External VM ID - 8 //ID of VM the conduit node is sending the txn on behalf of
         * Conduits - x [20 - 20 - 20 - etc...]
         * */

        ByteBuffer buffer = ByteBuffer.wrap(txn);
        buffer.position(6);

        long vmId = buffer.getLong();

        List<String> conduits = new LinkedList<>();

        while (buffer.hasRemaining()) {
            byte[] conduit = new byte[20];
            buffer.get(conduit);
            conduits.add("0x" + Hex.toHexString(conduit));
        }

        return RemoveConduitsTransaction.builder()
                .sender("0x" + Hex.toHexString(sender))
                .nonce(nonce)
                .size(txn.length)
                .vmId(vmId)
                .conduits(conduits.toArray(new String[0]))
                .rawTransaction(txn)
                .chainId(txn[1])
                .build();
    }

    private static Transaction decodeMoveStakeTxn(byte[] txn, byte[] sender, int nonce) {
        if (txn.length != 54 && txn.length != 119) throw new RuntimeException("Invalid length for move stake txn");

        /*
         * Identifier - 1
         * Chain Id - 1
         * nonce - 4           
         * shares amount - 8
         * from validator - 20
         * to validator - 20
         * signature - 65
         * */

        ByteBuffer buffer = ByteBuffer.wrap(txn);
        buffer.position(6);

        long sharesAmount = buffer.getLong();
        byte[] fromValidator = new byte[20];
        buffer.get(fromValidator);
        byte[] toValidator = new byte[20];
        buffer.get(toValidator);

        return MoveStakeTransaction.builder()
                .sender("0x" + Hex.toHexString(sender))
                .nonce(nonce)
                .size(txn.length)
                .fromValidator("0x" + Hex.toHexString(fromValidator))
                .toValidator("0x" + Hex.toHexString(toValidator))
                .sharesAmount(sharesAmount)
                .rawTransaction(txn)
                .chainId(txn[1])
                .build();
    }

    private static Transaction decodeChangeEarlyWithdrawPenaltyProposalTxn(byte[] txn, byte[] sender, int nonce) {
        if (txn.length < 18)
            throw new RuntimeException("Invalid length for change early withdrawal penalty proposal txn");
        /*
        Identifier - 1
        chain id - 1
        nonce - 4
        title size identifier - 4
        title - x
        withdrawal penalty time - 8
        withdrawal penalty - 4
        description - x
        signature - 65
        */

        ByteBuffer buffer = ByteBuffer.wrap(txn);
        buffer.position(6);

        int titleLength = buffer.getInt();
        byte[] titleBytea = new byte[titleLength];
        buffer.get(titleBytea);
        String title = new String(titleBytea, StandardCharsets.UTF_8);
        
        long earlyWithdrawalTime = buffer.getLong();
        int withdrawalPenalty = buffer.getInt();
        

        int descriptionLength;
        if (PWRJ.isVmAddress(Hex.toHexString(sender))) descriptionLength = txn.length - 18;
        else descriptionLength = txn.length - 75;

        byte[] descriptionBytea = new byte[descriptionLength];
        buffer.get(descriptionBytea);
        String description = new String(descriptionBytea, StandardCharsets.UTF_8);

        return ChangeEarlyWithdrawPenaltyProposalTransaction.builder()
                .sender("0x" + Hex.toHexString(sender))
                .nonce(nonce)
                .size(txn.length)
                .extraFee(0)
                .withdrawalPenaltyTime(earlyWithdrawalTime)
                .withdrawalPenalty(withdrawalPenalty)
                .description(description).title(title)
                .rawTransaction(txn)
                .chainId(txn[1])
                .build();

    }

    public static Transaction decodeChangeFeePerByteProposalTxn(byte[] txn, byte[] sender, int nonce) {
        if (txn.length < 16) throw new RuntimeException("Invalid length for change fee per byte proposal txn");
        /*
        Identifier - 1
        chain id - 1
        nonce - 4
        title size identifier - 4
        title - x
        fee per byte - 8
        description - x
        signature - 65
        */

        ByteBuffer buffer = ByteBuffer.wrap(txn);
        buffer.position(6);

        int titleLength = buffer.getInt();
        byte[] titleBytea = new byte[titleLength];
        buffer.get(titleBytea);
        String title = new String(titleBytea, StandardCharsets.UTF_8);
        
        long feePerByte = buffer.getLong();
        

        int descriptionLength;
        if (PWRJ.isVmAddress(Hex.toHexString(sender))) descriptionLength = txn.length - 16;
        else descriptionLength = txn.length - 81;

        byte[] descriptionBytea = new byte[descriptionLength];
        buffer.get(descriptionBytea);
        String description = new String(descriptionBytea, StandardCharsets.UTF_8);

        return ChangeFeePerByteProposalTransaction.builder()
                .sender("0x" + Hex.toHexString(sender))
                .nonce(nonce)
                .size(txn.length)
                .feePerByte(feePerByte)
                .description(description).title(title)
                .rawTransaction(txn)
                .chainId(txn[1])
                .build();
    }

    public static Transaction decodeChangeMaxBlockSizeProposalTxn(byte[] txn, byte[] sender, int nonce) {
        if (txn.length < 10) throw new RuntimeException("Invalid length for change fee per byte proposal txn");
        /*
        Identifier - 1
        chain id - 1
        nonce - 4
        title size identifier - 4
        title - x
        max block size - 4
        description - x
        signature - 65
        */

        ByteBuffer buffer = ByteBuffer.wrap(txn);
        buffer.position(6);

        int titleLength = buffer.getInt();
        byte[] titleBytea = new byte[titleLength];
        buffer.get(titleBytea);
        String title = new String(titleBytea, StandardCharsets.UTF_8);
        
        int maxBlockSize = buffer.getInt();
        

        int descriptionLength;
        if (PWRJ.isVmAddress(Hex.toHexString(sender))) descriptionLength = txn.length - 10;
        else descriptionLength = txn.length - 75;

        byte[] descriptionBytea = new byte[descriptionLength];
        buffer.get(descriptionBytea);
        String description = new String(descriptionBytea, StandardCharsets.UTF_8);

        return ChangeMaxBlockSizeProposalTranscation.builder()
                .sender("0x" + Hex.toHexString(sender))
                .nonce(nonce)
                .size(txn.length)
                .maxBlockSize(maxBlockSize)
                .description(description).title(title)
                .rawTransaction(txn)
                .chainId(txn[1])
                .build();
    }

    public static Transaction decodeChangeMaxTxnSizeProposalTxn(byte[] txn, byte[] sender, int nonce) {
        if (txn.length < 10) throw new RuntimeException("Invalid length for change fee per byte proposal txn");
        /*
        Identifier - 1
        chain id - 1
        nonce - 4
        title size identifier - 4
        title - x
        max txn size - 4
        description - x
        signature - 65
        */

        ByteBuffer buffer = ByteBuffer.wrap(txn);
        buffer.position(6);

        int titleLength = buffer.getInt();
        byte[] titleBytea = new byte[titleLength];
        buffer.get(titleBytea);
        String title = new String(titleBytea, StandardCharsets.UTF_8);
        
        int maxTxnSize = buffer.getInt();
        

        int descriptionLength = PWRJ.isVmAddress(Hex.toHexString(sender)) ? txn.length - 10 : txn.length - 75;

        byte[] descriptionBytea = new byte[descriptionLength];
        buffer.get(descriptionBytea);
        String description = new String(descriptionBytea, StandardCharsets.UTF_8);

        return ChangeMaxTxnSizeProposalTransaction.builder()
                .sender("0x" + Hex.toHexString(sender))
                .nonce(nonce)
                .size(txn.length)
                .maxTxnSize(maxTxnSize)
                .description(description).title(title)
                .rawTransaction(txn)
                .chainId(txn[1])
                .build();
    }

    public static Transaction decodeChangeOverallBurnPercentageProposalTxn(byte[] txn, byte[] sender, int nonce) {
        if (txn.length < 10) throw new RuntimeException("Invalid length for change fee per byte proposal txn");
        /*
        Identifier - 1
        chain id - 1
        nonce - 4
        title size identifier - 4
        title - x
        burn percentage - 4
        description - x
        signature - 65
        */

        ByteBuffer buffer = ByteBuffer.wrap(txn);
        buffer.position(6);

        int titleLength = buffer.getInt();
        byte[] titleBytea = new byte[titleLength];
        buffer.get(titleBytea);
        String title = new String(titleBytea, StandardCharsets.UTF_8);
        
        int burnPercentage = buffer.getInt();
        

        int descriptionLength = PWRJ.isVmAddress(Hex.toHexString(sender)) ? txn.length - 10 : txn.length - 75;

        byte[] descriptionBytea = new byte[descriptionLength];
        buffer.get(descriptionBytea);
        String description = new String(descriptionBytea, StandardCharsets.UTF_8);

        return ChangeOverallBurnPercentageProposalTransaction.builder()
                .sender("0x" + Hex.toHexString(sender))
                .nonce(nonce)
                .size(txn.length)
                .burnPercentage(burnPercentage)
                .description(description).title(title)
                .rawTransaction(txn)
                .chainId(txn[1])
                .build();
    }

    public static Transaction decodeChangeRewardPerYearProposalTxn(byte[] txn, byte[] sender, int nonce) {
        if (txn.length < 14) throw new RuntimeException("Invalid length for change fee per byte proposal txn");
        /*
        Identifier - 1
        chain id - 1
        nonce - 4
        title size identifier - 4
        title - x
        reward per year - 8
        description - x
        signature - 65
        */

        ByteBuffer buffer = ByteBuffer.wrap(txn);
        buffer.position(6);

        int titleLength = buffer.getInt();
        byte[] titleBytea = new byte[titleLength];
        buffer.get(titleBytea);
        String title = new String(titleBytea, StandardCharsets.UTF_8);
        
        long rewardPerYear = buffer.getLong();
        

        int descriptionLength = PWRJ.isVmAddress(Hex.toHexString(sender)) ? txn.length - 14 : txn.length - 79;

        byte[] descriptionBytea = new byte[descriptionLength];
        buffer.get(descriptionBytea);
        String description = new String(descriptionBytea, StandardCharsets.UTF_8);

        return ChangeRewardPerYearProposalTransaction.builder()
                .sender("0x" + Hex.toHexString(sender))
                .nonce(nonce)
                .size(txn.length)
                .rewardPerYear(rewardPerYear)
                .description(description).title(title)
                .rawTransaction(txn)
                .chainId(txn[1])
                .build();
    }

    public static Transaction decodeChangeValidatorCountLimitProposalTxn(byte[] txn, byte[] sender, int nonce) {
        if (txn.length < 10) throw new RuntimeException("Invalid length for change fee per byte proposal txn");
        /*
        Identifier - 1
        chain id - 1
        nonce - 4
        title size identifier - 4
        title - x
        validator count limit - 4
        description - x
        signature - 65
        */

        ByteBuffer buffer = ByteBuffer.wrap(txn);
        buffer.position(6);

        int titleLength = buffer.getInt();
        byte[] titleBytea = new byte[titleLength];
        buffer.get(titleBytea);
        String title = new String(titleBytea, StandardCharsets.UTF_8);
        
        int validatorCountLimit = buffer.getInt();
        

        int descriptionLength = PWRJ.isVmAddress(Hex.toHexString(sender)) ? txn.length - 10 : txn.length - 75;

        byte[] descriptionBytea = new byte[descriptionLength];
        buffer.get(descriptionBytea);
        String description = new String(descriptionBytea, StandardCharsets.UTF_8);

        return ChangeValidatorCountLimitProposalTransaction.builder()
                .sender("0x" + Hex.toHexString(sender))
                .nonce(nonce)
                .size(txn.length)
                .validatorCountLimit(validatorCountLimit)
                .description(description).title(title)
                .rawTransaction(txn)
                .chainId(txn[1])
                .build();
    }

    public static Transaction decodeChangeValidatorJoiningFeeProposalTxn(byte[] txn, byte[] sender, int nonce) {
        if (txn.length < 14) throw new RuntimeException("Invalid length for change fee per byte proposal txn");
        /*
        Identifier - 1
        chain id - 1
        nonce - 4
        title size identifier - 4
        title - x
        joining fee - 8
        description - x
        signature - 65
        */

        ByteBuffer buffer = ByteBuffer.wrap(txn);
        buffer.position(6);

        int titleLength = buffer.getInt();
        byte[] titleBytea = new byte[titleLength];
        buffer.get(titleBytea);
        String title = new String(titleBytea, StandardCharsets.UTF_8);
        
        long joiningFee = buffer.getLong();
        

        int descriptionLength = PWRJ.isVmAddress(Hex.toHexString(sender)) ? txn.length - 14 : txn.length - 79;

        byte[] descriptionBytea = new byte[descriptionLength];
        buffer.get(descriptionBytea);
        String description = new String(descriptionBytea, StandardCharsets.UTF_8);

        return ChangeValidatorJoiningFeeProposalTranscation.builder()
                .sender("0x" + Hex.toHexString(sender))
                .nonce(nonce)
                .size(txn.length)
                .joiningFee(joiningFee)
                .description(description).title(title)
                .rawTransaction(txn)
                .chainId(txn[1])
                .build();
    }

    private static Transaction decodeChangeVmIdClaimingFeeProposalTxn(byte[] txn, byte[] sender, int nonce) {
        if (txn.length < 18) throw new RuntimeException("Invalid length for change vm id claiming fee proposal txn");
        /*
        Identifier - 1
        chain id - 1
        nonce - 4
        title size identifier - 4
        title - x
        vm id claiming fee - 8
        description - x
        signature - 65
        */

        ByteBuffer buffer = ByteBuffer.wrap(txn);
        buffer.position(6);

        int titleLength = buffer.getInt();
        byte[] titleBytea = new byte[titleLength];
        buffer.get(titleBytea);
        String title = new String(titleBytea, StandardCharsets.UTF_8);
        
        long vmIdClaimingFee = buffer.getLong();
        

        int descriptionLength;
        if (PWRJ.isVmAddress(Hex.toHexString(sender))) descriptionLength = txn.length - 16;
        else descriptionLength = txn.length - 81;

        byte[] descriptionBytea = new byte[descriptionLength];
        buffer.get(descriptionBytea);
        String description = new String(descriptionBytea, StandardCharsets.UTF_8);

        return ChangeVmIdClaimingFeeProposalTransaction.builder()
                .sender("0x" + Hex.toHexString(sender))
                .nonce(nonce)
                .size(txn.length)
                .claimingFee(vmIdClaimingFee)
                .description(description).title(title)
                .rawTransaction(txn)
                .chainId(txn[1])
                .build();
    }

    private static Transaction decodeChangeVmOwnerTxnFeeShareProposalTxn(byte[] txn, byte[] sender, int nonce) {
        if (txn.length < 10)
            throw new RuntimeException("Invalid length for change vm owner txn fee share proposal txn");
        /*
        Identifier - 1
        chain id - 1
        nonce - 4
        title size identifier - 4
        title - x
        vm owner txn fee share - 4
        description - x
        signature - 65
        */

        ByteBuffer buffer = ByteBuffer.wrap(txn);
        buffer.position(6);

        int titleLength = buffer.getInt();
        byte[] titleBytea = new byte[titleLength];
        buffer.get(titleBytea);
        String title = new String(titleBytea, StandardCharsets.UTF_8);
        
        int vmOwnerTxnFeeShare = buffer.getInt();
        

        int descriptionLength;
        if (PWRJ.isVmAddress(Hex.toHexString(sender))) descriptionLength = txn.length - 10;
        else descriptionLength = txn.length - 75;

        byte[] descriptionBytea = new byte[descriptionLength];
        buffer.get(descriptionBytea);
        String description = new String(descriptionBytea, StandardCharsets.UTF_8);

        return ChangeVmOwnerTxnFeeShareProposalTransaction.builder()
                .sender("0x" + Hex.toHexString(sender))
                .nonce(nonce)
                .size(txn.length)
                .feeShare(vmOwnerTxnFeeShare)
                .description(description).title(title)
                .rawTransaction(txn)
                .chainId(txn[1])
                .build();
    }

    private static Transaction decodeOtherProposalTxn(byte[] txn, byte[] sender, int nonce) {
        if (txn.length < 6) throw new RuntimeException("Invalid length for change vm owner txn fee share proposal txn");
        /*
        Identifier - 1
        chain id - 1
        nonce - 4
        title size identifier - 4
        title - x
        description - x
        signature - 65
        */

        ByteBuffer buffer = ByteBuffer.wrap(txn);
        buffer.position(6);
        
        int titleLength = buffer.getInt();
        byte[] titleBytea = new byte[titleLength];
        buffer.get(titleBytea);
        String title = new String(titleBytea, StandardCharsets.UTF_8);

        int descriptionLength;
        if (PWRJ.isVmAddress(Hex.toHexString(sender))) descriptionLength = txn.length - 10;
        else descriptionLength = txn.length - 75;

        byte[] descriptionBytea = new byte[descriptionLength];
        buffer.get(descriptionBytea);
        String description = new String(descriptionBytea, StandardCharsets.UTF_8);

        return OtherProposalTxn.builder()
                .sender("0x" + Hex.toHexString(sender))
                .nonce(nonce)
                .size(txn.length)
                .description(description).title(title)
                .rawTransaction(txn)
                .chainId(txn[1])
                .build();
    }

    private static Transaction decodeVoteOnProposalTxn(byte[] txn, byte[] sender, int nonce) {
        /*
        Identifier - 1
        chain id - 1
        nonce - 4
        proposal hash - 32
        vote - 1
        signature - 65
        */

        ByteBuffer buffer = ByteBuffer.wrap(txn);
        buffer.position(6);

        byte[] proposalHash = new byte[32];
        buffer.get(proposalHash);
        byte vote = buffer.get();

        return VoteOnProposalTxn.builder()
                .sender("0x" + Hex.toHexString(sender))
                .nonce(nonce)
                .size(txn.length)
                .proposalHash("0x" + Hex.toHexString(proposalHash))
                .vote(vote)
                .rawTransaction(txn)
                .chainId(txn[1])
                .build();
    }

    public static Transaction decodeChangeIpTxn(byte[] txn, byte[] sender, int nonce) {
        if (txn.length < 78 || txn.length > 86) {
            throw new RuntimeException("Invalid length for change ip txn");
        }

        /*
         * Identifier - 1
         * chain id - 1
         * nonce - 4
         * ip - X
         * signature - 65
         * */

        ByteBuffer buffer = ByteBuffer.wrap(txn);
        buffer.position(6);

        byte[] ipByteArray = new byte[txn.length - 71];
        buffer.get(ipByteArray);

        return ChangeIpTransaction.builder()
                .sender("0x" + Hex.toHexString(sender))
                .nonce(nonce)
                .size(txn.length)
                .newIp(new String(ipByteArray, StandardCharsets.UTF_8))
                .rawTransaction(txn)
                .chainId(txn[1])
                .build();
    }

    private static byte[] getSender(byte[] txn) {
        byte[] signature = new byte[65];
        byte[] txnData = new byte[txn.length - 65];

        System.arraycopy(txn, 0, txnData, 0, txnData.length);
        System.arraycopy(txn, txnData.length, signature, 0, 65);

        return getSigner(txnData, signature);
    }


    public static byte[] getSigner(byte[] txn, byte[] signature) {
        try {
            Sign.SignatureData sigData = new Sign.SignatureData(signature[64], Arrays.copyOfRange(signature, 0, 32), Arrays.copyOfRange(signature, 32, 64));
            BigInteger pubKey = Sign.signedMessageToKey(txn, sigData);
            return org.bouncycastle.util.encoders.Hex.decode(Keys.getAddress(pubKey));
        } catch (SignatureException e) {
            throw new RuntimeException("Failed to get signer from signature", e);
        }
    }
}

