package com.github.pwrlabs.pwrj.wallet;

import com.github.pwrlabs.pwrj.Utils.Hash;
import com.github.pwrlabs.pwrj.Utils.Response;
import jdk.javadoc.doclet.Reporter;
import org.bouncycastle.jcajce.provider.digest.Keccak;
import org.bouncycastle.jce.ECNamedCurveTable;
import org.bouncycastle.math.ec.FixedPointCombMultiplier;
import org.bouncycastle.util.encoders.Hex;
import com.github.pwrlabs.pwrj.protocol.PWRJ;
import com.github.pwrlabs.pwrj.protocol.Signature;

import java.io.IOException;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.net.http.HttpClient;
import java.nio.ByteBuffer;
import org.bouncycastle.crypto.digests.KeccakDigest;
import org.bouncycastle.crypto.params.ECPrivateKeyParameters;
import org.bouncycastle.jce.provider.BouncyCastleProvider;
import org.bouncycastle.jce.spec.ECNamedCurveParameterSpec;
import org.bouncycastle.jce.spec.ECPrivateKeySpec;
import org.bouncycastle.math.ec.ECPoint;

import java.math.BigInteger;
import java.nio.charset.StandardCharsets;
import java.security.*;
import java.security.spec.InvalidKeySpecException;
import java.util.Arrays;

public class PWRWallet {

    static {
        // Add BouncyCastle as a Security Provider if it's not already present
        if (Security.getProvider(BouncyCastleProvider.PROVIDER_NAME) == null) {
            Security.addProvider(new BouncyCastleProvider());
        }
    }

    private static HttpClient client = HttpClient.newHttpClient();
    private final BigInteger privateKey;

    /**
     * Constructs a PWRWallet using a private key in string format.
     *
     * <p>This constructor converts the provided private key into {@code Credentials}
     * which can be used for various wallet operations.</p>
     *
     * @param privateKey The private key of the wallet in a string format.
     */
    public PWRWallet(String privateKey) {
        this.privateKey = new BigInteger(privateKey, 16);
    }


    /**
     * Constructs a PWRWallet using a private key in byte array format.
     *
     * <p>This constructor converts the byte array representation of the private key
     * into its hexadecimal string format, and then into {@code Credentials}.</p>
     *
     * @param privateKey The private key of the wallet in byte array format.
     */
    public PWRWallet(byte[] privateKey) {
        this.privateKey = new BigInteger(1, privateKey);
    }

    /**
     * Constructs a PWRWallet using a private key in {@code BigInteger} format.
     *
     * <p>This constructor converts the {@code BigInteger} representation of the private key
     * into its hexadecimal string format, and then into {@code Credentials}.</p>
     *
     * @param privateKey The private key of the wallet in {@code BigInteger} format.
     */
    public PWRWallet(BigInteger privateKey) {
        this.privateKey = privateKey;
    }

    /**
     * Constructs a PWRWallet using a random private key.
     *
     * <p>This constructor generates a random private key of 256 bits, and then
     * converts it into {@code Credentials}.</p>
     */
    public PWRWallet() {
        //Generate random private key
        this.privateKey = new BigInteger(256, new SecureRandom());
    }

    /**
     * Retrieves the address associated with the wallet's credentials.
     *
     * @return The address of the wallet in {@code String} format.
     */
    public String getAddress() {
        return publicKeyToAddress(publicKeyFromPrivate(privateKey));
    }

    public static String publicKeyToAddress(BigInteger publicKey) {
        // Convert public key to a byte array. This may vary depending on how the BigInteger represents the key.
        byte[] publicKeyBytes = publicKey.toByteArray();

        // If the first byte is 0x00 and the array is 65 bytes long, it is probably due to sign bit extension, and we can ignore it.
        if (publicKeyBytes.length == 65 && publicKeyBytes[0] == 0) {
            byte[] tmp = new byte[64];
            System.arraycopy(publicKeyBytes, 1, tmp, 0, 64);
            publicKeyBytes = tmp;
        }

        // Perform Keccak-256 hashing on the public key
        Keccak.Digest256 keccak256 = new Keccak.Digest256();
        byte[] addressBytes = keccak256.digest(publicKeyBytes);

        // Take the last 20 bytes of the hashed public key
        byte[] addr = new byte[20];
        System.arraycopy(addressBytes, addressBytes.length - 20, addr, 0, 20);

        // Convert to hex string and prepend "0x"
        return "0x" + Hex.toHexString(addr);
    }

    /**
     * Fetches the balance of the current wallet's address from the PWR network.
     *
     * @return The balance associated with the wallet's address.
     * @throws IOException If there's an issue with the network or stream handling.
     * @throws InterruptedException If the request is interrupted.
     */
    public long getBalance() throws IOException, InterruptedException {
        return PWRJ.getBalanceOfAddress(getAddress());
    }

    /**
     * Retrieves the nonce (number of transactions) of the wallet's address from the PWR network.
     *
     * @return The nonce of the wallet's address.
     * @throws IOException If there's an issue with the network or stream handling.
     * @throws InterruptedException If the request is interrupted.
     */
    public int getNonce() throws IOException, InterruptedException {
        return PWRJ.getNonceOfAddress(getAddress());
    }

    /**
     * Retrieves the private key associated with the wallet's credentials.
     *
     * <p>Note: Exposing private keys can pose security risks. Ensure that this method
     * is used judiciously and data is kept secure.</p>
     *
     * @return The private key of the wallet in {@code BigInteger} format.
     */
    public BigInteger getPrivateKey() {
        return privateKey;
    }


    /**
     * Transfers PWR tokens to a specified address.
     *
     * @param to The recipient address.
     * @param amount The amount of PWR tokens to be transferred.
     * @param nonce The transaction count of the wallet address.
     * @return A Response object encapsulating the outcome of the transaction broadcast.
     *         On successful broadcast: Response(success=true, message=transactionHash, error=null).
     *         On failure: Response(success=false, message=null, error=errorMessage).
     * @throws IOException If there's an issue with the network or stream handling.
     * @throws InterruptedException If the request is interrupted.
     * @throws RuntimeException For various transaction-related validation issues.
     */
    public Response transferPWR(String to, long amount, int nonce) throws IOException, InterruptedException {
        if(to.trim().length() != 42) {
            throw new RuntimeException("Invalid address");
        }
        if (amount < 0) {
            throw new RuntimeException("Amount cannot be negative");
        }
        if (nonce < 0) {
            throw new RuntimeException("Nonce cannot be negative");
        }
        if(amount + (98 * PWRJ.getFeePerByte()) > getBalance()) {
            throw new RuntimeException("Insufficient balance");
        }
        if (nonce < getNonce()) {
            throw new RuntimeException("Nonce is too low");
        }

        ByteBuffer buffer = ByteBuffer.allocate(33);
        buffer.put((byte) 0);
        buffer.putInt(nonce);
        buffer.putLong(amount);
        buffer.put(Hex.decode(to.substring(2)));
        byte[] txn = buffer.array();
        byte[] signature = Signature.signMessage(txn, privateKey);

        ByteBuffer finalTxn = ByteBuffer.allocate(98);
        finalTxn.put(txn);
        finalTxn.put(signature);

        return PWRJ.broadcastTxn(finalTxn.array());
    }

    /**
     * Transfers PWR tokens to a specified address using the current nonce.
     *
     * @param to The recipient address.
     * @param amount The amount of PWR tokens to be transferred.
     * @return A Response object encapsulating the outcome of the transaction broadcast.
     *         On successful broadcast: Response(success=true, message=transactionHash, error=null).
     *         On failure: Response(success=false, message=null, error=errorMessage).
     * @throws IOException If there's an issue with the network or stream handling.
     * @throws InterruptedException If the request is interrupted.
     */
    public Response transferPWR(String to, long amount) throws IOException, InterruptedException {
        return transferPWR(to, amount, getNonce());
    }

    /**
     * Joins the PWR network as a standby validator.
     *
     * @param ip The IP address of the validator.
     * @param nonce The transaction count of the wallet address.
     * @return A Response object encapsulating the outcome of the transaction broadcast.
     *         On successful broadcast: Response(success=true, message=transactionHash, error=null).
     *         On failure: Response(success=false, message=null, error=errorMessage).
     * @throws IOException If there's an issue with the network or stream handling.
     * @throws InterruptedException If the request is interrupted.
     * @throws RuntimeException For various transaction-related validation issues.
     */
    public Response join(String ip, int nonce) {
        ByteBuffer buffer = ByteBuffer.allocate(1 + 4 + ip.getBytes(StandardCharsets.UTF_8).length);

        buffer.put((byte) 1);
        buffer.putInt(nonce);
        buffer.put(ip.getBytes(StandardCharsets.UTF_8));

        byte[] txn = buffer.array();
        byte[] signature = Signature.signMessage(txn, privateKey);

        ByteBuffer finalTxn = ByteBuffer.allocate(txn.length + 65);
        finalTxn.put(txn);
        finalTxn.put(signature);

        return PWRJ.broadcastTxn(finalTxn.array());
    }
    /**
     * Joins the PWR network as a standby validator using the current nonce.
     *
     * @param ip The IP address of the validator.
     * @return A Response object encapsulating the outcome of the transaction broadcast.
     *         On successful broadcast: Response(success=true, message=transactionHash, error=null).
     *         On failure: Response(success=false, message=null, error=errorMessage).
     * @throws IOException If there's an issue with the network or stream handling.
     * @throws InterruptedException If the request is interrupted.
     */
    public Response join(String ip) throws IOException, InterruptedException {
        return join(ip, getNonce());
    }

    /**
     * Claims an active node spot on the PWR network.
     *
     * @param nonce The transaction count of the wallet address.
     * @return A Response object encapsulating the outcome of the transaction broadcast.
     *         On successful broadcast: Response(success=true, message=transactionHash, error=null).
     *         On failure: Response(success=false, message=null, error=errorMessage).
     * @throws IOException If there's an issue with the network or stream handling.
     * @throws InterruptedException If the request is interrupted.
     * @throws RuntimeException For various transaction-related validation issues.
     */
    public Response claimActiveNodeSpot(int nonce) {
        ByteBuffer buffer = ByteBuffer.allocate(5);

        buffer.put((byte) 2);
        buffer.putInt(nonce);

        byte[] txn = buffer.array();
        byte[] signature = Signature.signMessage(txn, privateKey);

        ByteBuffer finalTxn = ByteBuffer.allocate(txn.length + 65);
        finalTxn.put(txn);
        finalTxn.put(signature);

        return PWRJ.broadcastTxn(finalTxn.array());
    }
    /**
     * Claims an active node spot on the PWR network using the current nonce.
     *
     * @return A Response object encapsulating the outcome of the transaction broadcast.
     *         On successful broadcast: Response(success=true, message=transactionHash, error=null).
     *         On failure: Response(success=false, message=null, error=errorMessage).
     * @throws IOException If there's an issue with the network or stream handling.
     * @throws InterruptedException If the request is interrupted.
     */
    public Response claimActiveNodeSpot() throws IOException, InterruptedException {
        return claimActiveNodeSpot(getNonce());
    }

    /**
     * Delegates PWR tokens to a specified validator.
     *
     * @param to The validator address.
     * @param amount The amount of PWR tokens to be delegated.
     * @param nonce The transaction count of the wallet address.
     * @return A Response object encapsulating the outcome of the transaction broadcast.
     *         On successful broadcast: Response(success=true, message=transactionHash, error=null).
     *         On failure: Response(success=false, message=null, error=errorMessage).
     * @throws IOException If there's an issue with the network or stream handling.
     * @throws InterruptedException If the request is interrupted.
     * @throws RuntimeException For various transaction-related validation issues.
     */
    public Response delegate(String to, long amount, int nonce) {
        ByteBuffer buffer = ByteBuffer.allocate(33);

        buffer.put((byte) 3);
        buffer.putInt(nonce);
        buffer.putLong(amount);
        buffer.put(Hex.decode(to.substring(2)));

        byte[] txn = buffer.array();
        byte[] signature = Signature.signMessage(txn, privateKey);

        ByteBuffer finalTxn = ByteBuffer.allocate(txn.length + 65);
        finalTxn.put(txn);
        finalTxn.put(signature);

        return PWRJ.broadcastTxn(finalTxn.array());
    }
    /**
     * Delegates PWR tokens to a specified validator using the current nonce.
     *
     * @param to The validator address.
     * @param amount The amount of PWR tokens to be delegated.
     * @return A Response object encapsulating the outcome of the transaction broadcast.
     *         On successful broadcast: Response(success=true, message=transactionHash, error=null).
     *         On failure: Response(success=false, message=null, error=errorMessage).
     * @throws IOException If there's an issue with the network or stream handling.
     * @throws InterruptedException If the request is interrupted.
     */
    public Response delegate(String to, long amount) throws IOException, InterruptedException {
        return delegate(to, amount, getNonce());
    }

    /**
     * Withdraws PWR tokens from a specified validator.
     *
     * @param from The validator address.
     * @param sharesAmount The amount of shares to be withdrawn.
     * @param nonce The transaction count of the wallet address.
     * @return A Response object encapsulating the outcome of the transaction broadcast.
     *         On successful broadcast: Response(success=true, message=transactionHash, error=null).
     *         On failure: Response(success=false, message=null, error=errorMessage).
     * @throws IOException If there's an issue with the network or stream handling.
     * @throws InterruptedException If the request is interrupted.
     * @throws RuntimeException For various transaction-related validation issues.
     */
    public Response withdraw(String from, long sharesAmount, int nonce) {
        ByteBuffer buffer = ByteBuffer.allocate(33);

        buffer.put((byte) 4);
        buffer.putInt(nonce);
        buffer.putLong(sharesAmount);
        buffer.put(Hex.decode(from.substring(2)));

        byte[] txn = buffer.array();
        byte[] signature = Signature.signMessage(txn, privateKey);

        ByteBuffer finalTxn = ByteBuffer.allocate(txn.length + 65);
        finalTxn.put(txn);
        finalTxn.put(signature);

        return PWRJ.broadcastTxn(finalTxn.array());
    }
    /**
     * Withdraws PWR tokens from a specified validator using the current nonce.
     *
     * @param from The validator address.
     * @param sharesAmount The amount of shares to be withdrawn.
     * @return A Response object encapsulating the outcome of the transaction broadcast.
     *         On successful broadcast: Response(success=true, message=transactionHash, error=null).
     *         On failure: Response(success=false, message=null, error=errorMessage).
     * @throws IOException If there's an issue with the network or stream handling.
     * @throws InterruptedException If the request is interrupted.
     */
    public Response withdraw(String from, long sharesAmount) throws IOException, InterruptedException {
        return withdraw(from, sharesAmount, getNonce());
    }

    /**
     * Withdraws PWR tokens from a specified validator.
     *
     * @param from The validator address.
     * @param pwrAmount The amount of PWR to be withdrawn.
     * @param nonce The transaction count of the wallet address.
     * @return A Response object encapsulating the outcome of the transaction broadcast.
     *         On successful broadcast: Response(success=true, message=transactionHash, error=null).
     *         On failure: Response(success=false, message=null, error=errorMessage).
     * @throws IOException If there's an issue with the network or stream handling.
     * @throws InterruptedException If the request is interrupted.
     * @throws RuntimeException For various transaction-related validation issues.
     */
    public Response withdrawPWR(String from, long pwrAmount, int nonce) {
        BigDecimal shareValue = PWRJ.getShareValue(from);
        long sharesAmount = BigDecimal.valueOf(pwrAmount).divide(shareValue, 18, BigDecimal.ROUND_DOWN).longValue();

        ByteBuffer buffer = ByteBuffer.allocate(33);

        buffer.put((byte) 4);
        buffer.putInt(nonce);
        buffer.putLong(sharesAmount);
        buffer.put(Hex.decode(from.substring(2)));

        byte[] txn = buffer.array();
        byte[] signature = Signature.signMessage(txn, privateKey);

        ByteBuffer finalTxn = ByteBuffer.allocate(txn.length + 65);
        finalTxn.put(txn);
        finalTxn.put(signature);

        return PWRJ.broadcastTxn(finalTxn.array());
    }
    /**
     * Withdraws PWR tokens from a specified validator using the current nonce.
     *
     * @param from The validator address.
     * @param pwrAmount The amount of PWR to be withdrawn.
     * @return A Response object encapsulating the outcome of the transaction broadcast.
     *         On successful broadcast: Response(success=true, message=transactionHash, error=null).
     *         On failure: Response(success=false, message=null, error=errorMessage).
     * @throws IOException If there's an issue with the network or stream handling.
     * @throws InterruptedException If the request is interrupted.
     */
    public Response withdrawPWR(String from, long pwrAmount) throws IOException, InterruptedException {
        return withdraw(from, pwrAmount, getNonce());
    }

    /**
     * Sends data to a specified VM on the PWR network.
     *
     * @param vmId The ID of the VM.
     * @param data The data to be sent.
     * @param nonce The transaction count of the wallet address.
     * @return A Response object encapsulating the outcome of the transaction broadcast.
     *         On successful broadcast: Response(success=true, message=transactionHash, error=null).
     *         On failure: Response(success=false, message=null, error=errorMessage).
     * @throws IOException If there's an issue with the network or stream handling.
     * @throws InterruptedException If the request is interrupted.
     * @throws RuntimeException For various transaction-related validation issues.
     */
    public Response sendVmDataTxn(long vmId, byte[] data, int nonce) throws IOException, InterruptedException {
        if (nonce < 0) {
            throw new RuntimeException("Nonce cannot be negative");
        }
        if (nonce < getNonce()) {
            throw new RuntimeException("Nonce is too low");
        }

        ByteBuffer buffer = ByteBuffer.allocate(13 + data.length);
        buffer.put((byte) 5);
        buffer.putInt(nonce);
        buffer.putLong(vmId);
        buffer.put(data);
        byte[] txn = buffer.array();
        byte[] signature = Signature.signMessage(txn, privateKey);

        ByteBuffer finalTxn = ByteBuffer.allocate(13 + 65 + data.length);
        finalTxn.put(txn);
        finalTxn.put(signature);

        return PWRJ.broadcastTxn(finalTxn.array());
    }

    /**
     * Sends data to a specified VM on the PWR network using the current nonce.
     *
     * @param vmId The ID of the VM.
     * @param data The data to be sent.
     * @return A Response object encapsulating the outcome of the transaction broadcast.
     *         On successful broadcast: Response(success=true, message=transactionHash, error=null).
     *         On failure: Response(success=false, message=null, error=errorMessage).
     * @throws IOException If there's an issue with the network or stream handling.
     * @throws InterruptedException If the request is interrupted.
     */
    public Response sendVmDataTxn(long vmId, byte[] data) throws IOException, InterruptedException {
        return sendVmDataTxn(vmId, data, getNonce());
    }

    /**
     * Sends a transaction to claim a Virtual Machine ID on the PWR network, ensuring its owner 15% revenue of all transaction fees paid when transacting with this VM.
     *
     * @param vmId The ID of the VM.
     * @param nonce The transaction count of the wallet address.
     * @return A Response object encapsulating the outcome of the transaction broadcast.
     *         On successful broadcast: Response(success=true, message=transactionHash, error=null).
     *         On failure: Response(success=false, message=null, error=errorMessage).
     */
    public Response claimVmId(long vmId, int nonce) {
        ByteBuffer buffer = ByteBuffer.allocate(13);
        buffer.put((byte) 6);
        buffer.putInt(nonce);
        buffer.putLong(vmId);
        byte[] txn = buffer.array();
        byte[] signature = Signature.signMessage(txn, privateKey);

        ByteBuffer finalTxn = ByteBuffer.allocate(txn.length + 65);
        finalTxn.put(txn);
        finalTxn.put(signature);

        return PWRJ.broadcastTxn(finalTxn.array());
    }

    /**
     * Sends a transaction with the current nonce to claim a Virtual Machine ID on the PWR network, ensuring its owner 15% revenue of all transaction fees paid when transacting with this VM.
     *
     * @param vmId The ID of the VM.
     * @return A Response object encapsulating the outcome of the transaction broadcast.
     *         On successful broadcast: Response(success=true, message=transactionHash, error=null).
     *         On failure: Response(success=false, message=null, error=errorMessage).
     * @throws IOException If there's an issue with the network or stream handling.
     * @throws InterruptedException If the request is interrupted.
     */
    public Response claimVmId(long vmId) throws IOException, InterruptedException {
        return claimVmId(vmId, getNonce());
    }

    /**
     * Sends a conduit wrapped transaction of a specified VM on the PWR network. Must be sent from a conduit node of that VM.
     *
     * @param vmId The ID of the VM.
     * @param txn The transaction to be sent.
     * @param nonce The transaction count of the wallet address.
     * @return A Response object encapsulating the outcome of the transaction broadcast.
     *         On successful broadcast: Response(success=true, message=transactionHash, error=null).
     *         On failure: Response(success=false, message=errorMessage, error=null).
     * @throws IOException If there's an issue with the network or stream handling.
     * @throws InterruptedException If the request is interrupted.
     * @throws RuntimeException For various transaction-related validation issues.
     */
    public Response sendConduitTransaction(long vmId, byte[] txn, int nonce) throws IOException, InterruptedException {
        if (nonce < 0) {
            throw new RuntimeException("Nonce cannot be negative");
        }
        if (nonce < getNonce()) {
            throw new RuntimeException("Nonce is too low");
        }

        ByteBuffer buffer = ByteBuffer.allocate(13 + txn.length);
        buffer.put((byte) 7);
        buffer.putInt(nonce);
        buffer.putLong(vmId);
        buffer.put(txn);
        byte[] txnBytes = buffer.array();
        byte[] signature = Signature.signMessage(txnBytes, privateKey);

        ByteBuffer finalTxn = ByteBuffer.allocate(txnBytes.length + 65);
        finalTxn.put(txnBytes);
        finalTxn.put(signature);

        return PWRJ.broadcastTxn(finalTxn.array());
    }

    /**
     * Sends a conduit wrapped transaction of a specified VM on the PWR network using the current nonce. Must be sent from a conduit node of that VM.
     *
     * @param vmId The ID of the VM.
     * @param txn The transaction to be sent.
     * @return A Response object encapsulating the outcome of the transaction broadcast.
     *         On successful broadcast: Response(success=true, message=transactionHash, error=null).
     *         On failure: Response(success=false, message=errorMessage, error=null).
     * @throws IOException If there's an issue with the network or stream handling.
     * @throws InterruptedException If the request is interrupted.
     */
    public Response sendConduitTransaction(long vmId, byte[] txn) throws IOException, InterruptedException {
        return sendConduitTransaction(vmId, txn, getNonce());
    }

    public static BigInteger publicKeyFromPrivate(BigInteger privKey) {
        ECPoint point = publicPointFromPrivate(privKey);
        byte[] encoded = point.getEncoded(false);
        return new BigInteger(1, Arrays.copyOfRange(encoded, 1, encoded.length));
    }

    public static ECPoint publicPointFromPrivate(BigInteger privKey) {
        if (privKey.bitLength() > Signature.CURVE.getN().bitLength()) {
            privKey = privKey.mod(Signature.CURVE.getN());
        }

        return (new FixedPointCombMultiplier()).multiply(Signature.CURVE.getG(), privKey);
    }

}
