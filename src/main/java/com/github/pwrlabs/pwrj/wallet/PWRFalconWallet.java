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
        byte[] message = Hex.decode("68656c6c6f20776f726c64");

        byte[] signature512 = Hex.decode("399562badca08f2f81d204d6097562cc2b209bfe695baa0c3b9fbbcf51b97cc9eb1470a281e694ce7696768d2cda98535d6914938dda3a538a2c175fa1aa60f41f18f1b9693f784a834e88d7a4084cc65985bfbc686d85d2f64d6d73144f7bde65bba9013bebb453fbcf7a1b9ee177aae55bab7e733fd10cf748f3ec8a9cb5fd77a0ecdf7a55b28753ccc5864a9a9802e9b61976e62d49576f10b37b53b3a3c243af63e2e98b7b22ba6875a42f37317d1edab9b192badb1775ec4da3b1ec0300ad28cec3254c4b8994f69635455172dfb5f9aa39208a87db17b4caa2647f705c55171bdecf36ea465e6d31897c985a72324019171f475b2f9d99335096e156ffccd3e6bbeddcb31cafdc87d69c3596760dc4a54fc7d2d1115e5cb39b44976317197d8545b11f8fed8dabe469f17db84df7fc87e2a7196375d8c3a9b5b63bf88e30cc3f49676be4686fa31904dfaf6ef20d1c36b27acd8be50d41aa2bac50ce3dd341249aa6e6289d3ad0865220de6ad69b941280ab11ecc489874b09c9815c5819b313b5ead9a6db64bb4d23ad273a278ac3c92845e660aa466a2b54452ed96f66c8213d6f906588fdc4f67f5cb62bab04f329188e0e910375394dcafd248d4ecc920f20ee4b5b5731bc7378b2ee6de5187192d320211d5ecd588aa371667e89666e9e9c0e9b95c2faeb09ced9f5c82455491a09d57a19e51a369533b92436ad8ffb1ff38fdccea4f9451f58da14aeb4571b31bd7ab28364ae415ebbea62944ca97c6fea0c657738ccc902bac563abf458864c54a8c495a07e34b0759b36719c5e2b030a23399af35fd696615ec9bc38dbcf9a85d517dc4b9097d29083a0f490a59099bc0e07e04c76b517dd2748f2e70276b773d2791f736ce8b34bec6126b61528eba52666fdd2b6792dc4923bca");
        byte[] publicKey512 = Hex.decode("09535df890add4c840556f5a3821fe371e846bc46d7e7a3e015b686aa06edd101a5d5ce1bae8cfe7060394be35721664ca1e9f28fcca228d5e3235469376cb20091170d670579753f481d71c9b0449e6a1311787308cdcc801cacd813068bf29f2dd9794148077ebc18016510500badda4a2c6b5c70ddbbb95e9f7d97eaecf994455fa3953ba4851a776aa5ab432c02f1adb18a3aae24d764291c44b98dbe8ca9afa77456538b9521709a6f237126fa7a0952c18e729483f189fc0d3269a6b8a92c8eacbe4057ec7408723e948f4be406c0c5351243420e104855805fb8885dda19ce5d22509a48e550dc54831536095dd02c487d35f072e9903face6968edae597d87bc834dc8464e1652bd79f08429cf6a440adab6136c065f6463e701d8fe79c630b27ae364104c9b61618fb3689cd59a0de0f79c9a0a01579f78b5b13ef4daa86db916e98a7b46751c643b8a93565dadcd541013228328c0cbbba5daf4a582f17426957c60a1893ba55b1d2dd029c028548290929565c622119ff547a8f649f64ebe24d86044e78ad6577b3b84ce32f6248b576924281ef1936e924d65edac2ba9ac333f8df49bdc5ecda826ff9a661e296c952fe8c8d16b3b8ce82bad49d2747126d47c85ed46d9d5c53e46b2a76ada4ad31b0d2fca26145e5ce5a4f6df5a73ae0b37c6c2f48523fa885d2950b2d77c315c44e26054f54ce65a538a68916ed0f9a20428a1be4421526e9320273593033b5b25b548c9d759277530e5ec888e4c4a5198de03fbca120d09c46077cfecbdeea79b1c57a22a221dc9ea1a09a1544223a284093d1867f3e70f4c3472b75be19230b0a562791e8e2f2646738603d4b7a72c178b2ca1171a4df363cc116453aa85451005587b94e134ccab3e6e1d73ba245842174a1bb67c8c8b4b44f663c61b7aada00426275fc0444298f81c51841edcc5d8f5eeb31cbe4fc45621cc72001992d24bb4758efad013a57a7c3aa6683be8c49899f9f336af041ea4c68766cbdb0d0ed9e4a5aa13aaae7e469956736d2820774504090d62529356ef3c63c5c208ab68aeba506e7b45db78482edb557ba91b4b8ac677c75c237a5a4238876ed54c9d6fe4a6163ea3858163df6939a8eea9c9279f35424db975e2220089412a3a682aeb37fd5108a646e1bd4497933a5d4b31a000ba55049841754d7aa12a438fe9e9709f1df7a08946581fc12e780006b7c0995b574c196826e43c72c6c02346e5b1965c5ec9dd0d210800dee8e8e0a2");

        byte[] signature1024 = Hex.decode("3adf0b166c7fbdb6d92fdb4e62c058e24f9602828f2deb1d7a80cd064438fb37cbe16e3032ef6d7a2d84d709e4db83457f3d2d875daabbc4b22d5d488adfbab37b3b3bbd622318278fdb76e544a3e4204109da55daed132559a590e19e542b5beaa1669a4efb489929852284d2048e0945f6f2799cece495a6668dcd4dd57d5cc83ce21d8a4bf8dd9861655422c311ad37f61d12d3b858b5707ef72088a9c4839791405b0cfbaacaa8ec33dd3921e7c50193d1689f4d77a4adb1862e14b66b5cd2c9f8c70852acb342ef9d7422d137876ade15c538e8c4e3d0dadbc6c3348b720335ce14676f652d893992193acaa24f38b07bec4a9b0aacb2c8d1d5a3c1a146eb6290bb98b747249a4735354ceb09838e6ba0ee11b1f269ee3aebe9c7abb42d13a589c0b38dbd19af45542f7e377703e045e248de2931f930e6a798b09b5b2489f4a57de0fb645f9fe857642e7e661c555d4a4a14671a1d240e14f9ac5cad5fc627ee9f783a074321d2cc20acdb6d75f95ce2b61c2a81d6b1e3975e747d8bd5f1a132fb852eb2b4ce66dfbdbb0f993ed577850087dbd43b4483f9e3dfba444a0edf12c44ac67cdb08f0b02552e5aa34baf465a952652f291ccf6d3728687dbceb65b38281eb8606357d9aa6366c9241f56d32db6c720e47c5b6cba4ff67119314efd6341841b4e2526a383530ebbed32cb09f34ec44896fa574ed4cc8b7b7e63a15ad44b534aaf4494c916f21b10c1419ed8ecbd4110c8c7cfd79a8c1b2dc44f79305531ef51befaa07abcd319b2eff35e5d96f863e5b483dfbceb4967c94181981eaf6c638bba5870ff057a2a6ef8b4bcbc2278ef71fbad63ad56a82e996202c0bd185313be95be131f51bb2d42d0f82552665163cf5df27a76407cd8655df7686390e43721dbaa300dee2949e9fb1b0c317d35f2763659c23f99c2bdd4281fae5a07f4c6ab9a4381a932edc27176439cc95531759ae18842606e5c8d231959865a59633cdd675d96760b5d656a5058425d0b2a2e079ef59364286fa6e2d52c8b9fddc6366ed4e965937204ce9f2b6bcb64c9bbd389da464cedb8837c69668f023be8a625d096c15ceacaefee33c88c36fd193b968c4d3c3b15c9fae74a745646422dc68bd291a8e1f79eec0ec1374d6fe9c9e4597b2ccb35241509110e31615c4d7ecd71bee47ee2fedc4236dfe45657d200e867e8f73945ba51027a77abe9dd95eca23b0d84698781b24e2e01e752fb8ae21959e7d6353fddc424dee3adf204acf3497fc6e3affc59ec0cb35c91461d6b3c0234e7bdb8bafc998c4c56cef77209ab2268d42a9f75736017cf46bb39a283f3609d3d0caf9f843e095d2271ac52246ec646244129d53923357e8eb6f0c6accaed1ab5e9669a5125396d24c12c57d5978f33698c7c11364b7258a030bf16ede4c9fa170296ace16194755606b0d56b9eff4a0ef94086bda892dc7615669e70c6ee307d3429324af898666880a5bf0262ebee8e61e06c5896e1228414ef492e9c737570048d170c61b8d87d9174d12944962b5acb899d2bdd4b75bb852d73e7ef226edff270fb288d4f4c9d36188df12ba971190e444e5a254dc799c140ace6e7144eb0ca1dca27f9cba46d3c18f1ad89cb5442c6bdb6afa9636718156ec341cd4367ce3ebea355b4e051a81313d945d06cf55acd893bcdf8b11dbac619f583580ec2148af81829e45103b2fd135d784bb434b95266fbdc69bd9a3313aa6fa3044afa94bfd10f0aceca604b1308a17f1f3682347f917952447119e482ccac38f");
        byte[] publicKey1024 = Hex.decode("0a5e128e07a5c56191060ac1c78bae465222103068d42bda7975b2eee60f0c2252da20c72e5ae942772abe11c8a200c0cb322265cf799d4aad3c34e628a7aa864d14cbba24a4978bb411d12c0b1c2f4ec887fc5d595730b90a9d0e99817c9d7a2c2b3da26e7b8a50200bb64edcfa30e0dc8cb97790484ed280816ba8bb6df48ccaf985b7672746a9d5f962c91b66fde55bc18a352f3914b65e00a0b24eefb8625602acc215f0048bdf68edc0578444ef3d6e5ed1760a1e65b2194817d26c1c2eaf9453668b78ea2d81c804d0590c956b00c23617b8e5a91e90268d5454c6a31c40b902eda39aa9f1bbe1fb249068c42aa06dfb6366300eac5c694a6df122f67c4a5c2c561ec9e49830a97849e62dd16605415ec9e960b9294ab592b163355f09bc4a329b4aa5bc17a0d9a3d911b39fd5778727ca2680fc7b914a9ed205502af08982936b5d1ee65468ee5028ffd6c020c8a8b6dd716751eb525c6df8a1e3817bd6b33a1a9a6f9e26fd7b62a8e703d0db871d8d718765e3851e4df61efd3a1ae99cef6ffd4694f11c3faa8d70f350cba1ad665ab36a0eb0a6e5ef794fa5666b5d7f92a4d694a43ac24a19a1553a6556e57ee6ebb0ead9e23c2a6c26a2c376a1a3ba143a6d753ca07827c1cac43ba08e7436ac16a2ab58a230e484c70bcd59515453f0a19e77742fca6520970722ef64ef28a5b1d53250f55de19752e9a2c4cecfb094b7850f66d54df2eb424f2931210949f49e9e0b3930cfa430874437cd28d101493f3f21a0e95c8f270ad468e9bd58ea40e4cd4997a319175665399da796138d0fc46fa22e29e1e76b1b1aff2ca62660c75e4684981ca2ea9c6f67a03bf2098816091116f639117bbb99a2cb640aea9ba9654adb0af33975aa6698c7c08f4635800c4ba98e4c7a219194551ef4b0060748c35555ad5962607bbfde6986ed81ef00014cc2330e8d2c585768f989bf1a5aa8daf4c33364ac5163dd57484a922bfab12c732fe85859500f468bb9feab69c2bdb69d87d82d08d86ad2fc3a3c8a342494c4b3f725d4b2c1abe0520fa055b12d26cf9b98a2ece3a4158cf1a9a61388f42e5a7daa38c16794540b3dbe7b18a961209ee642355cc546d20cb8618eba3dbd5a991b24c2058804609cd6d22a39894b97136e2bb624f5d4af793dd6b9d3d66284846d75218fd1a0be18508b1581650ed2ab2bb8cc60ba340d6b98d18680e8c88160eab34946cb7bd700ca6cc422917be86e5df6d096ea57bc05aee181f020596e288478be036b02a861c88454bb7ff9616a17dd032868dc83290ffc5502f5773f476d5cd8cce2404bc317c064c22a8cbea221fae4f885e835f4b2b0f5c41f34566a97412a094b4c442108d56d1f74589345a1f7825112670f59f71e423344a9ea829121418ad3291cbce10dfbc6caa886129bf730174d0bc2b80330e0ae708e0e4b7fe56d600a3865c74cc4a7b9ea648328762cbe25f2bd9cc15a2620d9cc6d29a8c010925d5cc335a88b5098afac4e9a46756125dabae202312d0e0d84b40f34fed04313604c24cdc8877ec1eb6720af5148e58ee895d2bf7e0178676313d882812c54a945af8b61b1d16608ab210d2355de2f002dadd85b2a0d1761022fa3100837997548f633604c3ced6ce1e767bc97ea020805e28c84b93cc3ab57d9746622991250188c9204b8cd4d24bb2620e38ec26865797a9a26aeff8239686bcb06e63a5a3a87d8e90c21c4b1c620a58761ec94f1d90715bad88416326ddd04004c5b289bfb980a1e7b810c71091c0769b09aa81f346af199ad7108ac98020f08ad7ae52f4a7a41a42ad480baa7ad350684251b2e6ada61c42dd17b4fe3012d577a6c25051abe4872c4d8451ec1f0b599664f4265ba4ae6cbf4b5f66ddb36aed11301769523adde2408c25188ab80147ed5854a4ca14466e96b83af132f5031265754e60339a90b94804074080efae0eb295666c097af8c9860ed0bc3bddcf148bec190d1845d16dc7fb952c25e27ed81aa2c4590adfa34f5af56876a2169a9d74dd055ca0eb70c156491915daa6d0ff64a3104941a296f53a6e20bc568b402b084b6b7e97c9fae1bf0f0d073652dda28edabfb55ee24976d05f40beee4c2d80b44291e8b597b86ce08de5d78e151e42b626b6188293380f57b19b946d1365c459aa61beca82d59a2b4e63a965c72355c46275c9ca4a50599e7283c85e7cd048017fd24b4108c748cf415e56d82ad605a5ce578a7c4ad317ce90aab76b54da4617749cc5b40d9e3cd68581aa88e78cd09d44160fe085097e96edcbb987302d83e668d816b4fc5704175852c138eaa7e87075a517f95292f450703b6afb8e11a451d6760e8f4976760204434a1e68e816e1f71dad7755cd335b9e61bb21e1e202a9d11552a817d5441fb1f61c0a94063006c0b4d311ca1839b23c17a6dd610ba4da36666fc2893652f15ba1406ca9d5671e12f71346564a9a04498352cd52318c263021efd39a48ef13d1f6d47a920c8959ae37800a123a558acbad92ca73e0f90");

        System.out.println("Verifying 512 with 512 signature verification: " + Falcon.verify512(message, signature512, publicKey512));
        System.out.println("Verifying 512 with 1024 signature verification: " + Falcon.verify1024(message, signature512, publicKey512));

        System.out.println("Verifying 1024 with 512 signature verification: " + Falcon.verify512(message, signature1024, publicKey1024));
        System.out.println("Verifying 1024 with 1024 signature verification: " + Falcon.verify1024(message, signature1024, publicKey1024));
    }
}
