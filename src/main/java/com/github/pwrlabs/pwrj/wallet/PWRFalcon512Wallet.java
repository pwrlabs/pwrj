package com.github.pwrlabs.pwrj.wallet;

import com.github.pwrlabs.pwrj.Utils.Falcon;
import com.github.pwrlabs.pwrj.Utils.Hex;
import com.github.pwrlabs.pwrj.Utils.PWRHash;
import com.github.pwrlabs.pwrj.protocol.PWRJ;
import com.github.pwrlabs.pwrj.protocol.Signature;
import com.github.pwrlabs.pwrj.protocol.TransactionBuilder;
import com.github.pwrlabs.pwrj.record.response.Response;
import org.bouncycastle.asn1.pkcs.PrivateKeyInfo;
import org.bouncycastle.asn1.x509.SubjectPublicKeyInfo;
import org.bouncycastle.crypto.AsymmetricCipherKeyPair;
import org.bouncycastle.crypto.params.AsymmetricKeyParameter;
import org.bouncycastle.crypto.util.PrivateKeyFactory;
import org.bouncycastle.crypto.util.PrivateKeyInfoFactory;
import org.bouncycastle.crypto.util.PublicKeyFactory;
import org.bouncycastle.crypto.util.SubjectPublicKeyInfoFactory;
import org.bouncycastle.pqc.crypto.falcon.*;
import org.bouncycastle.util.io.pem.PemObject;
import org.bouncycastle.util.io.pem.PemReader;
import org.bouncycastle.util.io.pem.PemWriter;

import java.io.*;
import java.math.BigInteger;
import java.nio.ByteBuffer;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.security.KeyFactory;
import java.security.KeyPair;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.spec.PKCS8EncodedKeySpec;
import java.security.spec.X509EncodedKeySpec;
import java.util.Arrays;

import static com.github.pwrlabs.pwrj.Utils.NewError.errorIf;

import org.bouncycastle.asn1.pkcs.PrivateKeyInfo;
import org.bouncycastle.asn1.x509.SubjectPublicKeyInfo;
import org.bouncycastle.crypto.params.AsymmetricKeyParameter;
import org.bouncycastle.crypto.util.PrivateKeyFactory;
import org.bouncycastle.crypto.util.PrivateKeyInfoFactory;
import org.bouncycastle.crypto.util.PublicKeyFactory;
import org.bouncycastle.crypto.util.SubjectPublicKeyInfoFactory;
import org.bouncycastle.util.io.pem.PemObject;
import org.bouncycastle.util.io.pem.PemReader;
import org.bouncycastle.util.io.pem.PemWriter;
import org.bouncycastle.pqc.crypto.falcon.FalconPrivateKeyParameters;
import org.bouncycastle.pqc.crypto.falcon.FalconPublicKeyParameters;
import org.bouncycastle.util.io.pem.PemObject;
import org.bouncycastle.util.io.pem.PemWriter;
import org.bouncycastle.jce.provider.BouncyCastleProvider;

public class PWRFalcon512Wallet {

    private final AsymmetricCipherKeyPair keyPair;
    private PWRJ pwrj;
    private byte[] address;

    public PWRFalcon512Wallet(PWRJ pwrj) {
        this.pwrj = pwrj;
        this.keyPair = Falcon.generateKeyPair512();

        FalconPublicKeyParameters publicKey = (FalconPublicKeyParameters) keyPair.getPublic();
        byte[] hash = PWRHash.hash224(publicKey.getH());
        address = Arrays.copyOfRange(hash, 0, 20);
    }

    public PWRFalcon512Wallet(PWRJ pwrj, AsymmetricCipherKeyPair keyPair) {
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
        FalconPublicKeyParameters falconPubKey = (FalconPublicKeyParameters) keyPair.getPublic();

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
    public static PWRFalcon512Wallet loadWallet(PWRJ pwrj, String filePath) throws IOException {
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

        return new PWRFalcon512Wallet(pwrj, keyPair);
    }


    public String getAddress() {
        return "0x" + Hex.toHexString(address);
    }

    public byte[] sign(byte[] data) {
        return Falcon.sign(data, keyPair);
    }
    public byte[] getSignedTransaction(byte[] transaction) {
        byte[] signature = Falcon.sign(transaction, keyPair);

        ByteBuffer buffer = ByteBuffer.allocate(2 + signature.length + transaction.length);
        buffer.put(transaction);
        buffer.putShort((short) signature.length);
        buffer.put(signature);

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

        byte[] transaction = TransactionBuilder.getFalconTransferTransaction(feePerByte, address, receiver, amount, pwrj.getNonceOfAddress(getAddress()), pwrj.getChainId());
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

    public byte[] getSignedSubmitVmDataTransaction(long vmId, byte[] data, Long feePerByte) throws IOException {
        errorIf(data == null || data.length == 0, "Data cannot be empty");
        long baseFeePerByte = pwrj.getFeePerByte();
        if(feePerByte == null || feePerByte == 0) feePerByte = baseFeePerByte;
        errorIf(feePerByte < baseFeePerByte, "Fee per byte must be greater than or equal to " + baseFeePerByte);

        byte[] transaction = TransactionBuilder.getFalconVmDataTransaction(feePerByte, address, vmId, data, pwrj.getNonceOfAddress(getAddress()), pwrj.getChainId());
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

    public Response submitVmData(long vmId, byte[] data, Long feePerByte) throws IOException {
        Response response = makeSurePublicKeyIsSet(feePerByte);
        if(response != null && !response.isSuccess()) return response;

        return pwrj.broadcastTransaction(getSignedSubmitVmDataTransaction(vmId, data, feePerByte));
    }

    private Response makeSurePublicKeyIsSet(long feePerByte) throws IOException {
        if(pwrj.getNonceOfAddress(getAddress()) == 0) {
            return setPublicKey(feePerByte);
        } else {
            return null;
        }
    }

}
