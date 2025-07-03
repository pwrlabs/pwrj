package com.github.pwrlabs.pwrj.wallet;

import com.github.pwrlabs.pwrj.Utils.AES256;
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
import java.security.InvalidAlgorithmParameterException;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.security.spec.InvalidKeySpecException;
import java.util.*;

import static com.github.pwrlabs.pwrj.Utils.NewError.errorIf;

import org.bouncycastle.pqc.crypto.falcon.FalconPrivateKeyParameters;
import org.bouncycastle.pqc.crypto.falcon.FalconPublicKeyParameters;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.web3j.crypto.MnemonicUtils;

import javax.crypto.BadPaddingException;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.NoSuchPaddingException;

public class PWRFalconWallet {
    private static final Logger log = LoggerFactory.getLogger(PWRFalconWallet.class);
    private final AsymmetricCipherKeyPair keyPair;
    private final String seedPhrase;
    private PWRJ pwrj;
    private byte[] address;

    public PWRFalconWallet(int wordCount, PWRJ pwrj) {
        if (wordCount != 12 && wordCount != 15 && wordCount != 18 && wordCount != 21 && wordCount != 24) {
            throw new IllegalArgumentException("Word count must be one of 12, 15, 18, 21, or 24");
        }

        this.pwrj = pwrj;

        // Calculate entropy bytes based on word count
        int entropyBytes;
        switch (wordCount) {
            case 12: entropyBytes = 16; break; // 128 bits
            case 15: entropyBytes = 20; break; // 160 bits
            case 18: entropyBytes = 24; break; // 192 bits
            case 21: entropyBytes = 28; break; // 224 bits
            case 24: entropyBytes = 32; break; // 256 bits
            default: throw new IllegalArgumentException("Invalid word count");
        }

        // Generate random entropy
        byte[] entropy = new byte[entropyBytes];
        new SecureRandom().nextBytes(entropy);

        // Use Web3j's MnemonicUtils to create the mnemonic
        String phrase = MnemonicUtils.generateMnemonic(entropy);
        byte[] seed = MnemonicUtils.generateSeed(phrase, "");

        keyPair = Falcon.generateKeyPair512FromSeed(seed);
        FalconPublicKeyParameters publicKey = (FalconPublicKeyParameters) keyPair.getPublic();
        byte[] hash = PWRHash.hash224(publicKey.getH());
        address = Arrays.copyOfRange(hash, 0, 20);

        seedPhrase = phrase;
    }

    public PWRFalconWallet(String seedPhrase, PWRJ pwrj) {
        this.pwrj = pwrj;
        this.seedPhrase = seedPhrase;

        byte[] seed = MnemonicUtils.generateSeed(seedPhrase, "");
        keyPair = Falcon.generateKeyPair512FromSeed(seed);

        FalconPublicKeyParameters publicKey = (FalconPublicKeyParameters) keyPair.getPublic();
        byte[] hash = PWRHash.hash224(publicKey.getH());
        address = Arrays.copyOfRange(hash, 0, 20);
    }

    public PWRFalconWallet(String seedPhrase, int walletNumber, PWRJ pwrj) {
        this.pwrj = pwrj;
        this.seedPhrase = seedPhrase;

        byte[] seed = MnemonicUtils.generateSeed(seedPhrase, "");
        if(walletNumber > 0) seed = new BigInteger(1, seed).add(BigInteger.valueOf(walletNumber)).toByteArray();
        keyPair = Falcon.generateKeyPair512FromSeed(seed);

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
    public void storeWallet(String filePath, String password) throws IOException, InvalidAlgorithmParameterException, NoSuchPaddingException, IllegalBlockSizeException, NoSuchAlgorithmException, InvalidKeySpecException, BadPaddingException, InvalidKeyException {
        //Encrypt seed phrase
        byte[] encryptedSeed = AES256.encrypt(seedPhrase.getBytes(StandardCharsets.UTF_8), password);

        //create all paths if missing
        File file = new File(filePath);
        if (!file.getParentFile().exists()) {
            file.getParentFile().mkdirs();
        }

        Files.write(Paths.get(filePath), encryptedSeed);
    }

    /**
     * Loads a Falcon wallet (private/public key pair) from a PEM file on disk.
     *
     * @param pwrj     The PWRJ instance to use for the wallet.
     * @param filePath Path to the PEM file that contains the private/public keys.
     * @return a new PWRFalcon512Wallet with the loaded key pair.
     * @throws IOException if there's an error reading the file or parsing the keys.
     */
    public static PWRFalconWallet loadWallet(PWRJ pwrj, String filePath, String password) throws IOException, InvalidAlgorithmParameterException, NoSuchPaddingException, IllegalBlockSizeException, NoSuchAlgorithmException, InvalidKeySpecException, BadPaddingException, InvalidKeyException {
        File file = new File(filePath);
        if (!file.exists()) {
            throw new FileNotFoundException("Wallet file not found: " + filePath);
        }

        byte[] encryptedSeed = Files.readAllBytes(file.toPath());
        byte[] decryptedSeed = AES256.decrypt(encryptedSeed, password);

        String seedPhrase = new String(decryptedSeed, StandardCharsets.UTF_8);
        return new PWRFalconWallet(seedPhrase, pwrj);
    }

    public String getAddress() {
        return "0x" + Hex.toHexString(address);
    }

    public String getSeedPhrase() {
        return seedPhrase;
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

    public static void main(String[] args) throws Exception {
        PWRJ pwrj = new PWRJ("https://pwrrpc.pwrlabs.io/");
        PWRFalconWallet wallet = new PWRFalconWallet(12, pwrj);
        System.out.println("Address: " + wallet.getAddress());

        System.out.println("Seed phrase: " + wallet.getSeedPhrase());

    }
}
