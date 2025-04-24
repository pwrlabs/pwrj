package com.github.pwrlabs.pwrj.wallet;

import com.github.pwrlabs.pwrj.Utils.Falcon;
import com.github.pwrlabs.pwrj.Utils.Hex;
import com.github.pwrlabs.pwrj.Utils.PWRHash;
import com.github.pwrlabs.pwrj.protocol.PWRJ;
import com.github.pwrlabs.pwrj.protocol.TransactionBuilder;
import com.github.pwrlabs.pwrj.record.response.Response;
import io.pwrlabs.util.encoders.ByteArrayWrapper;
import org.bouncycastle.crypto.AsymmetricCipherKeyPair;
import org.bouncycastle.pqc.crypto.falcon.*;

import java.io.*;
import java.math.BigInteger;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.*;

import static com.github.pwrlabs.pwrj.Utils.NewError.errorIf;

import org.bouncycastle.pqc.crypto.falcon.FalconPrivateKeyParameters;
import org.bouncycastle.pqc.crypto.falcon.FalconPublicKeyParameters;

public class PWRFalconWallet {

    private final AsymmetricCipherKeyPair keyPair;
    private PWRJ pwrj;
    private byte[] address;

    public PWRFalconWallet(PWRJ pwrj) {
        this.pwrj = pwrj;
        this.keyPair = Falcon.generateKeyPair512();

        FalconPublicKeyParameters publicKey = (FalconPublicKeyParameters) keyPair.getPublic();
        byte[] hash = PWRHash.hash224(publicKey.getH());
        address = Arrays.copyOfRange(hash, 0, 20);
    }

    public PWRFalconWallet(PWRJ pwrj, AsymmetricCipherKeyPair keyPair) {
        this.pwrj = pwrj;
        this.keyPair = keyPair;

        FalconPublicKeyParameters publicKey = (FalconPublicKeyParameters) keyPair.getPublic();
        byte[] hash = PWRHash.hash224(publicKey.getH());
        address = Arrays.copyOfRange(hash, 0, 20);
    }

    /**
     * Stores the wallet's key pair to disk in PEM format.
     *
     * @param filePath Path to the file where the wallet will be stored.
     * @throws IOException if there's an error writing to the file.
     */
    public void storeWallet(String filePath) throws IOException {
        // Cast to FalconPrivateKeyParameters and FalconPublicKeyParameters
        FalconPrivateKeyParameters falconPrivKey = (FalconPrivateKeyParameters) keyPair.getPrivate();

        byte[] g = falconPrivKey.getG();
        byte[] f = falconPrivKey.getSpolyf();
        byte[] F = falconPrivKey.getSpolyF();
        byte[] publicKeyBytes = falconPrivKey.getPublicKey();

        ByteBuffer buffer = ByteBuffer.allocate(4 + g.length + 4 + f.length + 4 + F.length + 4 + publicKeyBytes.length);
        buffer.putInt((short) g.length);
        buffer.put(g);
        buffer.putInt((short) f.length);
        buffer.put(f);
        buffer.putInt((short) F.length);
        buffer.put(F);
        buffer.putInt((short) publicKeyBytes.length);
        buffer.put(publicKeyBytes);

        Files.write(Paths.get(filePath), buffer.array());
    }

    /**
     * Loads a Falcon wallet (private/public key pair) from a PEM file on disk.
     *
     * @param pwrj     The PWRJ instance to use for the wallet.
     * @param filePath Path to the PEM file that contains the private/public keys.
     * @return a new PWRFalcon512Wallet with the loaded key pair.
     * @throws IOException if there's an error reading the file or parsing the keys.
     */
    public static PWRFalconWallet loadWallet(PWRJ pwrj, String filePath) throws IOException {
        byte[] data = Files.readAllBytes(Paths.get(filePath));
        if(data == null) throw new IOException("File is empty");

        ByteBuffer buffer = ByteBuffer.wrap(data);
        int gLength = buffer.getInt();
        byte[] g = new byte[gLength];
        buffer.get(g);

        int fLength = buffer.getInt();
        byte[] f = new byte[fLength];
        buffer.get(f);

        int FLength = buffer.getInt();
        byte[] F = new byte[FLength];
        buffer.get(F);

        int publicKeyLength = buffer.getInt();
        byte[] publicKeyBytes = new byte[publicKeyLength];
        buffer.get(publicKeyBytes);

        FalconParameters p = FalconParameters.falcon_512;
        FalconPrivateKeyParameters falconPrivKey = new FalconPrivateKeyParameters(p, f, g, F, publicKeyBytes);
        FalconPublicKeyParameters falconPubKey = new FalconPublicKeyParameters(p, publicKeyBytes);
        AsymmetricCipherKeyPair keyPair = new AsymmetricCipherKeyPair(falconPubKey, falconPrivKey);

        return new PWRFalconWallet(pwrj, keyPair);
    }


    public String getAddress() {
        return "0x" + Hex.toHexString(address);
    }

    public byte[] getByteaAddress() {
        return address;
    }

    public byte[] getPublicKey() {
        FalconPublicKeyParameters publicKey = (FalconPublicKeyParameters) keyPair.getPublic();
        return publicKey.getH();
    }

    public byte[] sign(byte[] data) {
        return Falcon.sign(data, keyPair);
    }

    public byte[] getSignedTransaction(byte[] transaction) {
        byte[] txnHash = PWRHash.hash256(transaction);
        byte[] signature = sign(txnHash);

        ByteBuffer buffer = ByteBuffer.allocate(2 + signature.length + transaction.length);
        buffer.put(transaction);
        buffer.put(signature);
        buffer.putShort((short) signature.length);

        return buffer.array();
    }

    public byte[] getSignedSetPublicKeyTransaction(Long feePerByte) throws IOException {
        long baseFeePerByte = pwrj.getFeePerByte();
        if(feePerByte == null || feePerByte == 0) feePerByte = baseFeePerByte;
        errorIf(feePerByte < baseFeePerByte, "Fee per byte must be greater than or equal to " + baseFeePerByte);

        FalconPublicKeyParameters publicKey = (FalconPublicKeyParameters) keyPair.getPublic();
        byte[] transaction = TransactionBuilder.getSetPublicKeyTransaction(feePerByte, publicKey.getH(), address, pwrj.getNonceOfAddress(getAddress()), pwrj.getChainId());
        return getSignedTransaction(transaction);
    }

    public byte[] getSignedTransferTransaction(byte[] receiver, long amount, Long feePerByte) throws IOException {
        errorIf(receiver.length != 20, "Receiver address must be 20 bytes long");
        long baseFeePerByte = pwrj.getFeePerByte();
        if(feePerByte == null || feePerByte == 0) feePerByte = baseFeePerByte;
        errorIf(feePerByte < baseFeePerByte, "Fee per byte must be greater than or equal to " + baseFeePerByte);

        byte[] transaction = TransactionBuilder.getTransferTransaction(feePerByte, address, receiver, amount, pwrj.getNonceOfAddress(getAddress()), pwrj.getChainId());
        return getSignedTransaction(transaction);
    }

    public byte[] getSignedJoinAsValidatorTransaction(long feePerByte, String ip) throws IOException {
        long baseFeePerByte = pwrj.getFeePerByte();
        if(feePerByte == 0) feePerByte = baseFeePerByte;
        errorIf(feePerByte < baseFeePerByte, "Fee per byte must be greater than or equal to " + baseFeePerByte);

        byte[] transaction = TransactionBuilder.getFalconJoinAsValidatorTransaction(feePerByte, address, ip, pwrj.getNonceOfAddress(getAddress()), pwrj.getChainId());
        return getSignedTransaction(transaction);
    }

    public byte[] getSignedDelegateTransaction(byte[] validator, long pwrAmount, Long feePerByte) throws IOException {
        errorIf(validator.length != 20, "Validator address must be 20 bytes long");
        long baseFeePerByte = pwrj.getFeePerByte();
        if(feePerByte == null || feePerByte == 0) feePerByte = baseFeePerByte;
        errorIf(feePerByte < baseFeePerByte, "Fee per byte must be greater than or equal to " + baseFeePerByte);

        byte[] transaction = TransactionBuilder.getFalconDelegateTransaction(feePerByte, address, validator, pwrAmount, pwrj.getNonceOfAddress(getAddress()), pwrj.getChainId());
        return getSignedTransaction(transaction);
    }

    public byte[] getSignedChangeIpTransaction(String newIp, Long feePerByte) throws IOException {
        errorIf(newIp == null || newIp.isEmpty() || newIp.length() < 7 || newIp.length() > 15, "Invalid IP address");
        long baseFeePerByte = pwrj.getFeePerByte();
        if(feePerByte == null || feePerByte == 0) feePerByte = baseFeePerByte;
        errorIf(feePerByte < baseFeePerByte, "Fee per byte must be greater than or equal to " + baseFeePerByte);

        byte[] transaction = TransactionBuilder.getFalconChangeIpTransaction(feePerByte, address, newIp, pwrj.getNonceOfAddress(getAddress()), pwrj.getChainId());
        return getSignedTransaction(transaction);
    }

    public byte[] getSignedClaimActiveNodeSpotTransaction(Long feePerByte) throws IOException {
        long baseFeePerByte = pwrj.getFeePerByte();
        if(feePerByte == null || feePerByte == 0) feePerByte = baseFeePerByte;
        errorIf(feePerByte < baseFeePerByte, "Fee per byte must be greater than or equal to " + baseFeePerByte);

        byte[] transaction = TransactionBuilder.getFalconClaimActiveNodeSpotTransaction(feePerByte, address, pwrj.getNonceOfAddress(getAddress()), pwrj.getChainId());
        return getSignedTransaction(transaction);
    }

    // Action methods that use the getSigned...Transaction methods
    public Response setPublicKey(Long feePerByte) throws IOException {
        return pwrj.broadcastTransaction(getSignedSetPublicKeyTransaction(feePerByte));
    }

    public Response transferPWR(byte[] receiver, long amount, Long feePerByte) throws IOException {
        Response response = makeSurePublicKeyIsSet(feePerByte);
        if(response != null && !response.isSuccess()) return response;

        return pwrj.broadcastTransaction(getSignedTransferTransaction(receiver, amount, feePerByte));
    }

    public Response joinAsValidator(long feePerByte, String ip) throws IOException {
        Response response = makeSurePublicKeyIsSet(feePerByte);
        if(response != null && !response.isSuccess()) return response;

        return pwrj.broadcastTransaction(getSignedJoinAsValidatorTransaction(feePerByte, ip));
    }

    public Response delegate(byte[] validator, long pwrAmount, Long feePerByte) throws IOException {
        Response response = makeSurePublicKeyIsSet(feePerByte);
        if(response != null && !response.isSuccess()) return response;

        return pwrj.broadcastTransaction(getSignedDelegateTransaction(validator, pwrAmount, feePerByte));
    }

    public Response changeIp(String newIp, Long feePerByte) throws IOException {
        Response response = makeSurePublicKeyIsSet(feePerByte);
        if(response != null && !response.isSuccess()) return response;

        return pwrj.broadcastTransaction(getSignedChangeIpTransaction(newIp, feePerByte));
    }

    public Response claimActiveNodeSpot(Long feePerByte) throws IOException {
        Response response = makeSurePublicKeyIsSet(feePerByte);
        if(response != null && !response.isSuccess()) return response;

        return pwrj.broadcastTransaction(getSignedClaimActiveNodeSpotTransaction(feePerByte));
    }

    public Response makeSurePublicKeyIsSet(long feePerByte) throws IOException {
        if(pwrj.getNonceOfAddress(getAddress()) == 0) {
            Response r = setPublicKey(feePerByte);
            if(!r.isSuccess()) {
                System.out.println("Failed to set public key");
                System.out.println(r.getError());
                return r;
            }
            else {
                long startingTime = System.currentTimeMillis();
                while (pwrj.getPublicKeyOfAddress(getAddress()) == null) {
                    if(System.currentTimeMillis() - startingTime > 30000) break;

                    try {Thread.sleep(1000);} catch (InterruptedException e) {e.printStackTrace();}
                }

                if(pwrj.getPublicKeyOfAddress(getAddress()) == null) {
                    return new Response(false, null, "Failed to set public key");
                } else {
                    System.out.println("Public key set successfully");
                }
            }
        }

        return null;
    }

    public byte[] getSignedChangeEarlyWithdrawPenaltyProposalTransaction(String title, String description,
                                                                         long earlyWithdrawalTime, int withdrawalPenalty,
                                                                         Long feePerByte) throws IOException {
        errorIf(title == null || title.isEmpty(), "Title cannot be empty");
        errorIf(description == null || description.isEmpty(), "Description cannot be empty");

        long baseFeePerByte = pwrj.getFeePerByte();
        if(feePerByte == null || feePerByte == 0) feePerByte = baseFeePerByte;
        errorIf(feePerByte < baseFeePerByte, "Fee per byte must be greater than or equal to " + baseFeePerByte);

        byte[] transaction = TransactionBuilder.getChangeEarlyWithdrawPenaltyProposalTransaction(
                feePerByte, address, title, description, earlyWithdrawalTime, withdrawalPenalty,
                pwrj.getNonceOfAddress(getAddress()), pwrj.getChainId());

        return getSignedTransaction(transaction);
    }

    public Response proposeChangeEarlyWithdrawPenalty(String title, String description,
                                                      long earlyWithdrawalTime, int withdrawalPenalty,
                                                      Long feePerByte) throws IOException {
        Response response = makeSurePublicKeyIsSet(feePerByte);
        if(response != null && !response.isSuccess()) return response;

        return pwrj.broadcastTransaction(getSignedChangeEarlyWithdrawPenaltyProposalTransaction(
                title, description, earlyWithdrawalTime, withdrawalPenalty, feePerByte));
    }

    public byte[] getSignedChangeFeePerByteProposalTransaction(String title, String description,
                                                               long newFeePerByte, Long feePerByte) throws IOException {
        errorIf(title == null || title.isEmpty(), "Title cannot be empty");
        errorIf(description == null || description.isEmpty(), "Description cannot be empty");

        long baseFeePerByte = pwrj.getFeePerByte();
        if(feePerByte == null || feePerByte == 0) feePerByte = baseFeePerByte;
        errorIf(feePerByte < baseFeePerByte, "Fee per byte must be greater than or equal to " + baseFeePerByte);

        byte[] transaction = TransactionBuilder.getChangeFeePerByteProposalTransaction(
                feePerByte, address, title, description, newFeePerByte,
                pwrj.getNonceOfAddress(getAddress()), pwrj.getChainId());

        return getSignedTransaction(transaction);
    }

    public Response proposeChangeFeePerByte(String title, String description,
                                            long newFeePerByte, Long feePerByte) throws IOException {
        Response response = makeSurePublicKeyIsSet(feePerByte);
        if(response != null && !response.isSuccess()) return response;

        return pwrj.broadcastTransaction(getSignedChangeFeePerByteProposalTransaction(
                title, description, newFeePerByte, feePerByte));
    }

    public byte[] getSignedChangeMaxBlockSizeProposalTransaction(String title, String description,
                                                                 int maxBlockSize, Long feePerByte) throws IOException {
        errorIf(title == null || title.isEmpty(), "Title cannot be empty");
        errorIf(description == null || description.isEmpty(), "Description cannot be empty");

        long baseFeePerByte = pwrj.getFeePerByte();
        if(feePerByte == null || feePerByte == 0) feePerByte = baseFeePerByte;
        errorIf(feePerByte < baseFeePerByte, "Fee per byte must be greater than or equal to " + baseFeePerByte);

        byte[] transaction = TransactionBuilder.getChangeMaxBlockSizeProposalTransaction(
                feePerByte, address, title, description, maxBlockSize,
                pwrj.getNonceOfAddress(getAddress()), pwrj.getChainId());

        return getSignedTransaction(transaction);
    }

    public Response proposeChangeMaxBlockSize(String title, String description,
                                              int maxBlockSize, Long feePerByte) throws IOException {
        Response response = makeSurePublicKeyIsSet(feePerByte);
        if(response != null && !response.isSuccess()) return response;

        return pwrj.broadcastTransaction(getSignedChangeMaxBlockSizeProposalTransaction(
                title, description, maxBlockSize, feePerByte));
    }

    public byte[] getSignedChangeMaxTxnSizeProposalTransaction(String title, String description,
                                                               int maxTxnSize, Long feePerByte) throws IOException {
        errorIf(title == null || title.isEmpty(), "Title cannot be empty");
        errorIf(description == null || description.isEmpty(), "Description cannot be empty");

        long baseFeePerByte = pwrj.getFeePerByte();
        if(feePerByte == null || feePerByte == 0) feePerByte = baseFeePerByte;
        errorIf(feePerByte < baseFeePerByte, "Fee per byte must be greater than or equal to " + baseFeePerByte);

        byte[] transaction = TransactionBuilder.getChangeMaxTxnSizeProposalTransaction(
                feePerByte, address, title, description, maxTxnSize,
                pwrj.getNonceOfAddress(getAddress()), pwrj.getChainId());

        return getSignedTransaction(transaction);
    }

    public Response proposeChangeMaxTxnSize(String title, String description,
                                            int maxTxnSize, Long feePerByte) throws IOException {
        Response response = makeSurePublicKeyIsSet(feePerByte);
        if(response != null && !response.isSuccess()) return response;

        return pwrj.broadcastTransaction(getSignedChangeMaxTxnSizeProposalTransaction(
                title, description, maxTxnSize, feePerByte));
    }

    public byte[] getSignedChangeOverallBurnPercentageProposalTransaction(String title, String description,
                                                                          int burnPercentage, Long feePerByte) throws IOException {
        errorIf(title == null || title.isEmpty(), "Title cannot be empty");
        errorIf(description == null || description.isEmpty(), "Description cannot be empty");
        errorIf(burnPercentage < 0 || burnPercentage > 100, "Burn percentage must be between 0 and 100");

        long baseFeePerByte = pwrj.getFeePerByte();
        if(feePerByte == null || feePerByte == 0) feePerByte = baseFeePerByte;
        errorIf(feePerByte < baseFeePerByte, "Fee per byte must be greater than or equal to " + baseFeePerByte);

        byte[] transaction = TransactionBuilder.getChangeOverallBurnPercentageProposalTransaction(
                feePerByte, address, title, description, burnPercentage,
                pwrj.getNonceOfAddress(getAddress()), pwrj.getChainId());

        return getSignedTransaction(transaction);
    }

    public Response proposeChangeOverallBurnPercentage(String title, String description,
                                                       int burnPercentage, Long feePerByte) throws IOException {
        Response response = makeSurePublicKeyIsSet(feePerByte);
        if(response != null && !response.isSuccess()) return response;

        return pwrj.broadcastTransaction(getSignedChangeOverallBurnPercentageProposalTransaction(
                title, description, burnPercentage, feePerByte));
    }

    public byte[] getSignedChangeRewardPerYearProposalTransaction(String title, String description,
                                                                  long rewardPerYear, Long feePerByte) throws IOException {
        errorIf(title == null || title.isEmpty(), "Title cannot be empty");
        errorIf(description == null || description.isEmpty(), "Description cannot be empty");

        long baseFeePerByte = pwrj.getFeePerByte();
        if(feePerByte == null || feePerByte == 0) feePerByte = baseFeePerByte;
        errorIf(feePerByte < baseFeePerByte, "Fee per byte must be greater than or equal to " + baseFeePerByte);

        byte[] transaction = TransactionBuilder.getChangeRewardPerYearProposalTransaction(
                feePerByte, address, title, description, rewardPerYear,
                pwrj.getNonceOfAddress(getAddress()), pwrj.getChainId());

        return getSignedTransaction(transaction);
    }

    public Response proposeChangeRewardPerYear(String title, String description,
                                               long rewardPerYear, Long feePerByte) throws IOException {
        Response response = makeSurePublicKeyIsSet(feePerByte);
        if(response != null && !response.isSuccess()) return response;

        return pwrj.broadcastTransaction(getSignedChangeRewardPerYearProposalTransaction(
                title, description, rewardPerYear, feePerByte));
    }

    public byte[] getSignedChangeValidatorCountLimitProposalTransaction(String title, String description,
                                                                        int validatorCountLimit, Long feePerByte) throws IOException {
        errorIf(title == null || title.isEmpty(), "Title cannot be empty");
        errorIf(description == null || description.isEmpty(), "Description cannot be empty");
        errorIf(validatorCountLimit <= 0, "Validator count limit must be positive");

        long baseFeePerByte = pwrj.getFeePerByte();
        if(feePerByte == null || feePerByte == 0) feePerByte = baseFeePerByte;
        errorIf(feePerByte < baseFeePerByte, "Fee per byte must be greater than or equal to " + baseFeePerByte);

        byte[] transaction = TransactionBuilder.getChangeValidatorCountLimitProposalTransaction(
                feePerByte, address, title, description, validatorCountLimit,
                pwrj.getNonceOfAddress(getAddress()), pwrj.getChainId());

        return getSignedTransaction(transaction);
    }

    public Response proposeChangeValidatorCountLimit(String title, String description,
                                                     int validatorCountLimit, Long feePerByte) throws IOException {
        Response response = makeSurePublicKeyIsSet(feePerByte);
        if(response != null && !response.isSuccess()) return response;

        return pwrj.broadcastTransaction(getSignedChangeValidatorCountLimitProposalTransaction(
                title, description, validatorCountLimit, feePerByte));
    }

    public byte[] getSignedChangeValidatorJoiningFeeProposalTransaction(String title, String description,
                                                                        long joiningFee, Long feePerByte) throws IOException {
        errorIf(title == null || title.isEmpty(), "Title cannot be empty");
        errorIf(description == null || description.isEmpty(), "Description cannot be empty");

        long baseFeePerByte = pwrj.getFeePerByte();
        if(feePerByte == null || feePerByte == 0) feePerByte = baseFeePerByte;
        errorIf(feePerByte < baseFeePerByte, "Fee per byte must be greater than or equal to " + baseFeePerByte);

        byte[] transaction = TransactionBuilder.getChangeValidatorJoiningFeeProposalTransaction(
                feePerByte, address, title, description, joiningFee,
                pwrj.getNonceOfAddress(getAddress()), pwrj.getChainId());

        return getSignedTransaction(transaction);
    }

    public Response proposeChangeValidatorJoiningFee(String title, String description,
                                                     long joiningFee, Long feePerByte) throws IOException {
        Response response = makeSurePublicKeyIsSet(feePerByte);
        if(response != null && !response.isSuccess()) return response;

        return pwrj.broadcastTransaction(getSignedChangeValidatorJoiningFeeProposalTransaction(
                title, description, joiningFee, feePerByte));
    }

    public byte[] getSignedChangeVidaIdClaimingFeeProposalTransaction(String title, String description,
                                                                      long vidaIdClaimingFee, Long feePerByte) throws IOException {
        errorIf(title == null || title.isEmpty(), "Title cannot be empty");
        errorIf(description == null || description.isEmpty(), "Description cannot be empty");

        long baseFeePerByte = pwrj.getFeePerByte();
        if(feePerByte == null || feePerByte == 0) feePerByte = baseFeePerByte;
        errorIf(feePerByte < baseFeePerByte, "Fee per byte must be greater than or equal to " + baseFeePerByte);

        byte[] transaction = TransactionBuilder.getChangeVidaIdClaimingFeeProposalTransaction(
                feePerByte, address, title, description, vidaIdClaimingFee,
                pwrj.getNonceOfAddress(getAddress()), pwrj.getChainId());

        return getSignedTransaction(transaction);
    }

    public Response proposeChangeVidaIdClaimingFee(String title, String description,
                                                   long vidaIdClaimingFee, Long feePerByte) throws IOException {
        Response response = makeSurePublicKeyIsSet(feePerByte);
        if(response != null && !response.isSuccess()) return response;

        return pwrj.broadcastTransaction(getSignedChangeVidaIdClaimingFeeProposalTransaction(
                title, description, vidaIdClaimingFee, feePerByte));
    }

    public byte[] getSignedChangeVmOwnerTxnFeeShareProposalTransaction(String title, String description,
                                                                       int vmOwnerTxnFeeShare, Long feePerByte) throws IOException {
        errorIf(title == null || title.isEmpty(), "Title cannot be empty");
        errorIf(description == null || description.isEmpty(), "Description cannot be empty");
        errorIf(vmOwnerTxnFeeShare < 0 || vmOwnerTxnFeeShare > 10000, "VM owner txn fee share must be between 0 and 10000");

        long baseFeePerByte = pwrj.getFeePerByte();
        if(feePerByte == null || feePerByte == 0) feePerByte = baseFeePerByte;
        errorIf(feePerByte < baseFeePerByte, "Fee per byte must be greater than or equal to " + baseFeePerByte);

        byte[] transaction = TransactionBuilder.getChangeVmOwnerTxnFeeShareProposalTransaction(
                feePerByte, address, title, description, vmOwnerTxnFeeShare,
                pwrj.getNonceOfAddress(getAddress()), pwrj.getChainId());

        return getSignedTransaction(transaction);
    }

    public Response proposeChangeVmOwnerTxnFeeShare(String title, String description,
                                                    int vmOwnerTxnFeeShare, Long feePerByte) throws IOException {
        Response response = makeSurePublicKeyIsSet(feePerByte);
        if(response != null && !response.isSuccess()) return response;

        return pwrj.broadcastTransaction(getSignedChangeVmOwnerTxnFeeShareProposalTransaction(
                title, description, vmOwnerTxnFeeShare, feePerByte));
    }

    public byte[] getSignedOtherProposalTransaction(String title, String description, Long feePerByte) throws IOException {
        errorIf(title == null || title.isEmpty(), "Title cannot be empty");
        errorIf(description == null || description.isEmpty(), "Description cannot be empty");

        long baseFeePerByte = pwrj.getFeePerByte();
        if(feePerByte == null || feePerByte == 0) feePerByte = baseFeePerByte;
        errorIf(feePerByte < baseFeePerByte, "Fee per byte must be greater than or equal to " + baseFeePerByte);

        byte[] transaction = TransactionBuilder.getOtherProposalTransaction(
                feePerByte, address, title, description,
                pwrj.getNonceOfAddress(getAddress()), pwrj.getChainId());

        return getSignedTransaction(transaction);
    }

    public Response proposeOther(String title, String description, Long feePerByte) throws IOException {
        Response response = makeSurePublicKeyIsSet(feePerByte);
        if(response != null && !response.isSuccess()) return response;

        return pwrj.broadcastTransaction(getSignedOtherProposalTransaction(
                title, description, feePerByte));
    }

    public byte[] getSignedVoteOnProposalTransaction(byte[] proposalHash, byte vote, Long feePerByte) throws IOException {
        errorIf(proposalHash == null || proposalHash.length != 32, "Proposal hash must be 32 bytes");
        errorIf(vote != 0 && vote != 1, "Vote must be 0 (against) or 1 (in favor)");

        long baseFeePerByte = pwrj.getFeePerByte();
        if(feePerByte == null || feePerByte == 0) feePerByte = baseFeePerByte;
        errorIf(feePerByte < baseFeePerByte, "Fee per byte must be greater than or equal to " + baseFeePerByte);

        byte[] transaction = TransactionBuilder.getVoteOnProposalTransaction(
                feePerByte, address, proposalHash, vote,
                pwrj.getNonceOfAddress(getAddress()), pwrj.getChainId());

        return getSignedTransaction(transaction);
    }

    public Response voteOnProposal(byte[] proposalHash, byte vote, Long feePerByte) throws IOException {
        Response response = makeSurePublicKeyIsSet(feePerByte);
        if(response != null && !response.isSuccess()) return response;

        return pwrj.broadcastTransaction(getSignedVoteOnProposalTransaction(
                proposalHash, vote, feePerByte));
    }

    // Guardian Transactions

    public byte[] getSignedGuardianApprovalTransaction(List<byte[]> wrappedTxns, Long feePerByte) throws IOException {
        errorIf(wrappedTxns == null || wrappedTxns.isEmpty(), "No transactions provided for guardian approval");

        long baseFeePerByte = pwrj.getFeePerByte();
        if(feePerByte == null || feePerByte == 0) feePerByte = baseFeePerByte;
        errorIf(feePerByte < baseFeePerByte, "Fee per byte must be greater than or equal to " + baseFeePerByte);

        byte[] transaction = TransactionBuilder.getGuardianApprovalTransaction(
                feePerByte, address, wrappedTxns,
                pwrj.getNonceOfAddress(getAddress()), pwrj.getChainId());

        return getSignedTransaction(transaction);
    }

    public Response approveAsGuardian(List<byte[]> wrappedTxns, Long feePerByte) throws IOException {
        Response response = makeSurePublicKeyIsSet(feePerByte);
        if(response != null && !response.isSuccess()) return response;

        return pwrj.broadcastTransaction(getSignedGuardianApprovalTransaction(
                wrappedTxns, feePerByte));
    }

    public byte[] getSignedRemoveGuardianTransaction(Long feePerByte) throws IOException {
        long baseFeePerByte = pwrj.getFeePerByte();
        if(feePerByte == null || feePerByte == 0) feePerByte = baseFeePerByte;
        errorIf(feePerByte < baseFeePerByte, "Fee per byte must be greater than or equal to " + baseFeePerByte);

        byte[] transaction = TransactionBuilder.getRemoveGuardianTransaction(
                feePerByte, address,
                pwrj.getNonceOfAddress(getAddress()), pwrj.getChainId());

        return getSignedTransaction(transaction);
    }

    public Response removeGuardian(Long feePerByte) throws IOException {
        Response response = makeSurePublicKeyIsSet(feePerByte);
        if(response != null && !response.isSuccess()) return response;

        return pwrj.broadcastTransaction(getSignedRemoveGuardianTransaction(feePerByte));
    }

    public byte[] getSignedSetGuardianTransaction(long expiryDate, byte[] guardianAddress, Long feePerByte) throws IOException {
        errorIf(guardianAddress == null || guardianAddress.length != 20, "Guardian address must be 20 bytes");
        errorIf(expiryDate <= System.currentTimeMillis(), "Expiry date must be in the future");

        long baseFeePerByte = pwrj.getFeePerByte();
        if(feePerByte == null || feePerByte == 0) feePerByte = baseFeePerByte;
        errorIf(feePerByte < baseFeePerByte, "Fee per byte must be greater than or equal to " + baseFeePerByte);

        byte[] transaction = TransactionBuilder.getSetGuardianTransaction(
                feePerByte, address, expiryDate, guardianAddress,
                pwrj.getNonceOfAddress(getAddress()), pwrj.getChainId());

        return getSignedTransaction(transaction);
    }

    public Response setGuardian(long expiryDate, byte[] guardianAddress, Long feePerByte) throws IOException {
        Response response = makeSurePublicKeyIsSet(feePerByte);
        if(response != null && !response.isSuccess()) return response;

        return pwrj.broadcastTransaction(getSignedSetGuardianTransaction(
                expiryDate, guardianAddress, feePerByte));
    }

    // Staking Transactions

    public byte[] getSignedMoveStakeTransaction(BigInteger sharesAmount, byte[] fromValidator,
                                                byte[] toValidator, Long feePerByte) throws IOException {
        errorIf(sharesAmount == null || sharesAmount.compareTo(BigInteger.ZERO) <= 0, "Shares amount must be positive");
        errorIf(fromValidator == null || fromValidator.length != 20, "From validator address must be 20 bytes");
        errorIf(toValidator == null || toValidator.length != 20, "To validator address must be 20 bytes");

        long baseFeePerByte = pwrj.getFeePerByte();
        if(feePerByte == null || feePerByte == 0) feePerByte = baseFeePerByte;
        errorIf(feePerByte < baseFeePerByte, "Fee per byte must be greater than or equal to " + baseFeePerByte);

        byte[] transaction = TransactionBuilder.getMoveStakeTxnTransaction(
                feePerByte, address, sharesAmount, fromValidator, toValidator,
                pwrj.getNonceOfAddress(getAddress()), pwrj.getChainId());

        return getSignedTransaction(transaction);
    }

    public Response moveStake(BigInteger sharesAmount, byte[] fromValidator,
                              byte[] toValidator, Long feePerByte) throws IOException {
        Response response = makeSurePublicKeyIsSet(feePerByte);
        if(response != null && !response.isSuccess()) return response;

        return pwrj.broadcastTransaction(getSignedMoveStakeTransaction(
                sharesAmount, fromValidator, toValidator, feePerByte));
    }

    public byte[] getSignedRemoveValidatorTransaction(byte[] validatorAddress, Long feePerByte) throws IOException {
        errorIf(validatorAddress == null || validatorAddress.length != 20, "Validator address must be 20 bytes");

        long baseFeePerByte = pwrj.getFeePerByte();
        if(feePerByte == null || feePerByte == 0) feePerByte = baseFeePerByte;
        errorIf(feePerByte < baseFeePerByte, "Fee per byte must be greater than or equal to " + baseFeePerByte);

        byte[] transaction = TransactionBuilder.getRemoveValidatorTransaction(
                feePerByte, address, validatorAddress,
                pwrj.getNonceOfAddress(getAddress()), pwrj.getChainId());

        return getSignedTransaction(transaction);
    }

    public Response removeValidator(byte[] validatorAddress, Long feePerByte) throws IOException {
        Response response = makeSurePublicKeyIsSet(feePerByte);
        if(response != null && !response.isSuccess()) return response;

        return pwrj.broadcastTransaction(getSignedRemoveValidatorTransaction(
                validatorAddress, feePerByte));
    }

    public byte[] getSignedWithdrawTransaction(BigInteger sharesAmount, byte[] validator, Long feePerByte) throws IOException {
        errorIf(sharesAmount == null || sharesAmount.compareTo(BigInteger.ZERO) <= 0, "Shares amount must be positive");
        errorIf(validator == null || validator.length != 20, "Validator address must be 20 bytes");

        long baseFeePerByte = pwrj.getFeePerByte();
        if(feePerByte == null || feePerByte == 0) feePerByte = baseFeePerByte;
        errorIf(feePerByte < baseFeePerByte, "Fee per byte must be greater than or equal to " + baseFeePerByte);

        byte[] transaction = TransactionBuilder.getWithdrawTransaction(
                feePerByte, address, sharesAmount, validator,
                pwrj.getNonceOfAddress(getAddress()), pwrj.getChainId());

        return getSignedTransaction(transaction);
    }

    public Response withdraw(BigInteger sharesAmount, byte[] validator, Long feePerByte) throws IOException {
        Response response = makeSurePublicKeyIsSet(feePerByte);
        if(response != null && !response.isSuccess()) return response;

        return pwrj.broadcastTransaction(getSignedWithdrawTransaction(
                sharesAmount, validator, feePerByte));
    }

    // VIDA Transactions

    public byte[] getSignedSetConduitModeTransaction(long vidaId, byte mode, int conduitThreshold,
                                                     Set<byte[]> conduits, Map<ByteArrayWrapper, Long> conduitsWithVotingPower, Long feePerByte) throws IOException {
        errorIf(conduitThreshold < 0, "Conduit threshold must be non-negative");
        if(mode == 1 || mode == 2) { // COUNT_BASED or STAKE_BASED mode
            errorIf((conduits == null || conduits.isEmpty()) && (conduitsWithVotingPower == null || conduitsWithVotingPower.isEmpty()), "Conduit addresses must be provided for this mode");
        }

        long baseFeePerByte = pwrj.getFeePerByte();
        if(feePerByte == null || feePerByte == 0) feePerByte = baseFeePerByte;
        errorIf(feePerByte < baseFeePerByte, "Fee per byte must be greater than or equal to " + baseFeePerByte);

        byte[] transaction = TransactionBuilder.getSetConduitModeTransaction(
                feePerByte, address, vidaId, mode, conduitThreshold, conduits, conduitsWithVotingPower,
                pwrj.getNonceOfAddress(getAddress()), pwrj.getChainId());

        return getSignedTransaction(transaction);
    }

    public Response setConduitMode(long vidaId, byte mode, int conduitThreshold,
                                   Set<byte[]> conduits, Map<ByteArrayWrapper, Long> conduitsWithVotingPower, Long feePerByte) throws IOException {
        Response response = makeSurePublicKeyIsSet(feePerByte);
        if(response != null && !response.isSuccess()) return response;

        return pwrj.broadcastTransaction(getSignedSetConduitModeTransaction(
                vidaId, mode, conduitThreshold, conduits, conduitsWithVotingPower, feePerByte));
    }

    public byte[] getSignedSetConduitModeWithVidaBasedTransaction(long vidaId, byte mode, int conduitThreshold,
                                                                  List<byte[]> conduits, List<Long> stakingPowers,
                                                                  Long feePerByte) throws IOException {
        errorIf(conduitThreshold < 0, "Conduit threshold must be non-negative");
        errorIf(mode != 3, "This method is only for VIDA_BASED mode (3)");
        errorIf(conduits == null || conduits.isEmpty(), "Conduit addresses must be provided");
        errorIf(stakingPowers == null || stakingPowers.isEmpty(), "Staking powers must be provided");
        errorIf(conduits.size() != stakingPowers.size(), "Conduits and staking powers lists must be the same size");

        long baseFeePerByte = pwrj.getFeePerByte();
        if(feePerByte == null || feePerByte == 0) feePerByte = baseFeePerByte;
        errorIf(feePerByte < baseFeePerByte, "Fee per byte must be greater than or equal to " + baseFeePerByte);

        byte[] transaction = TransactionBuilder.getSetConduitModeWithVidaBasedTransaction(
                feePerByte, address, vidaId, mode, conduitThreshold, conduits, stakingPowers,
                pwrj.getNonceOfAddress(getAddress()), pwrj.getChainId());

        return getSignedTransaction(transaction);
    }

    public Response setConduitModeWithVidaBased(long vidaId, byte mode, int conduitThreshold,
                                                List<byte[]> conduits, List<Long> stakingPowers,
                                                Long feePerByte) throws IOException {
        Response response = makeSurePublicKeyIsSet(feePerByte);
        if(response != null && !response.isSuccess()) return response;

        return pwrj.broadcastTransaction(getSignedSetConduitModeWithVidaBasedTransaction(
                vidaId, mode, conduitThreshold, conduits, stakingPowers, feePerByte));
    }

    public byte[] getSignedClaimVidaIdTransaction(long vidaId, Long feePerByte) throws IOException {
        errorIf(vidaId == 0, "VIDA ID cannot be zero");

        long baseFeePerByte = pwrj.getFeePerByte();
        if(feePerByte == null || feePerByte == 0) feePerByte = baseFeePerByte;
        errorIf(feePerByte < baseFeePerByte, "Fee per byte must be greater than or equal to " + baseFeePerByte);

        byte[] transaction = TransactionBuilder.getClaimVidaIdTransaction(
                feePerByte, address, vidaId,
                pwrj.getNonceOfAddress(getAddress()), pwrj.getChainId());

        return getSignedTransaction(transaction);
    }

    public Response claimVidaId(long vidaId, Long feePerByte) throws IOException {
        Response response = makeSurePublicKeyIsSet(feePerByte);
        if(response != null && !response.isSuccess()) return response;

        return pwrj.broadcastTransaction(getSignedClaimVidaIdTransaction(
                vidaId, feePerByte));
    }

    public byte[] getSignedConduitApprovalTransaction(long vidaId, List<byte[]> wrappedTxns, Long feePerByte) throws IOException {
        errorIf(vidaId == 0, "VIDA ID cannot be zero");
        errorIf(wrappedTxns == null || wrappedTxns.isEmpty(), "No transactions provided for conduit approval");

        long baseFeePerByte = pwrj.getFeePerByte();
        if(feePerByte == null || feePerByte == 0) feePerByte = baseFeePerByte;
        errorIf(feePerByte < baseFeePerByte, "Fee per byte must be greater than or equal to " + baseFeePerByte);

        byte[] transaction = TransactionBuilder.getConduitApprovalTransaction(
                feePerByte, address, vidaId, wrappedTxns,
                pwrj.getNonceOfAddress(getAddress()), pwrj.getChainId());

        return getSignedTransaction(transaction);
    }

    public Response approveAsConduit(long vidaId, List<byte[]> wrappedTxns, Long feePerByte) throws IOException {
        Response response = makeSurePublicKeyIsSet(feePerByte);
        if(response != null && !response.isSuccess()) return response;

        return pwrj.broadcastTransaction(getSignedConduitApprovalTransaction(
                vidaId, wrappedTxns, feePerByte));
    }

    public byte[] getSignedPayableVidaDataTransaction(long vidaId, byte[] data, long value, Long feePerByte) throws IOException {
        errorIf(vidaId == 0, "VIDA ID cannot be zero");
        errorIf(data == null || data.length == 0, "Data cannot be empty");
        errorIf(value < 0, "Value cannot be negative");

        long baseFeePerByte = pwrj.getFeePerByte();
        if(feePerByte == null || feePerByte == 0) feePerByte = baseFeePerByte;
        errorIf(feePerByte < baseFeePerByte, "Fee per byte must be greater than or equal to " + baseFeePerByte);

        byte[] transaction = TransactionBuilder.getPayableVidaDataTransaction(
                feePerByte, address, vidaId, data, value,
                pwrj.getNonceOfAddress(getAddress()), pwrj.getChainId());

        return getSignedTransaction(transaction);
    }

    public Response submitPayableVidaData(long vidaId, byte[] data, long value, Long feePerByte) throws IOException {
        Response response = makeSurePublicKeyIsSet(feePerByte);
        if(response != null && !response.isSuccess()) return response;

        return pwrj.broadcastTransaction(getSignedPayableVidaDataTransaction(
                vidaId, data, value, feePerByte));
    }

    public byte[] getSignedRemoveConduitsTransaction(long vidaId, List<byte[]> conduits, Long feePerByte) throws IOException {
        errorIf(vidaId == 0, "VIDA ID cannot be zero");
        errorIf(conduits == null || conduits.isEmpty(), "No conduits provided for removal");
        for(byte[] conduit : conduits) {
            errorIf(conduit == null || conduit.length != 20, "Conduit address must be 20 bytes");
        }

        long baseFeePerByte = pwrj.getFeePerByte();
        if(feePerByte == null || feePerByte == 0) feePerByte = baseFeePerByte;
        errorIf(feePerByte < baseFeePerByte, "Fee per byte must be greater than or equal to " + baseFeePerByte);

        byte[] transaction = TransactionBuilder.getRemoveConduitsTransaction(
                feePerByte, address, vidaId, conduits,
                pwrj.getNonceOfAddress(getAddress()), pwrj.getChainId());

        return getSignedTransaction(transaction);
    }

    public Response removeConduits(long vidaId, List<byte[]> conduits, Long feePerByte) throws IOException {
        Response response = makeSurePublicKeyIsSet(feePerByte);
        if(response != null && !response.isSuccess()) return response;

        return pwrj.broadcastTransaction(getSignedRemoveConduitsTransaction(
                vidaId, conduits, feePerByte));
    }

    public byte[] getSignedAddVidaAllowedSendersTransaction(long vidaId, Set<byte[]> allowedSenders, Long feePerByte) throws IOException {
        errorIf(vidaId == 0, "VIDA ID cannot be zero");
        errorIf(allowedSenders == null || allowedSenders.isEmpty(), "No allowed senders provided");
        for(byte[] sender : allowedSenders) {
            errorIf(sender == null || sender.length != 20, "Sender address must be 20 bytes");
        }

        long baseFeePerByte = pwrj.getFeePerByte();
        if(feePerByte == null || feePerByte == 0) feePerByte = baseFeePerByte;
        errorIf(feePerByte < baseFeePerByte, "Fee per byte must be greater than or equal to " + baseFeePerByte);

        byte[] transaction = TransactionBuilder.getAddVidaAllowedSendersTransaction(
                feePerByte, address, vidaId, allowedSenders,
                pwrj.getNonceOfAddress(getAddress()), pwrj.getChainId());

        return getSignedTransaction(transaction);
    }

    public Response addVidaAllowedSenders(long vidaId, Set<byte[]> allowedSenders, Long feePerByte) throws IOException {
        Response response = makeSurePublicKeyIsSet(feePerByte);
        if(response != null && !response.isSuccess()) return response;

        return pwrj.broadcastTransaction(getSignedAddVidaAllowedSendersTransaction(
                vidaId, allowedSenders, feePerByte));
    }

    public byte[] getSignedAddVidaSponsoredAddressesTransaction(long vidaId, Set<byte[]> sponsoredAddresses, Long feePerByte) throws IOException {
        errorIf(vidaId == 0, "VIDA ID cannot be zero");
        errorIf(sponsoredAddresses == null || sponsoredAddresses.isEmpty(), "No sponsored addresses provided");
        for(byte[] address : sponsoredAddresses) {
            errorIf(address == null || address.length != 20, "Sponsored address must be 20 bytes");
        }

        long baseFeePerByte = pwrj.getFeePerByte();
        if(feePerByte == null || feePerByte == 0) feePerByte = baseFeePerByte;
        errorIf(feePerByte < baseFeePerByte, "Fee per byte must be greater than or equal to " + baseFeePerByte);

        byte[] transaction = TransactionBuilder.getAddVidaSponsoredAddressesTransaction(
                feePerByte, address, vidaId, sponsoredAddresses,
                pwrj.getNonceOfAddress(getAddress()), pwrj.getChainId());

        return getSignedTransaction(transaction);
    }

    public Response addVidaSponsoredAddresses(long vidaId, Set<byte[]> sponsoredAddresses, Long feePerByte) throws IOException {
        Response response = makeSurePublicKeyIsSet(feePerByte);
        if(response != null && !response.isSuccess()) return response;

        return pwrj.broadcastTransaction(getSignedAddVidaSponsoredAddressesTransaction(
                vidaId, sponsoredAddresses, feePerByte));
    }

    public byte[] getSignedRemoveSponsoredAddressesTransaction(long vidaId, Set<byte[]> sponsoredAddresses, Long feePerByte) throws IOException {
        errorIf(vidaId == 0, "VIDA ID cannot be zero");
        errorIf(sponsoredAddresses == null || sponsoredAddresses.isEmpty(), "No sponsored addresses provided for removal");
        for(byte[] address : sponsoredAddresses) {
            errorIf(address == null || address.length != 20, "Sponsored address must be 20 bytes");
        }

        long baseFeePerByte = pwrj.getFeePerByte();
        if(feePerByte == null || feePerByte == 0) feePerByte = baseFeePerByte;
        errorIf(feePerByte < baseFeePerByte, "Fee per byte must be greater than or equal to " + baseFeePerByte);

        byte[] transaction = TransactionBuilder.getRemoveSponsoredAddressesTransaction(
                feePerByte, address, vidaId, sponsoredAddresses,
                pwrj.getNonceOfAddress(getAddress()), pwrj.getChainId());

        return getSignedTransaction(transaction);
    }

    public Response removeVidaSponsoredAddresses(long vidaId, Set<byte[]> sponsoredAddresses, Long feePerByte) throws IOException {
        Response response = makeSurePublicKeyIsSet(feePerByte);
        if(response != null && !response.isSuccess()) return response;

        return pwrj.broadcastTransaction(getSignedRemoveSponsoredAddressesTransaction(
                vidaId, sponsoredAddresses, feePerByte));
    }

    public byte[] getSignedRemoveVidaAllowedSendersTransaction(long vidaId, Set<byte[]> allowedSenders, Long feePerByte) throws IOException {
        errorIf(vidaId == 0, "VIDA ID cannot be zero");
        errorIf(allowedSenders == null || allowedSenders.isEmpty(), "No allowed senders provided for removal");
        for(byte[] sender : allowedSenders) {
            errorIf(sender == null || sender.length != 20, "Sender address must be 20 bytes");
        }

        long baseFeePerByte = pwrj.getFeePerByte();
        if(feePerByte == null || feePerByte == 0) feePerByte = baseFeePerByte;
        errorIf(feePerByte < baseFeePerByte, "Fee per byte must be greater than or equal to " + baseFeePerByte);

        byte[] transaction = TransactionBuilder.getRemoveVidaAllowedSendersTransaction(
                feePerByte, address, vidaId, allowedSenders,
                pwrj.getNonceOfAddress(getAddress()), pwrj.getChainId());

        return getSignedTransaction(transaction);
    }

    public Response removeVidaAllowedSenders(long vidaId, Set<byte[]> allowedSenders, Long feePerByte) throws IOException {
        Response response = makeSurePublicKeyIsSet(feePerByte);
        if(response != null && !response.isSuccess()) return response;

        return pwrj.broadcastTransaction(getSignedRemoveVidaAllowedSendersTransaction(
                vidaId, allowedSenders, feePerByte));
    }

    public byte[] getSignedSetVidaPrivateStateTransaction(long vidaId, boolean privateState, Long feePerByte) throws IOException {
        errorIf(vidaId == 0, "VIDA ID cannot be zero");

        long baseFeePerByte = pwrj.getFeePerByte();
        if(feePerByte == null || feePerByte == 0) feePerByte = baseFeePerByte;
        errorIf(feePerByte < baseFeePerByte, "Fee per byte must be greater than or equal to " + baseFeePerByte);

        byte[] transaction = TransactionBuilder.getSetVidaPrivateStateTransaction(
                feePerByte, address, vidaId, privateState,
                pwrj.getNonceOfAddress(getAddress()), pwrj.getChainId());

        return getSignedTransaction(transaction);
    }

    public Response setVidaPrivateState(long vidaId, boolean privateState, Long feePerByte) throws IOException {
        Response response = makeSurePublicKeyIsSet(feePerByte);
        if(response != null && !response.isSuccess()) return response;

        return pwrj.broadcastTransaction(getSignedSetVidaPrivateStateTransaction(
                vidaId, privateState, feePerByte));
    }

    public byte[] getSignedSetVidaToAbsolutePublicTransaction(long vidaId, Long feePerByte) throws IOException {
        errorIf(vidaId == 0, "VIDA ID cannot be zero");

        long baseFeePerByte = pwrj.getFeePerByte();
        if(feePerByte == null || feePerByte == 0) feePerByte = baseFeePerByte;
        errorIf(feePerByte < baseFeePerByte, "Fee per byte must be greater than or equal to " + baseFeePerByte);

        byte[] transaction = TransactionBuilder.getSetVidaToAbsolutePublicTransaction(
                feePerByte, address, vidaId,
                pwrj.getNonceOfAddress(getAddress()), pwrj.getChainId());

        return getSignedTransaction(transaction);
    }

    public Response setVidaToAbsolutePublic(long vidaId, Long feePerByte) throws IOException {
        Response response = makeSurePublicKeyIsSet(feePerByte);
        if(response != null && !response.isSuccess()) return response;

        return pwrj.broadcastTransaction(getSignedSetVidaToAbsolutePublicTransaction(
                vidaId, feePerByte));
    }

    public byte[] getSignedSetPWRTransferRightsTransaction(long vidaId, boolean ownerCanTransferPWR, long feePerByte) throws IOException {
        errorIf(vidaId == 0, "VIDA ID cannot be zero");

        byte[] transaction = TransactionBuilder.getSetPWRTransferRightsTransaction(feePerByte, address, vidaId, ownerCanTransferPWR,
                pwrj.getNonceOfAddress(getAddress()), pwrj.getChainId());

        return getSignedTransaction(transaction);
    }

    public Response setPWRTransferRights(long vidaId, boolean ownerCanTransferPWR, long feePerByte) throws IOException {
        Response response = makeSurePublicKeyIsSet(feePerByte);
        if(response != null && !response.isSuccess()) return response;

        return pwrj.broadcastTransaction(getSignedSetPWRTransferRightsTransaction(vidaId, ownerCanTransferPWR, feePerByte));
    }

    public byte[] getSignedTransferPWRFromVidaTransaction(long vidaId, byte[] receiver, long amount, long feePerByte) throws IOException {
        errorIf(vidaId == 0, "VIDA ID cannot be zero");
        errorIf(receiver == null || receiver.length != 20, "Receiver address must be 20 bytes");
        errorIf(amount <= 0, "Amount must be positive");

        byte[] transaction = TransactionBuilder.getTransferPWRFromVidaTransaction(
                feePerByte, address, vidaId, receiver, amount,
                pwrj.getNonceOfAddress(getAddress()), pwrj.getChainId());

        return getSignedTransaction(transaction);
    }

    public Response transferPWRFromVida(long vidaId, byte[] receiver, long amount, long feePerByte) throws IOException {
        Response response = makeSurePublicKeyIsSet(feePerByte);
        if(response != null && !response.isSuccess()) return response;

        return pwrj.broadcastTransaction(getSignedTransferPWRFromVidaTransaction(vidaId, receiver, amount, feePerByte));
    }

    public static void main(String[] args) {
        byte[] message = "Hello".getBytes(StandardCharsets.UTF_8);

        byte[] signature512 = Hex.decode("3900000000000000000000000000000000000000000000000000000000000000000000000000000000596739dd57065566bf21f9832e78345e038f7979530e0522dd16c422891d098cc2c1185a840678b4aa6af9a684c9f91a3f06edb375725132d310f16e71776f84cb1a93f296c7615886d7252a27f3fce210b7ed2d63f74d18903afc0652866455bf440ee3eec23dd04ce75983d448d0b2fb60932ebb1c97998ad0b6c38bf38de80cdd7bdb68cec14b342641cd45eaddcfc7e9c741d9c32736276c030c353a4d6d7cdf59669b6086330696ebb38997f9ad1f024c56c9d521f6d3c2d15ff56b8570a0a2f4ae248b98b5aaec2a316dcb3edd1abef19eec54260afd4f19c54a920649714af0ef3af47c23f7048f13dc63ab643dbb3f44c6e081d8ba29c5d1a088fb158e53bceddde708ca9ee1601ab416ddd7e967cfafdfdb0025030ed92e6ece3165dec261ede2e847d81e9266a26e7d344a88a0b1c91ccbff1993e7584788c97b10779a0045a1db4659818f20adc1d97794a1f56588911fcd8d1435cdc62254683ae12fc32b702849bfce2e09a61db78a3032a408c324db05add78fb5a45b7c8d796ced1f6d0e6be04e6f49ffe7c1b54d720adcac399198e540700f46e566b1c60d8d9a042187de8e72b73b4726d19a2fda5765a7beb1aaa1564de9162ec29080c2d998b1b47ab11669c0a0c29b394ba8994b26c8d46feda5a27bd335323e9af0491b3b7af2e4e59c542345b9cc08d5cd7ff6f4ddaa35d6a444d7180e1093e0f5f1c6b55d91711bc357a72b548b4e0604c92168e3198f3a6dc193941b290458d7b6b17e76e8d6f135cd6d9c9ab2b49c146394cdcfa12f4f14a3b26e3195c0b3f2b6d1158521de3a2e018c70a9aa36b5fe7ffebbec99acf9b7ecbaa6a5d173430d9f90c7cbaca64861a0");
        byte[] publicKey512 = Hex.decode("09752027b748482e60bc36eaae6abe27282ac6bf0b3c0255d31afea44f9a8a8be5d85bd58a869cb95e2ff83d2dfd09ac850a2ff8499695aaf924f225017025527b89cf73df29b907c6da9287500a6808f0812e03b3a0c2046962a9b997a68df966cea1135e7b29e6ebfd8df0e2a4ca1b95469d957106ea274c76a6b0e8c6628fc002485c6f322ad0ec90ef4a0576ad6d9aec543b93fd98172247957a5ddf78c2df5774ccf91498ad9c63b892c206495c0a8eb3cb552e2f5fe58851a36e5d9651bf151ac2e282507b1271a076a00e21d38e2c0f1ac6d66606e36b52b102a22a5e4d23a2584059d6c953f4ff4380af9c3cfe5744459a2c3c297bb280ad5e7c76c18252e7754a16c29793cc9a7aaaf071a4c9fb78b5ced57ce1c67c54b751b026a82d042a89368f0b4b99403aeb5b9b253a8a1443574a7d5aef17446b429ea8527878d1d818dea841696e417ae923e29e927069566fc66d08fe7aa6e5b25f00df83a5d4c01a4b4bbf991152f9c42a2d486915bb1bb429c18ab2996c3a8b291af93c09a07e804905cf2fad27eca5e0082e2538fc6812bc9ed504a638e9ca68b54b158f0b2905ce58e1220af58784689638107a971b210a7ae8e05573abcaacd0f421b3278e76a6105960afc1b2d7369fd67f99b6542fcc864c05f1aa070d21bdc0b9f092a3ef3a05acf8a766693d2ef856bc04e966b646e834c9a36fe6625335883f917414f3e84c65bb0e2e033bd8c35f586e24910b3110ceb665b44cf589e1afda8a27e48b1a215bff45c36555f98842ad8d8dfabeb149639068c53184dc5c91a2890a744cc26560cd89c8a1869170786ba904c8628cace2c609210940e62b95395fdfa6d47e5028ab954b66bc9377a096944dc7d17a94d82710ff84daa06a84a5706a2605a1383d8823fb820411860905d6614cbbfaa4b9ad9cdd0ceed944eaaa3f4ac9d668a4e53a7dcaba444c17086c2161d2755a0565487b97bfc6245668905700d4af9da8e7a8580f68a284b239f1ebc972443119df6cf22c12e106a85e6d693a0dc0ffd86d11d6cbaa4eced6a8a95899949f84bc184ae14350d37d069a8703540220ed162647a5191d0ec1b1c34f1534d03c11face4f8a625a001286d410697858f258a28d32b8953249a1b7968fdaeb988731419ce00482de9369d4f6ee263c9b60fd31efceff5c6aac61886751b1e20794a397132f122cb293d757396486a205ae96bf2280f92f518206b92d511358d7988dbc8484503f8c8c9065589043");

        System.out.println("Verifying 512 with 512 signature verification: " + Falcon.verify512(message, signature512, publicKey512));
    }
}
