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
import java.time.Instant;
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

    public byte[] getSignedTxn(byte[] txn) {
        if(txn == null) return null;

        byte[] signature = Signature.signMessage(txn, privateKey);

        ByteBuffer finalTxn = ByteBuffer.allocate(txn.length + 65);
        finalTxn.put(txn);
        finalTxn.put(signature);

        return finalTxn.array();
    }

    public byte[] getTxnBase(byte identifier, int nonce) throws IOException {
        ByteBuffer buffer = ByteBuffer.allocate(6);
        buffer.put(identifier);
        buffer.put(PWRJ.getChainId());
        buffer.putInt(nonce);
        return buffer.array();
    }

    /**
     * Returns the transaction of transfer PWR tokens to a specified address.
     *
     * @param to The recipient address.
     * @param amount The amount of PWR tokens to be transferred.
     * @param nonce The transaction count of the wallet address.
     * @return A byte array that represents the transaction of this method.
     * @throws RuntimeException For various transaction-related validation issues.
     */
    public byte[] getTransferPWRTxn(String to, long amount, int nonce) throws IOException {
        if(to.length() != 40 && to.length() != 42) {
            throw new RuntimeException("Invalid address");
        }
        if (amount < 0) {
            throw new RuntimeException("Amount cannot be negative");
        }
        if (nonce < 0) {
            throw new RuntimeException("Nonce cannot be negative");
        }

        if(to.length() == 42) {
            to = to.substring(2);
        }

        byte[] txnBase = getTxnBase((byte) 0, nonce);
        ByteBuffer buffer = ByteBuffer.allocate(txnBase.length + 8 + 20);
        buffer.put(txnBase);
        buffer.putLong(amount);
        buffer.put(Hex.decode(to));

        return buffer.array();
    }
    /**
     * Returns the signed transaction of Transfer PWR tokens to a specified address.
     *
     * @param to The recipient address.
     * @param amount The amount of PWR tokens to be transferred.
     * @param nonce The transaction count of the wallet address.
     * @return A byte array which represents the signed transaction of this method
     * @throws RuntimeException For various transaction-related validation issues.
     */
    public byte[] getSignedTransferPWRTxn(String to, long amount, int nonce) throws IOException {
        return getSignedTxn(getTransferPWRTxn(to, amount, nonce));
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
    public Response transferPWR(String to, long amount, int nonce) throws IOException {
        return PWRJ.broadcastTxn(getSignedTransferPWRTxn(to, amount, nonce));
    }
    /**
     * Transfers PWR tokens to a specified address using the current nonce.
     *
     * @param to The recipient address using the current nonce.
     * @param amount The amount of PWR tokens to be transferred.
     * @return A Response object encapsulating the outcome of the transaction broadcast.
     *         On successful broadcast: Response(success=true, message=transactionHash, error=null).
     *         On failure: Response(success=false, message=null, error=errorMessage).
     * @throws IOException If there's an issue with the network or stream handling.
     * @throws InterruptedException If the request is interrupted.
     * @throws RuntimeException For various transaction-related validation issues.
     */
    public Response transferPWR(String to, long amount) throws IOException, InterruptedException {
        return transferPWR(to, amount, getNonce());
    }

    /**
     * returns the transaction of joining the PWR network as a standby validator.
     *
     * @param ip The IP address of the validator.
     * @param nonce The transaction count of the wallet address.
     * @return A byte array that represents the transaction of this method
     */
    public byte[] getJoinTxn(String ip, int nonce) throws IOException {
        byte[] txnBase = getTxnBase((byte) 1, nonce);
        byte[] ipBytes = ip.getBytes(StandardCharsets.UTF_8);

        ByteBuffer buffer = ByteBuffer.allocate(txnBase.length + ipBytes.length);
        buffer.put(txnBase);
        buffer.put(ip.getBytes(StandardCharsets.UTF_8));

        return buffer.array();
    }
    /**
     * Returns a signed transaction of joining the PWR network as a standby validator.
     *
     * @param ip The IP address of the validator.
     * @param nonce The transaction count of the wallet address.
     * @return A byte array that represents a signed transaction of this method
     */
    public byte[] getSignedJoinTxn(String ip, int nonce) throws IOException {
        return getSignedTxn(getJoinTxn(ip, nonce));
    }
    /**
     * Joins the PWR network as a standby validator.
     *
     * @param ip The IP address of the validator.
     * @param nonce The transaction count of the wallet address.
     * @return A Response object encapsulating the outcome of the transaction broadcast.
     *         On successful broadcast: Response(success=true, message=transactionHash, error=null).
     *         On failure: Response(success=false, message=null, error=errorMessage).
     */
    public Response join(String ip, int nonce) throws IOException {
        return PWRJ.broadcastTxn(getSignedJoinTxn(ip, nonce));
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
     * Returns the transaction of claim an active node spot on the PWR network.
     *
     * @param nonce The transaction count of the wallet address.
     * @return A byte array that represents a signed transaction of the method
     */
    public byte[] getClaimActiveNodeSpotTxn(int nonce) throws IOException {
        byte[] txnBase = getTxnBase((byte) 2, nonce);

        return txnBase;
    }
    /**
     * Returns the signed transaction of Claim an active node spot on the PWR network.
     *
     * @param nonce The transaction count of the wallet address.
     * @return A byte array that represents the signed transaction of this method
     */
    public byte[] getSignedClaimActiveNodeSpotTxn(int nonce) throws IOException {
        return getSignedTxn(getClaimActiveNodeSpotTxn(nonce));
    }
    /**
     * Claims an active node spot on the PWR network.
     *
     * @param nonce The transaction count of the wallet address.
     * @return A Response object encapsulating the outcome of the transaction broadcast.
     *         On successful broadcast: Response(success=true, message=transactionHash, error=null).
     *         On failure: Response(success=false, message=null, error=errorMessage).
     */
    public Response claimActiveNodeSpot(int nonce) throws IOException {
        return PWRJ.broadcastTxn(getSignedClaimActiveNodeSpotTxn(nonce));
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
     * Returns the transaction of delegate PWR tokens to a specified validator.
     *
     * @param to The validator address.
     * @param amount The amount of PWR tokens to be delegated.
     * @param nonce The transaction count of the wallet address.
     * @return A byte array that represents the transaction of the method
     */
    public byte[] getDelegateTxn(String to, long amount, int nonce) throws IOException {
        if(to.length() != 40 && to.length() != 42) {
            throw new RuntimeException("Invalid address");
        } if (amount < 0) {
            throw new RuntimeException("Amount cannot be negative");
        } if (nonce < 0) {
            throw new RuntimeException("Nonce cannot be negative");
        }

        if(to.length() == 42) {
            to = to.substring(2);
        }

        byte[] txnBase = getTxnBase((byte) 3, nonce);
        ByteBuffer buffer = ByteBuffer.allocate(txnBase.length + 8 + (to.length()/2));
        buffer.put(txnBase);
        buffer.putLong(amount);
        buffer.put(Hex.decode(to));

        return buffer.array();
    }
    /**
     * Returns the signed transaction of delegate PWR tokens to a specified validator.
     *
     * @param to The validator address.
     * @param amount The amount of PWR tokens to be delegated.
     * @param nonce The transaction count of the wallet address.
     * @return A byte array that represents a signed transaction of the method
     */
    public byte[] getSignedDelegateTxn(String to, long amount, int nonce) throws IOException {
        return getSignedTxn(getDelegateTxn(to, amount, nonce));
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
     */
    public Response delegate(String to, long amount, int nonce) throws IOException {
        return PWRJ.broadcastTxn(getSignedDelegateTxn(to, amount, nonce));
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
     * Returns the transaction of withdraw PWR tokens from a specified validator.
     *
     * @param from The validator address.
     * @param sharesAmount The amount of shares to be withdrawn.
     * @param nonce The transaction count of the wallet address.
     * @return A byte array that represents the transaction of this method
     */
    public byte[] getWithdrawTxn(String from, long sharesAmount, int nonce) throws IOException {
        if(from.length() != 40 && from.length() != 42) {
            throw new RuntimeException("Invalid address");
        }
        if (sharesAmount < 0) {
            throw new RuntimeException("Shares amount cannot be negative");
        }
        if (nonce < 0) {
            throw new RuntimeException("Nonce cannot be negative");
        }

        if(from.length() == 42) {
            from = from.substring(2);
        }

        byte[] txnBase = getTxnBase((byte) 4, nonce);
        ByteBuffer buffer = ByteBuffer.allocate(txnBase.length + 8 + (from.length()/2));
        buffer.put(txnBase);
        buffer.putLong(sharesAmount);
        buffer.put(Hex.decode(from));

        return buffer.array();
    }
    /**
     * Returns the signed transaction of withdraw PWR tokens from a specified validator.
     *
     * @param from The validator address.
     * @param sharesAmount The amount of shares to be withdrawn.
     * @param nonce The transaction count of the wallet address.
     * @return A byte array that represents the signed transaction of this method
     */
    public byte[] getSignedWithdrawTxn(String from, long sharesAmount, int nonce) throws IOException {
        return getSignedTxn(getWithdrawTxn(from, sharesAmount, nonce));
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
     */
    public Response withdraw(String from, long sharesAmount, int nonce) throws IOException {
        return PWRJ.broadcastTxn(getSignedWithdrawTxn(from, sharesAmount, nonce));
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
     * Returns the transaction of withdraw PWR tokens from a specified validator.
     *
     * @param from The validator address.
     * @param pwrAmount The amount of PWR to be withdrawn.
     * @param nonce The transaction count of the wallet address.
     * @return A byte array that represents the transaction of this method
     */
    public byte[] getWithdrawPWRTxn(String from, long pwrAmount, int nonce) throws IOException {
        if(from.length() != 40 && from.length() != 42) {
            throw new RuntimeException("Invalid address");
        }
        if (pwrAmount < 0) {
            throw new RuntimeException("PWR amount cannot be negative");
        }
        if (nonce < 0) {
            throw new RuntimeException("Nonce cannot be negative");
        }

        if(from.length() == 42) {
            from = from.substring(2);
        }

        BigDecimal shareValue = PWRJ.getShareValue(from);
        long sharesAmount = BigDecimal.valueOf(pwrAmount).divide(shareValue, 18, BigDecimal.ROUND_DOWN).longValue();

        if(sharesAmount <= 0) {
            throw new RuntimeException("Shares amount is too low");
        }

        byte[] txnBase = getTxnBase((byte) 4, nonce);
        ByteBuffer buffer = ByteBuffer.allocate(txnBase.length + 8 + (from.length()/2));
        buffer.put(txnBase);
        buffer.putLong(sharesAmount);
        buffer.put(Hex.decode(from));

        return buffer.array();
    }
    /**
     * Returns the signed transaction of withdraw PWR tokens from a specified validator.
     *
     * @param from The validator address.
     * @param pwrAmount The amount of PWR to be withdrawn.
     * @param nonce The transaction count of the wallet address.
     * @return A byte array that represents the signed transaction of this method
     */
    public byte[] getSignedWithdrawPWRTxn(String from, long pwrAmount, int nonce) throws IOException {
        return getSignedTxn(getWithdrawPWRTxn(from, pwrAmount, nonce));
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
     */
    public Response withdrawPWR(String from, long pwrAmount, int nonce) throws IOException {
        return PWRJ.broadcastTxn(getSignedWithdrawPWRTxn(from, pwrAmount, nonce));
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
     * Returns the transaction of send data to a specified VM on the PWR network.
     *
     * @param vmId The ID of the VM.
     * @param data The data to be sent.
     * @param nonce The transaction count of the wallet address.
     * @return A byte array that represents the transaction of this method
     * @throws IOException If there's an issue with the network or stream handling.
     * @throws InterruptedException If the request is interrupted.
     * @throws RuntimeException For various transaction-related validation issues.
     */
    public byte[] getSendVmDataTxn(long vmId, byte[] data, int nonce) throws IOException, InterruptedException{
        if (nonce < 0) {
            throw new RuntimeException("Nonce cannot be negative");
        }
        if (nonce < getNonce()) {
            throw new RuntimeException("Nonce is too low");
        }

        byte[] txnBase = getTxnBase((byte) 5, nonce);
        ByteBuffer buffer = ByteBuffer.allocate(txnBase.length + 8 + data.length);
        buffer.put(txnBase);
        buffer.putLong(vmId);
        buffer.put(data);

        return buffer.array();
    }
    /**
     * Returns the signed transaction of send data to a specified VM on the PWR network.
     *
     * @param vmId The ID of the VM.
     * @param data The data to be sent.
     * @param nonce The transaction count of the wallet address.
     * @return A byte array that represents a signed transaction of this method
     * @throws IOException If there's an issue with the network or stream handling.
     * @throws InterruptedException If the request is interrupted.
     * @throws RuntimeException For various transaction-related validation issues.
     */
    public byte[] getSignedSendVmDataTxn(long vmId, byte[] data, int nonce) throws IOException, InterruptedException {
        return getSignedTxn(getSendVmDataTxn(vmId, data, nonce));
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
    public Response sendVmDataTxn(long vmId, byte[] data, int nonce) {
        try {
            return PWRJ.broadcastTxn(getSignedSendVmDataTxn(vmId, data, nonce));
        } catch (Exception e) {
            return new Response(false, null, e.getMessage());
        }
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
     * Returns the transaction of sending a transaction to claim a Virtual Machine ID on the PWR network, ensuring its owner 15% revenue of all transaction fees paid when transacting with this VM.
     *
     * @param vmId The ID of the VM.
     * @param nonce The transaction count of the wallet address.
     * @return A byte array that represents the transaction of this method
     */
    public byte[] getClaimVmIdTxn(long vmId, int nonce) throws IOException {
        byte[] txnBase = getTxnBase((byte) 6, nonce);
        ByteBuffer buffer = ByteBuffer.allocate(txnBase.length + 8);
        buffer.put(txnBase);
        buffer.putLong(vmId);

        return buffer.array();
    }
    /**
     * Returns a signed transaction of sending a transaction to claim a Virtual Machine ID on the PWR network, ensuring its owner 15% revenue of all transaction fees paid when transacting with this VM.
     *
     * @param vmId The ID of the VM.
     * @param nonce The transaction count of the wallet address.
     * @return A byte array that represents a signed transaction of this method
     */
    public byte[] getSignedClaimVmIdTxn(long vmId, int nonce) throws IOException {
        return getSignedTxn(getClaimVmIdTxn(vmId, nonce));
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
    public Response claimVmId(long vmId, int nonce) throws IOException {
        return PWRJ.broadcastTxn(getSignedClaimVmIdTxn(vmId, nonce));
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
     * Returns a transaction of sending a conduit wrapped transaction of a specified VM on the PWR network. Must be sent from a conduit node of that VM.
     *
     * @param vmId The ID of the VM.
     * @param txn The transaction to be sent.
     * @param nonce The transaction count of the wallet address.
     * @return A byte array that represents the transaction of this method
     * @throws IOException If there's an issue with the network or stream handling.
     * @throws InterruptedException If the request is interrupted.
     * @throws RuntimeException For various transaction-related validation issues.
     */
    public byte[] getSendConduitTransactionTxn(long vmId, byte[] txn, int nonce) throws  IOException, InterruptedException{
        if (nonce < 0) {
            throw new RuntimeException("Nonce cannot be negative");
        }
        if (nonce < getNonce()) {
            throw new RuntimeException("Nonce is too low");
        }

        byte[] txnBase = getTxnBase((byte) 11, nonce);
        ByteBuffer buffer = ByteBuffer.allocate(txnBase.length + 8 + txn.length);
        buffer.put(txnBase);
        buffer.putLong(vmId);
        buffer.put(txn);

        return buffer.array();
    }
    /**
     * Returns a transaction of sending a conduit wrapped transaction of a specified VM on the PWR network. Must be sent from a conduit node of that VM.
     *
     * @param vmId The ID of the VM.
     * @param txn The transaction to be sent.
     * @param nonce The transaction count of the wallet address.
     * @return A byte array that represents the signed transaction of this method
     * @throws IOException If there's an issue with the network or stream handling.
     * @throws InterruptedException If the request is interrupted.
     * @throws RuntimeException For various transaction-related validation issues.
     */
    public byte[] getSignedSendConduitTransactionTxn(long vmId, byte[] txn, int nonce) throws IOException, InterruptedException{
        return getSignedTxn(getSendConduitTransactionTxn(vmId, txn, nonce));
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
        return PWRJ.broadcastTxn(getSignedSendConduitTransactionTxn(vmId, txn, nonce));
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

    /**
     * Returns a transaction of setting a guardian
     *
     * @param guardianAddress the wallet address of the chosen guardian
     * @param expiryDate the expiry date after which the guardian will have revoked privileges
     * @param nonce the transaction count of the wallet address
     * @return a byte array with the outcome of this transaction
     */
    public byte[] getSetGuardianTxn(String guardianAddress, long expiryDate, int nonce) throws IOException {
        if(guardianAddress.length() != 40 && guardianAddress.length() != 42) {
            throw new RuntimeException("Invalid address");
        } if (nonce < 0) {
            throw new RuntimeException("Nonce cannot be negative");
        } if (expiryDate < 0) {
            throw new RuntimeException("Expiry date cannot be negative");
        } if (expiryDate < Instant.now().getEpochSecond()) {
            throw new RuntimeException("Expiry date cannot be in the past");
        }

        if(guardianAddress.length() == 42) {
            guardianAddress = guardianAddress.substring(2);
        }

        byte[] txnBase = getTxnBase((byte) 8, nonce);
        ByteBuffer buffer = ByteBuffer.allocate(txnBase.length + 20 + 8);
        buffer.put(txnBase);
        buffer.putLong(expiryDate);
        buffer.put(Hex.decode(guardianAddress));

        return buffer.array();
    }
    /**
     * Returns a signed transaction of setting a guardian
     *
     * @param guardianAddress the wallet address of the chosen guardian
     * @param expiryDate the expiry date after which the guardian will have revoked privileges
     * @param nonce the transaction count of the wallet address
     * @return a byte array with the outcome of this transaction
     */
    public byte[] getSignedSetGuardianTxn(String guardianAddress, long expiryDate, int nonce) throws IOException {
        return getSignedTxn(getSetGuardianTxn(guardianAddress, expiryDate, nonce));
    }
    /**
     * Sets a guardian
     *
     * @param guardianAddress the wallet address of the chosen guardian
     * @param expiryDate the expiry date after which the guardian will have revoked privileges
     * @param nonce the transaction count of the wallet address
     * @return A Response object encapsulating the outcome of the transaction broadcast.
     *         On successful broadcast: Response(success=true, message=transactionHash, error=null).
     *         On failure: Response(success=false, message=errorMessage, error=null).
     */
    public Response setGuardian(String guardianAddress, long expiryDate, int nonce) throws IOException {
        return PWRJ.broadcastTxn(getSignedSetGuardianTxn(guardianAddress, expiryDate, nonce));
    }
    /**
     * Sets a guardian
     *
     * @param guardianAddress the wallet address of the chosen guardian
     * @param expiryDate the expiry date after which the guardian will have revoked privileges
     * @return A Response object encapsulating the outcome of the transaction broadcast.
     *         On successful broadcast: Response(success=true, message=transactionHash, error=null).
     *         On failure: Response(success=false, message=errorMessage, error=null).
     * @throws IOException if there's an issue with the network or stream handling
     * @throws InterruptedException if the request is interrupted
     */
    public Response setGuardian(String guardianAddress, long expiryDate) throws IOException, InterruptedException {
        return setGuardian(guardianAddress, expiryDate, getNonce());
    }

    /**
     * Returns the transaction of removing/revoking a guardian wallet
     *
     * @param nonce the transaction count of the wallet address
     * @return a byte array with the outcome of the transaction
     */
    public byte[] getRemoveGuardianTxn(int nonce) throws IOException {
        byte[] txnBase = getTxnBase((byte) 9, nonce);

        return txnBase;
    }
    /**
     * Returns the signed transaction of removing/revoking a guardian wallet
     *
     * @param nonce the transaction count of the wallet address
     * @return a byte array with the outcome of the transaction
     */
    public byte[] getSignedRemoveGuardianTxn(int nonce) throws IOException {
        return getSignedTxn(getRemoveGuardianTxn(nonce));
    }
    public byte[] getSignedRemoveGuardianTxn() throws IOException, InterruptedException {
        return getSignedTxn(getRemoveGuardianTxn(getNonce()));
    }
    /**
     * Removes/Revokes a guardian wallet
     *
     * @param nonce the transaction count of the wallet address
     * @return A Response object encapsulating the outcome of the transaction broadcast.
     *         On successful broadcast: Response(success=true, message=transactionHash, error=null).
     *         On failure: Response(success=false, message=null, error=errorMessage).
     */
    public Response removeGuardian(int nonce) throws IOException {
        return PWRJ.broadcastTxn(getSignedRemoveGuardianTxn(nonce));
    }
    /**
     * Removes/Revokes a guardian wallet
     *
     * @return A Response object encapsulating the outcome of the transaction broadcast.
     *         On successful broadcast: Response(success=true, message=transactionHash, error=null).
     *         On failure: Response(success=false, message=null, error=errorMessage).
     * @throws  IOException If there's an issue with the network of stream handling
     * @throws InterruptedException If hte request is interrupted
     */
    public Response removeGuardian() throws IOException, InterruptedException {
        return removeGuardian(getNonce());
    }

    /**
     * Returns the transaction for sending the guardian wallet a wrapped transaction
     *
     * @param txn The transaction to be wrapped and sent to the guardian wallet
     * @param nonce The transaction count of the wallet address
     * @return A byte array representing the transaction of this method (not to be confused with the transaction
     *         to be sent to the guardian wallet)
     */
    public byte[] getSendGuardianWrappedTransactionTxn(byte[] txn, int nonce) throws IOException {
        byte[] txnBase = getTxnBase((byte) 10, nonce);
        ByteBuffer buffer = ByteBuffer.allocate(txnBase.length + txn.length);
        buffer.put(txnBase);
        buffer.put(txn);

        return buffer.array();
    }
    /**
     * Returns the signed transaction for sending the guardian wallet a wrapped transaction
     *
     * @param txn The transaction to be wrapped and sent to the guardian wallet
     * @param nonce The transaction count of the wallet address
     * @return A byte array representing the transaction of this method (not to be confused with the transaction
     *         to be sent to the guardian wallet)
     */
    public byte[] getSignedSendGuardianWrappedTransactionTxn(byte[] txn, int nonce) throws IOException {
        return getSignedTxn(getSendGuardianWrappedTransactionTxn(txn, nonce));
    }
    /**
     * Sends the guardian wallet a wrapped transaction
     *
     * @param txn The transaction to be wrapped and sent to the guardian wallet
     * @param nonce The transaction count of the wallet address
     * @return A Response object encapsulating the outcome of the transaction broadcast.
     *         On successful broadcast: Response(success=true, message=transactionHash, error=null).
     *         On failure: Response(success=false, message=null, error=errorMessage).
     */
    public Response sendGuardianWrappedTransaction(byte[] txn, int nonce) throws IOException {
        return PWRJ.broadcastTxn(getSignedSendGuardianWrappedTransactionTxn(txn, nonce));
    }
    /**
     * Sends the guardian wallet a wrapped transaction using current nonce
     *
     * @param txn The transaction to be wrapped and sent to the guardian wallet
     * @return A Response object encapsulating the outcome of the transaction broadcast.
     *         On successful broadcast: Response(success=true, message=transactionHash, error=null).
     *         On failure: Response(success=false, message=null, error=errorMessage).
     * @throws IOException If there's an issue wih the network of stream handling
     * @throws InterruptedException If the request is interrupted
     */
    public Response sendGuardianWrappedTransaction(byte[] txn) throws IOException, InterruptedException {
        return sendGuardianWrappedTransaction(txn, getNonce());
    }

    /**
     * Returns the transaction of sending the transaction to remove validator
     *
     * @param validator
     * @return a byte array representing the transaction of this method
     * @throws IOException If there's an issue with the network or stream handling
     * @throws InterruptedException If the request is interrupted
     */
    public byte[] getSendValidatorRemoveTxn(String validator, int nonce) throws IOException {
        if(validator.length() != 40 && validator.length() != 42) {
            throw new RuntimeException("Invalid address");
        } if (nonce < 0) {
            throw new RuntimeException("Nonce cannot be negative");
        }

        if(validator.length() == 42) {
            validator = validator.substring(2);
        }

        byte[] txnBase = getTxnBase((byte) 7, nonce);
        ByteBuffer buffer = ByteBuffer.allocate(txnBase.length + 20);
        buffer.put(txnBase);
        buffer.put(Hex.decode(validator));

        return buffer.array();
    }
    /**
     * Returns the signed transaction of sending the transaction to remove validator
     *
     * @param validator
     * @return a byte array representing the transaction of this method
     * @throws IOException If there's an issue with the network or stream handling
     * @throws InterruptedException If the request is interrupted
     */
    public byte[] getSignedSendValidatorRemoveTxn(String validator, int nonce) throws IOException, InterruptedException {
        return getSignedTxn(getSendValidatorRemoveTxn(validator, nonce));
    }
    /**
     * Sends the transaction to remove validator
     *
     * @param validator
     * @return A Response object encapsulating the outcome of the transaction broadcast.
     *         On successful broadcast: Response(success=true, message=transactionHash, error=null).
     *         On failure: Response(success=false, message=null, error=errorMessage).
     * @throws IOException If there's an issue with the network or stream handling
     * @throws InterruptedException If the request is interrupted
     */
    public Response sendValidatorRemoveTxn(String validator, int nonce) throws IOException, InterruptedException {
        return PWRJ.broadcastTxn(getSignedSendValidatorRemoveTxn(validator, nonce));
    }
    /**
     * Sends the transaction to remove validator
     *
     * @param validator
     * @return A Response object encapsulating the outcome of the transaction broadcast.
     *         On successful broadcast: Response(success=true, message=transactionHash, error=null).
     *         On failure: Response(success=false, message=null, error=errorMessage).
     * @throws IOException If there's an issue with the network or stream handling
     * @throws InterruptedException If the request is interrupted
     */
    public Response sendValidatorRemoveTxn(String validator) throws IOException, InterruptedException {
        return PWRJ.broadcastTxn(getSignedSendValidatorRemoveTxn(validator, getNonce()));
    }

    /**
     * Returns the public key of the wallet of this private key
     *
     * @param privKey the private key of this wallet
     * @return a BigInteger which represents the public key of the provided private key
     */
    public static BigInteger publicKeyFromPrivate(BigInteger privKey) {
        ECPoint point = publicPointFromPrivate(privKey);
        byte[] encoded = point.getEncoded(false);
        return new BigInteger(1, Arrays.copyOfRange(encoded, 1, encoded.length));
    }

    /**
     * Returns the public ECPoint related to the provided private key
     *
     * @param privKey The private key related to the requested public ECPoint
     * @return An ECPoint object witch represents the Public point of the provided private key
     */
    public static ECPoint publicPointFromPrivate(BigInteger privKey) {
        if (privKey.bitLength() > Signature.CURVE.getN().bitLength()) {
            privKey = privKey.mod(Signature.CURVE.getN());
        }

        return (new FixedPointCombMultiplier()).multiply(Signature.CURVE.getG(), privKey);
    }

    public static void main(String[] args) {
        //Test for all the functions
        try {
            PWRWallet wallet = new PWRWallet(new BigInteger("19025338099182849188500822369817708178555441129124871592504836170414925188857"));
            System.out.println("Wallet Address: " + wallet.getAddress());
            System.exit(0);

            int nonce = wallet.getNonce();

            Response r = wallet.transferPWR("0x8953f1c3B53Bd9739F78dc8B0CD5DB9686C40b09", 1000000000, nonce);
            System.out.println("Transfer PWR success: " + r.isSuccess());
            System.out.println("Transfer PWR txn hash: " + r.getTxnHash());
            System.out.println("Transfer PWR error: " + r.getError());
            System.out.println();

            ++nonce;
            r = wallet.delegate("0x61Bd8fc1e30526Aaf1C4706Ada595d6d236d9883", 1000000000, nonce);
            System.out.println("Delegate success: " + r.isSuccess());
            System.out.println("Delegate txn hash: " + r.getTxnHash());
            System.out.println("Delegate error: " + r.getError());
            System.out.println();

            ++nonce;
            r = wallet.claimVmId(1, nonce);
            System.out.println("Claim VM ID success: " + r.isSuccess());
            System.out.println("Claim VM ID txn hash: " + r.getTxnHash());
            System.out.println("Claim VM ID error: " + r.getError());

            ++nonce;
            r = wallet.sendVmDataTxn(1, "Hello World".getBytes(), nonce);
            System.out.println("Send VM Data success: " + r.isSuccess());
            System.out.println("Send VM Data txn hash: " + r.getTxnHash());
            System.out.println("Send VM Data error: " + r.getError());

            while(PWRJ.getDelegatedPWR(wallet.getAddress(), "0x61Bd8fc1e30526Aaf1C4706Ada595d6d236d9883") == 0) {
                Thread.sleep(1000);
            }

            //TODO send this after the delegation has been completed
            ++nonce;
            r = wallet.withdraw("0x61Bd8fc1e30526Aaf1C4706Ada595d6d236d9883", 10, nonce);
            System.out.println("Withdraw success: " + r.isSuccess());
            System.out.println("Withdraw txn hash: " + r.getTxnHash());
            System.out.println("Withdraw error: " + r.getError());
            System.out.println();

            //TODO send this after the delegation has been completed
            ++nonce;
            r = wallet.withdrawPWR("0x61Bd8fc1e30526Aaf1C4706Ada595d6d236d9883", 10000000, nonce);
            System.out.println("Withdraw PWR success: " + r.isSuccess());
            System.out.println("Withdraw PWR txn hash: " + r.getTxnHash());
            System.out.println("Withdraw PWR error: " + r.getError());

        } catch (Exception e) {

        }
    }

    public static void guardianTest(PWRWallet wallet) {
        try {
            int nonce = wallet.getNonce();
            Response r = wallet.setGuardian("0x61Bd8fc1e30526Aaf1C4706Ada595d6d236d9883", 1000000000, nonce);
            System.out.println("Set Guardian success: " + r.isSuccess());
            System.out.println("Set Guardian txn hash: " + r.getTxnHash());
            System.out.println("Set Guardian error: " + r.getError());

            while(PWRJ.getGuardianOfAddress(wallet.getAddress()) == null) {
                Thread.sleep(1000);
            }

            ++nonce;
            byte[] transferTxn = wallet.getSignedTransferPWRTxn("0x61Bd8fc1e30526Aaf1C4706Ada595d6d236d9883", 1000, nonce);

            PWRWallet guardian = new PWRWallet(new BigInteger("03a5240936d67dc18dca348e793010a14c5eba86a73d0c9e45764681295a73df", 16));

            r = guardian.sendGuardianWrappedTransaction(transferTxn);
            System.out.println("Send Guardian Wrapped Transaction success: " + r.isSuccess());
            System.out.println("Send Guardian Wrapped Transaction txn hash: " + r.getTxnHash());
            System.out.println("Send Guardian Wrapped Transaction error: " + r.getError());

            Thread.sleep(5000);

            byte[] removeGuardianTxn = wallet.getSignedRemoveGuardianTxn();
            r = guardian.sendGuardianWrappedTransaction(removeGuardianTxn);
            System.out.println("Remove Guardian success: " + r.isSuccess());
            System.out.println("Remove Guardian txn hash: " + r.getTxnHash());
            System.out.println("Remove Guardian error: " + r.getError());

        } catch (Exception e) {
            System.out.println("Guardian test failed: " + e.getMessage());
        }
    }
}
