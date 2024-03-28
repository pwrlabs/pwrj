package com.github.pwrlabs.pwrj.wallet;

import com.github.pwrlabs.pwrj.Utils.Hex;
import com.github.pwrlabs.pwrj.Utils.Response;
import com.github.pwrlabs.pwrj.protocol.PWRJ;
import org.bouncycastle.jcajce.provider.digest.Keccak;
import org.bouncycastle.math.ec.FixedPointCombMultiplier;
import com.github.pwrlabs.pwrj.protocol.Signature;

import java.io.IOException;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.net.http.HttpClient;
import java.nio.ByteBuffer;

import org.bouncycastle.jce.provider.BouncyCastleProvider;
import org.bouncycastle.math.ec.ECPoint;

import java.nio.charset.StandardCharsets;
import java.security.*;
import java.time.Instant;
import java.util.Arrays;
import java.util.List;

public class PWRWallet {

    static {
        // Add BouncyCastle as a Security Provider if it's not already present
        if (Security.getProvider(BouncyCastleProvider.PROVIDER_NAME) == null) {
            Security.addProvider(new BouncyCastleProvider());
        }
    }

    private static HttpClient client = HttpClient.newHttpClient();
    private final BigInteger privateKey;
    private PWRJ pwrj;

    /**
     * Constructs a PWRWallet using a private key in string format.
     *
     * <p>This constructor converts the provided private key into {@code Credentials}
     * which can be used for various wallet operations.</p>
     *
     * @param privateKey The private key of the wallet in a string format.
     */
    public PWRWallet(String privateKey, PWRJ pwrj) {
        this.privateKey = new BigInteger(privateKey, 16);
        this.pwrj = pwrj;
    }


    /**
     * Constructs a PWRWallet using a private key in byte array format.
     *
     * <p>This constructor converts the byte array representation of the private key
     * into its hexadecimal string format, and then into {@code Credentials}.</p>
     *
     * @param privateKey The private key of the wallet in byte array format.
     */
    public PWRWallet(byte[] privateKey, PWRJ pwrj) {
        this.privateKey = new BigInteger(1, privateKey);
        this.pwrj = pwrj;
    }

    /**
     * Constructs a PWRWallet using a private key in {@code BigInteger} format.
     *
     * <p>This constructor converts the {@code BigInteger} representation of the private key
     * into its hexadecimal string format, and then into {@code Credentials}.</p>
     *
     * @param privateKey The private key of the wallet in {@code BigInteger} format.
     */
    public PWRWallet(BigInteger privateKey, PWRJ pwrj) {
        this.privateKey = privateKey;
        this.pwrj = pwrj;
    }

    /**
     * Constructs a PWRWallet using a random private key.
     *
     * <p>This constructor generates a random private key of 256 bits, and then
     * converts it into {@code Credentials}.</p>
     */
    public PWRWallet(PWRJ pwrj) {
        //Generate random private key
        this.privateKey = new BigInteger(256, new SecureRandom());
        this.pwrj = pwrj;
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
        return pwrj.getBalanceOfAddress(getAddress());
    }

    /**
     * Retrieves the nonce (number of transactions) of the wallet's address from the PWR network.
     *
     * @return The nonce of the wallet's address.
     * @throws IOException If there's an issue with the network or stream handling.
     * @throws InterruptedException If the request is interrupted.
     */
    public int getNonce() throws IOException {
        return pwrj.getNonceOfAddress(getAddress());
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

    public byte[] getSignedTransaction(byte[] Transaction) {
        if(Transaction == null) return null;

        byte[] signature = Signature.signMessage(Transaction, privateKey);

        ByteBuffer finalTransaction = ByteBuffer.allocate(Transaction.length + 65);
        finalTransaction.put(Transaction);
        finalTransaction.put(signature);

        return finalTransaction.array();
    }

    public byte[] getTransactionBase(byte identifier, int nonce) throws IOException {
        ByteBuffer buffer = ByteBuffer.allocate(6);
        buffer.put(identifier);
        buffer.put(pwrj.getChainId());
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
    public byte[] getTransferPWRTransaction(String to, long amount, int nonce) throws IOException {
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

        byte[] TransactionBase = getTransactionBase((byte) 0, nonce);
        ByteBuffer buffer = ByteBuffer.allocate(TransactionBase.length + 8 + 20);
        buffer.put(TransactionBase);
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
    public byte[] getSignedTransferPWRTransaction(String to, long amount, int nonce) throws IOException {
        return getSignedTransaction(getTransferPWRTransaction(to, amount, nonce));
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
        return pwrj.broadcastTransaction(getSignedTransferPWRTransaction(to, amount, nonce));
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
    public byte[] getJoinTransaction(String ip, int nonce) throws IOException {
        byte[] TransactionBase = getTransactionBase((byte) 1, nonce);
        byte[] ipBytes = ip.getBytes(StandardCharsets.UTF_8);

        ByteBuffer buffer = ByteBuffer.allocate(TransactionBase.length + ipBytes.length);
        buffer.put(TransactionBase);
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
    public byte[] getSignedJoinTransaction(String ip, int nonce) throws IOException {
        return getSignedTransaction(getJoinTransaction(ip, nonce));
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
        return pwrj.broadcastTransaction(getSignedJoinTransaction(ip, nonce));
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
    public byte[] getClaimActiveNodeSpotTransaction(int nonce) throws IOException {
        byte[] TransactionBase = getTransactionBase((byte) 2, nonce);

        return TransactionBase;
    }
    /**
     * Returns the signed transaction of Claim an active node spot on the PWR network.
     *
     * @param nonce The transaction count of the wallet address.
     * @return A byte array that represents the signed transaction of this method
     */
    public byte[] getSignedClaimActiveNodeSpotTransaction(int nonce) throws IOException {
        return getSignedTransaction(getClaimActiveNodeSpotTransaction(nonce));
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
        return pwrj.broadcastTransaction(getSignedClaimActiveNodeSpotTransaction(nonce));
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
    public byte[] getDelegateTransaction(String to, long amount, int nonce) throws IOException {
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

        byte[] TransactionBase = getTransactionBase((byte) 3, nonce);
        ByteBuffer buffer = ByteBuffer.allocate(TransactionBase.length + 8 + (to.length()/2));
        buffer.put(TransactionBase);
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
    public byte[] getSignedDelegateTransaction(String to, long amount, int nonce) throws IOException {
        return getSignedTransaction(getDelegateTransaction(to, amount, nonce));
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
        return pwrj.broadcastTransaction(getSignedDelegateTransaction(to, amount, nonce));
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
    public byte[] getWithdrawTransaction(String from, long sharesAmount, int nonce) throws IOException {
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

        byte[] TransactionBase = getTransactionBase((byte) 4, nonce);
        ByteBuffer buffer = ByteBuffer.allocate(TransactionBase.length + 8 + (from.length()/2));
        buffer.put(TransactionBase);
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
    public byte[] getSignedWithdrawTransaction(String from, long sharesAmount, int nonce) throws IOException {
        return getSignedTransaction(getWithdrawTransaction(from, sharesAmount, nonce));
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
        return pwrj.broadcastTransaction(getSignedWithdrawTransaction(from, sharesAmount, nonce));
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
    public byte[] getWithdrawPWRTransaction(String from, long pwrAmount, int nonce) throws IOException {
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

        BigDecimal shareValue = pwrj.getShareValue(from);
        long sharesAmount = BigDecimal.valueOf(pwrAmount).divide(shareValue, 18, BigDecimal.ROUND_DOWN).longValue();

        if(sharesAmount <= 0) {
            throw new RuntimeException("Shares amount is too low");
        }

        byte[] TransactionBase = getTransactionBase((byte) 4, nonce);
        ByteBuffer buffer = ByteBuffer.allocate(TransactionBase.length + 8 + (from.length()/2));
        buffer.put(TransactionBase);
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
    public byte[] getSignedWithdrawPWRTransaction(String from, long pwrAmount, int nonce) throws IOException {
        return getSignedTransaction(getWithdrawPWRTransaction(from, pwrAmount, nonce));
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
        return pwrj.broadcastTransaction(getSignedWithdrawPWRTransaction(from, pwrAmount, nonce));
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
        return withdrawPWR(from, pwrAmount, getNonce());
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
    public byte[] getVmDataTransaction(long vmId, byte[] data, int nonce) throws IOException, InterruptedException{
        if (nonce < 0) {
            throw new RuntimeException("Nonce cannot be negative");
        }
        if (nonce < getNonce()) {
            throw new RuntimeException("Nonce is too low");
        }

        byte[] TransactionBase = getTransactionBase((byte) 5, nonce);
        ByteBuffer buffer = ByteBuffer.allocate(TransactionBase.length + 8 + data.length);
        buffer.put(TransactionBase);
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
    public byte[] getSignedVmDataTransaction(long vmId, byte[] data, int nonce) throws IOException, InterruptedException {
        return getSignedTransaction(getVmDataTransaction(vmId, data, nonce));
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
    public Response sendVmDataTransaction(long vmId, byte[] data, int nonce) {
        try {
            return pwrj.broadcastTransaction(getSignedVmDataTransaction(vmId, data, nonce));
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
    public Response sendVmDataTransaction(long vmId, byte[] data) throws IOException, InterruptedException {
        return sendVmDataTransaction(vmId, data, getNonce());
    }

    /**
     * Returns the transaction of sending a transaction to claim a Virtual Machine ID on the PWR network, ensuring its owner 15% revenue of all transaction fees paid when transacting with this VM.
     *
     * @param vmId The ID of the VM.
     * @param nonce The transaction count of the wallet address.
     * @return A byte array that represents the transaction of this method
     */
    public byte[] getClaimVmIdTransaction(long vmId, int nonce) throws IOException {
        byte[] TransactionBase = getTransactionBase((byte) 6, nonce);
        ByteBuffer buffer = ByteBuffer.allocate(TransactionBase.length + 8);
        buffer.put(TransactionBase);
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
    public byte[] getSignedClaimVmIdTransaction(long vmId, int nonce) throws IOException {
        return getSignedTransaction(getClaimVmIdTransaction(vmId, nonce));
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
        return pwrj.broadcastTransaction(getSignedClaimVmIdTransaction(vmId, nonce));
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
     * Returns a transaction of setting a guardian
     *
     * @param guardianAddress the wallet address of the chosen guardian
     * @param expiryDate the expiry date after which the guardian will have revoked privileges
     * @param nonce the transaction count of the wallet address
     * @return a byte array with the outcome of this transaction
     */
    public byte[] getSetGuardianTransaction(String guardianAddress, long expiryDate, int nonce) throws IOException {
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

        byte[] TransactionBase = getTransactionBase((byte) 8, nonce);
        ByteBuffer buffer = ByteBuffer.allocate(TransactionBase.length + 20 + 8);
        buffer.put(TransactionBase);
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
    public byte[] getSignedSetGuardianTransaction(String guardianAddress, long expiryDate, int nonce) throws IOException {
        return getSignedTransaction(getSetGuardianTransaction(guardianAddress, expiryDate, nonce));
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
        return pwrj.broadcastTransaction(getSignedSetGuardianTransaction(guardianAddress, expiryDate, nonce));
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
    public byte[] getRemoveGuardianTransaction(int nonce) throws IOException {
        byte[] TransactionBase = getTransactionBase((byte) 9, nonce);

        return TransactionBase;
    }
    /**
     * Returns the signed transaction of removing/revoking a guardian wallet
     *
     * @param nonce the transaction count of the wallet address
     * @return a byte array with the outcome of the transaction
     */
    public byte[] getSignedRemoveGuardianTransaction(int nonce) throws IOException {
        return getSignedTransaction(getRemoveGuardianTransaction(nonce));
    }
    public byte[] getSignedRemoveGuardianTransaction() throws IOException, InterruptedException {
        return getSignedTransaction(getRemoveGuardianTransaction(getNonce()));
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
        return pwrj.broadcastTransaction(getSignedRemoveGuardianTransaction(nonce));
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
     * @param transactions The transactions to be wrapped and sent to the guardian wallet
     * @param nonce The transaction count of the wallet address
     * @return A byte array representing the transaction of this method (not to be confused with the transaction
     *         to be sent to the guardian wallet)
     */
    public byte[] getGuardianWrappedTransaction(List<byte[]> transactions, int nonce) throws IOException {
        int totalLength = 0;
        for (byte[] Transaction : transactions) {
            totalLength += Transaction.length;
        }

        byte[] TransactionBase = getTransactionBase((byte) 10, nonce);
        ByteBuffer buffer = ByteBuffer.allocate(TransactionBase.length + (transactions.size() * 4) + totalLength);
        buffer.put(TransactionBase);

        for (byte[] Transaction : transactions) {
            buffer.putInt(Transaction.length);
            buffer.put(Transaction);
        }

        return buffer.array();
    }
    /**
     * Returns the signed transaction for sending the guardian wallet a wrapped transaction
     *
     * @param transactions List of transactions to be wrapped and sent to the guardian wallet
     * @param nonce The transaction count of the wallet address
     * @return A byte array representing the transaction of this method (not to be confused with the transaction
     *         to be sent to the guardian wallet)
     */
    public byte[] getSignedGuardianWrappedTransactionTransaction(List<byte[]> transactions, int nonce) throws IOException {
        return getSignedTransaction(getGuardianWrappedTransaction(transactions, nonce));
    }
    /**
     * Sends the guardian wallet a wrapped transaction
     *
     * @param transactions List of transactions to be wrapped and sent to the guardian wallet
     * @param nonce The transaction count of the wallet address
     * @return A Response object encapsulating the outcome of the transaction broadcast.
     *         On successful broadcast: Response(success=true, message=transactionHash, error=null).
     *         On failure: Response(success=false, message=null, error=errorMessage).
     */
    public Response sendGuardianWrappedTransaction(List<byte[]> transactions, int nonce) throws IOException {
        return pwrj.broadcastTransaction(getSignedGuardianWrappedTransactionTransaction(transactions, nonce));
    }
    /**
     * Sends the guardian wallet a wrapped transaction using current nonce
     *
     * @param transactions List of transactions to be wrapped and sent to the guardian wallet
     * @return A Response object encapsulating the outcome of the transaction broadcast.
     *         On successful broadcast: Response(success=true, message=transactionHash, error=null).
     *         On failure: Response(success=false, message=null, error=errorMessage).
     * @throws IOException If there's an issue wih the network of stream handling
     * @throws InterruptedException If the request is interrupted
     */
    public Response sendGuardianWrappedTransaction(List<byte[]> transactions) throws IOException, InterruptedException {
        return sendGuardianWrappedTransaction(transactions, getNonce());
    }


    /**
     * Returns the transaction of send payable data to a specified VM on the PWR network.
     *
     * @param vmId The ID of the VM.
     * @param value The amount of PWR tokens to be sent.
     * @param data The data to be sent.
     * @param nonce The transaction count of the wallet address.
     * @return A byte array that represents the transaction of this method
     * @throws IOException If there's an issue with the network or stream handling.
     * @throws InterruptedException If the request is interrupted.
     * @throws RuntimeException For various transaction-related validation issues.
     */
    public byte[] getPayableVmDataTransaction(long vmId, long value, byte[] data, int nonce) throws IOException, InterruptedException{
        if (nonce < 0) {
            throw new RuntimeException("Nonce cannot be negative");
        }
        if (nonce < getNonce()) {
            throw new RuntimeException("Nonce is too low");
        }

        byte[] TransactionBase = getTransactionBase((byte) 11, nonce);
        ByteBuffer buffer = ByteBuffer.allocate(TransactionBase.length + 16 + data.length);
        buffer.put(TransactionBase);
        buffer.putLong(vmId);
        buffer.put(data);
        buffer.putLong(value);

        return buffer.array();
    }
    /**
     * Returns the signed transaction of send payable data to a specified VM on the PWR network.
     *
     * @param vmId The ID of the VM.
     * @param value The amount of PWR tokens to be sent.
     * @param data The data to be sent.
     * @param nonce The transaction count of the wallet address.
     * @return A byte array that represents a signed transaction of this method
     * @throws IOException If there's an issue with the network or stream handling.
     * @throws InterruptedException If the request is interrupted.
     * @throws RuntimeException For various transaction-related validation issues.
     */
    public byte[] getSignedPayableVmDataTransaction(long vmId, long value, byte[] data, int nonce) throws IOException, InterruptedException {
        return getSignedTransaction(getPayableVmDataTransaction(vmId, value, data, nonce));
    }
    /**
     * Sends data and PWR coins to a specified VM on the PWR network.
     *
     * @param vmId The ID of the VM.
     * @param value The amount of PWR tokens to be sent.
     * @param data The data to be sent.
     * @param nonce The transaction count of the wallet address.
     * @return A Response object encapsulating the outcome of the transaction broadcast.
     *         On successful broadcast: Response(success=true, message=transactionHash, error=null).
     *         On failure: Response(success=false, message=null, error=errorMessage).
     * @throws IOException If there's an issue with the network or stream handling.
     * @throws InterruptedException If the request is interrupted.
     * @throws RuntimeException For various transaction-related validation issues.
     */
    public Response sendPayableVmDataTransaction(long vmId, long value, byte[] data, int nonce) {
        try {
            return pwrj.broadcastTransaction(getSignedPayableVmDataTransaction(vmId, value, data, nonce));
        } catch (Exception e) {
            return new Response(false, null, e.getMessage());
        }
    }

    /**
     * Sends data and PWR coins to a specified VM on the PWR network using the current nonce.
     *
     * @param vmId The ID of the VM.
     * @param value The amount of PWR tokens to be sent.
     * @param data The data to be sent.
     * @return A Response object encapsulating the outcome of the transaction broadcast.
     *         On successful broadcast: Response(success=true, message=transactionHash, error=null).
     *         On failure: Response(success=false, message=null, error=errorMessage).
     * @throws IOException If there's an issue with the network or stream handling.
     * @throws InterruptedException If the request is interrupted.
     */
    public Response sendPayableVmDataTransaction(long vmId, long value, byte[] data) throws IOException, InterruptedException {
        return sendPayableVmDataTransaction(vmId, value, data, getNonce());
    }


    /**
     * Returns the transaction of sending the transaction to remove validator
     *
     * @param validator
     * @return a byte array representing the transaction of this method
     * @throws IOException If there's an issue with the network or stream handling
     * @throws InterruptedException If the request is interrupted
     */
    public byte[] getValidatorRemoveTransaction(String validator, int nonce) throws IOException {
        if(validator.length() != 40 && validator.length() != 42) {
            throw new RuntimeException("Invalid address");
        } if (nonce < 0) {
            throw new RuntimeException("Nonce cannot be negative");
        }

        if(validator.length() == 42) {
            validator = validator.substring(2);
        }

        byte[] TransactionBase = getTransactionBase((byte) 7, nonce);
        ByteBuffer buffer = ByteBuffer.allocate(TransactionBase.length + 20);
        buffer.put(TransactionBase);
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
    public byte[] getSignedValidatorRemoveTransaction(String validator, int nonce) throws IOException, InterruptedException {
        return getSignedTransaction(getValidatorRemoveTransaction(validator, nonce));
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
    public Response sendValidatorRemoveTransaction(String validator, int nonce) throws IOException, InterruptedException {
        return pwrj.broadcastTransaction(getSignedValidatorRemoveTransaction(validator, nonce));
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
    public Response sendValidatorRemoveTransaction(String validator) throws IOException, InterruptedException {
        return pwrj.broadcastTransaction(getSignedValidatorRemoveTransaction(validator, getNonce()));
    }

    public byte[] getConduitApprovalTransaction(long vmId, List<byte[]> Transactions, int nonce) throws  IOException, InterruptedException{
        if (nonce < 0) {
            throw new RuntimeException("Nonce cannot be negative");
        }
        if(Transactions.size() == 0) {
            throw new RuntimeException("No transactions to approve");
        }

        int totalTransactionsLength = 0;
        for(byte[] Transaction : Transactions) {
            totalTransactionsLength += Transaction.length;
        }

        byte[] TransactionBase = getTransactionBase((byte) 12, nonce);
        ByteBuffer buffer = ByteBuffer.allocate(TransactionBase.length + 8 + (Transactions.size() * 4) + totalTransactionsLength);
        buffer.put(TransactionBase);
        buffer.putLong(vmId);

        for(byte[] Transaction : Transactions) {
            buffer.putInt(Transaction.length);
            buffer.put(Transaction);
        }

        return buffer.array();
    }
    /**
     * Returns a transaction of sending a conduit wrapped transaction of a specified VM on the PWR network. Must be sent from a conduit node of that VM.
     *
     * @param vmId The ID of the VM.
     * @param transactions The transaction to be sent.
     * @param nonce The transaction count of the wallet address.
     * @return A byte array that represents the signed transaction of this method
     * @throws IOException If there's an issue with the network or stream handling.
     * @throws InterruptedException If the request is interrupted.
     * @throws RuntimeException For various transaction-related validation issues.
     */
    public byte[] getSignedConduitApprovalTransaction(long vmId, List<byte[]> transactions, int nonce) throws IOException, InterruptedException{
        return getSignedTransaction(getConduitApprovalTransaction(vmId, transactions, nonce));
    }
    /**
     * Sends a conduit wrapped transaction of a specified VM on the PWR network. Must be sent from a conduit node of that VM.
     *
     * @param vmId The ID of the VM.
     * @param transactions The transactions to be sent.
     * @param nonce The transaction count of the wallet address.
     * @return A Response object encapsulating the outcome of the transaction broadcast.
     *         On successful broadcast: Response(success=true, message=transactionHash, error=null).
     *         On failure: Response(success=false, message=errorMessage, error=null).
     * @throws IOException If there's an issue with the network or stream handling.
     * @throws InterruptedException If the request is interrupted.
     * @throws RuntimeException For various transaction-related validation issues.
     */
    public Response conduitApprove(long vmId, List<byte[]> transactions, int nonce) throws IOException, InterruptedException {
        return pwrj.broadcastTransaction(getSignedConduitApprovalTransaction(vmId, transactions, nonce));
    }
    /**
     * Sends a conduit wrapped transaction of a specified VM on the PWR network using the current nonce. Must be sent from a conduit node of that VM.
     *
     * @param vmId The ID of the VM.
     * @param transactions The transactions to be sent.
     * @return A Response object encapsulating the outcome of the transaction broadcast.
     *         On successful broadcast: Response(success=true, message=transactionHash, error=null).
     *         On failure: Response(success=false, message=errorMessage, error=null).
     * @throws IOException If there's an issue with the network or stream handling.
     * @throws InterruptedException If the request is interrupted.
     */
    public Response conduitApprove(long vmId, List<byte[]> transactions) throws IOException, InterruptedException {
        return conduitApprove(vmId, transactions, getNonce());
    }

    public byte[] getSetConduitsTransaction(long vmId, List<byte[]> conduits, int nonce) throws  IOException, InterruptedException{
        if (nonce < 0) {
            throw new RuntimeException("Nonce cannot be negative");
        }
        if(conduits.size() == 0) {
            throw new RuntimeException("No transactions to approve");
        }

        int totalConduitLength = 0;
        for(byte[] conduit : conduits) {
            totalConduitLength += conduit.length;
        }

        byte[] TransactionBase = getTransactionBase((byte) 13, nonce);
        ByteBuffer buffer = ByteBuffer.allocate(TransactionBase.length + 8 + (conduits.size() * 4) + totalConduitLength);
        buffer.put(TransactionBase);
        buffer.putLong(vmId);

        for(byte[] conduit : conduits) {
            buffer.putInt(conduit.length);
            buffer.put(conduit);
        }

        return buffer.array();
    }
    public byte[] getSignedSetConduitTransaction(long vmId, List<byte[]> conduits, int nonce) throws IOException, InterruptedException{
        return getSignedTransaction(getSetConduitsTransaction(vmId, conduits, nonce));
    }
    public Response setConduits(long vmId, List<byte[]> conduits, int nonce) throws IOException, InterruptedException {
        return pwrj.broadcastTransaction(getSignedSetConduitTransaction(vmId, conduits, nonce));
    }
    public Response setConduits(long vmId, List<byte[]> conduits) throws IOException, InterruptedException {
        return setConduits(vmId, conduits, getNonce());
    }

    public byte[] getAddConduitsTransaction(long vmId, List<byte[]> conduits, int nonce) throws  IOException, InterruptedException{
        if (nonce < 0) {
            throw new RuntimeException("Nonce cannot be negative");
        }
        if(conduits.size() == 0) {
            throw new RuntimeException("No transactions to approve");
        }

        int totalConduitLength = 0;
        for(byte[] conduit : conduits) {
            totalConduitLength += conduit.length;
        }

        byte[] TransactionBase = getTransactionBase((byte) 14, nonce);
        ByteBuffer buffer = ByteBuffer.allocate(TransactionBase.length + 8 + (conduits.size() * 4) + totalConduitLength);
        buffer.put(TransactionBase);
        buffer.putLong(vmId);

        for(byte[] conduit : conduits) {
            buffer.putInt(conduit.length);
            buffer.put(conduit);
        }

        return buffer.array();
    }
    public byte[] getSignedAddConduitTransaction(long vmId, List<byte[]> conduits, int nonce) throws IOException, InterruptedException{
        return getSignedTransaction(getAddConduitsTransaction(vmId, conduits, nonce));
    }
    public Response addConduits(long vmId, List<byte[]> conduits, int nonce) throws IOException, InterruptedException {
        return pwrj.broadcastTransaction(getSignedAddConduitTransaction(vmId, conduits, nonce));
    }
    public Response addConduits(long vmId, List<byte[]> conduits) throws IOException, InterruptedException {
        return addConduits(vmId, conduits, getNonce());
    }

    public byte[] getRemoveConduitsTransaction(long vmId, List<byte[]> conduits, int nonce) throws  IOException, InterruptedException{
        if (nonce < 0) {
            throw new RuntimeException("Nonce cannot be negative");
        }
        if(conduits.size() == 0) {
            throw new RuntimeException("No transactions to approve");
        }

        int totalConduitLength = 0;
        for(byte[] conduit : conduits) {
            totalConduitLength += conduit.length;
        }

        byte[] TransactionBase = getTransactionBase((byte) 15, nonce);
        ByteBuffer buffer = ByteBuffer.allocate(TransactionBase.length + 8 + (conduits.size() * 4) + totalConduitLength);
        buffer.put(TransactionBase);
        buffer.putLong(vmId);

        for(byte[] conduit : conduits) {
            buffer.putInt(conduit.length);
            buffer.put(conduit);
        }

        return buffer.array();
    }
    public byte[] getSignedRemoveConduitTransaction(long vmId, List<byte[]> conduits, int nonce) throws IOException, InterruptedException{
        return getSignedTransaction(getAddConduitsTransaction(vmId, conduits, nonce));
    }
    public Response removeConduits(long vmId, List<byte[]> conduits, int nonce) throws IOException, InterruptedException {
        return pwrj.broadcastTransaction(getSignedRemoveConduitTransaction(vmId, conduits, nonce));
    }
    public Response removeConduits(long vmId, List<byte[]> conduits) throws IOException, InterruptedException {
        return removeConduits(vmId, conduits, getNonce());
    }


    public byte[] getMoveStakeTransaction(long sharesAmount, byte[] fromValidator, byte[] toValidator, int nonce) throws IOException {
        byte[] TransactionBase = getTransactionBase((byte) 16, nonce);
        ByteBuffer buffer = ByteBuffer.allocate(TransactionBase.length + 8 + fromValidator.length + toValidator.length);
        buffer.put(TransactionBase);
        buffer.putLong(sharesAmount);
        buffer.put(fromValidator);
        buffer.put(toValidator);

        return buffer.array();
    }
    public byte[] getMoveStakeTransaction(long sharesAmount, String fromValidator, String toValidator, int nonce) throws IOException {
        return getMoveStakeTransaction(sharesAmount, Hex.decode(fromValidator), Hex.decode(toValidator), nonce);
    }
    public byte[] getSignedMoveStakeTransaction(long sharesAmount, byte[] fromValidator, byte[] toValidator, int nonce) throws IOException {
        return getSignedTransaction(getMoveStakeTransaction(sharesAmount, fromValidator, toValidator, nonce));
    }
    public byte[] getSignedMoveStakeTransaction(long sharesAmount, String fromValidator, String toValidator, int nonce) throws IOException {
        return getSignedTransaction(getMoveStakeTransaction(sharesAmount, fromValidator, toValidator, nonce));
    }
    public Response moveStake(long sharesAmount, byte[] fromValidator, byte[] toValidator, int nonce) throws IOException {
        return pwrj.broadcastTransaction(getSignedMoveStakeTransaction(sharesAmount, fromValidator, toValidator, nonce));
    }
    public Response moveStake(long sharesAmount, String fromValidator, String toValidator, int nonce) throws IOException {
        return moveStake(sharesAmount, Hex.decode(fromValidator), Hex.decode(toValidator), nonce);
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
}
