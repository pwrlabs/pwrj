package com.github.pwrlabs.pwrj.wallet;

import com.github.pwrlabs.pwrj.Utils.Falcon;
import com.github.pwrlabs.pwrj.Utils.Hex;
import com.github.pwrlabs.pwrj.Utils.PWRHash;
import com.github.pwrlabs.pwrj.protocol.PWRJ;
import com.github.pwrlabs.pwrj.protocol.Signature;
import com.github.pwrlabs.pwrj.protocol.TransactionBuilder;
import com.github.pwrlabs.pwrj.record.response.Response;
import org.bouncycastle.crypto.AsymmetricCipherKeyPair;
import org.bouncycastle.pqc.crypto.falcon.FalconPublicKeyParameters;

import java.io.IOException;
import java.math.BigInteger;
import java.nio.ByteBuffer;
import java.util.Arrays;

import static com.github.pwrlabs.pwrj.Utils.NewError.errorIf;

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

    public String getAddress() {
        return "0x" + Hex.toHexString(address);
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
        return pwrj.broadcastTransaction(getSignedTransferTransaction(receiver, amount, feePerByte));
    }

    public Response joinAsValidator(long feePerByte, String ip) throws IOException {
        return pwrj.broadcastTransaction(getSignedJoinAsValidatorTransaction(feePerByte, ip));
    }

    public Response delegate(byte[] validator, long pwrAmount, Long feePerByte) throws IOException {
        return pwrj.broadcastTransaction(getSignedDelegateTransaction(validator, pwrAmount, feePerByte));
    }

    public Response changeIp(String newIp, Long feePerByte) throws IOException {
        return pwrj.broadcastTransaction(getSignedChangeIpTransaction(newIp, feePerByte));
    }

    public Response claimActiveNodeSpot(Long feePerByte) throws IOException {
        return pwrj.broadcastTransaction(getSignedClaimActiveNodeSpotTransaction(feePerByte));
    }

    public Response submitVmData(long vmId, byte[] data, Long feePerByte) throws IOException {
        return pwrj.broadcastTransaction(getSignedSubmitVmDataTransaction(vmId, data, feePerByte));
    }

}
