package com.github.pwrlabs.pwrj.wallet;

import com.github.pwrlabs.pwrj.Utils.Hash;
import com.github.pwrlabs.pwrj.Utils.Response;
import jdk.javadoc.doclet.Reporter;
import org.bouncycastle.jcajce.provider.digest.Keccak;
import org.bouncycastle.jce.ECNamedCurveTable;
import org.bouncycastle.util.encoders.Hex;
import com.github.pwrlabs.pwrj.protocol.PWRJ;
import com.github.pwrlabs.pwrj.protocol.Signature;

import java.io.IOException;
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
import java.security.*;
import java.util.Arrays;

public class PWRWallet {
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
     * Retrieves the public key associated with the wallet's credentials.
     *
     * @return The public key of the wallet in {@code BigInteger} format.
     */
    public BigInteger getPublicKey() {
        try {
            ECNamedCurveParameterSpec spec = org.bouncycastle.jce.ECNamedCurveTable.getParameterSpec("secp256k1");
            ECPrivateKeySpec privateKeySpec = new ECPrivateKeySpec(new BigInteger(1, privateKey.toByteArray()), spec);
            KeyFactory kf = KeyFactory.getInstance("ECDSA", "BC");
            PrivateKey privateKey = kf.generatePrivate(privateKeySpec);
            ECPoint publicKey = spec.getG().multiply(((java.security.interfaces.ECPrivateKey) privateKey).getS());

            // Skip the first byte of the public key (it's the uncompressed key prefix 0x04)
            byte[] publicKeyBytes = publicKey.getEncoded(false);
            byte[] publicKeyNoPrefix = Arrays.copyOfRange(publicKeyBytes, 1, publicKeyBytes.length);

            // Take the Keccak-256 hash of the public key (no prefix)
            byte[] hashedPublicKey = Hash.keccak256(publicKeyNoPrefix);

            // Take the last 20 bytes of the hash to get the Ethereum address
            return new BigInteger(1, publicKeyNoPrefix);
        } catch (Exception e) {
            e.printStackTrace();
        }
        return new BigInteger(1, new byte[0]);
    }

    /**
     * Retrieves the address associated with the wallet's credentials.
     *
     * @return The address of the wallet in {@code String} format.
     */
    public String getAddress() {
        try {
            //Extract the public key as byte array and removet he first byte
            byte[] publicKeyBytes = getPublicKey().toByteArray();
            byte[] publicKeyNoPrefix = Arrays.copyOfRange(publicKeyBytes, 1, publicKeyBytes.length);
            BigInteger publicKeyHash = new BigInteger(Hash.keccak256(publicKeyNoPrefix));

            // Take the last 20 bytes of the hash to get the Ethereum address
            byte[] addressBytes = Arrays.copyOfRange(publicKeyHash.toByteArray(), publicKeyHash.toByteArray().length - 20, publicKeyHash.toByteArray().length);
            return "0x" + Hex.toHexString(addressBytes);
        } catch (Exception e) {
            e.printStackTrace();
        }
        return "0x0000000000000000000000000000000000000000";
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
        buffer.put((byte) 1);
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
}
