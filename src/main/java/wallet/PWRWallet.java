package wallet;

import org.bouncycastle.util.encoders.Hex;
import org.web3j.crypto.Credentials;
import org.web3j.crypto.ECKeyPair;
import protocol.PWRJ;
import protocol.Signature;

import java.io.IOException;
import java.math.BigInteger;
import java.net.http.HttpClient;
import java.nio.ByteBuffer;

public class PWRWallet {
    private static HttpClient client = HttpClient.newHttpClient();

    private final Credentials credentials;

    /**
     * Constructs a PWRWallet using a private key in string format.
     *
     * <p>This constructor converts the provided private key into {@code Credentials}
     * which can be used for various wallet operations.</p>
     *
     * @param privateKey The private key of the wallet in a string format.
     */
    public PWRWallet(String privateKey) {
        credentials = Credentials.create(privateKey);
    }

    /**
     * Constructs a PWRWallet using an existing {@code Credentials} object.
     *
     * <p>Use this constructor if you already have the credentials object and want to
     * wrap it within the PWRWallet.</p>
     *
     * @param credentials The credentials object representing the wallet.
     */
    public PWRWallet(Credentials credentials) {
        this.credentials = credentials;
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
        credentials = Credentials.create(Hex.toHexString(privateKey));
    }

    /**
     * Constructs a PWRWallet using an ECKeyPair object.
     *
     * <p>Use this constructor if you have an elliptic curve key pair and want to
     * create a wallet from it.</p>
     *
     * @param ecKeyPair The elliptic curve key pair representing the wallet's keys.
     */
    public PWRWallet(ECKeyPair ecKeyPair) {
        credentials = Credentials.create(ecKeyPair);
    }


    /**
     * Retrieves the address associated with the wallet's credentials.
     *
     * @return The address in string format.
     */
    public String getAddress() {
        return credentials.getAddress();
    }

    /**
     * Fetches the balance of the current wallet's address from the PWR network.
     *
     * @return The balance associated with the wallet's address.
     * @throws IOException If there's an issue with the network or stream handling.
     * @throws InterruptedException If the request is interrupted.
     */
    public long getBalance() throws IOException, InterruptedException {
        return PWRJ.getBalanceOfAddress(credentials.getAddress());
    }

    /**
     * Retrieves the nonce (number of transactions) of the wallet's address from the PWR network.
     *
     * @return The nonce of the wallet's address.
     * @throws IOException If there's an issue with the network or stream handling.
     * @throws InterruptedException If the request is interrupted.
     */
    public int getNonce() throws IOException, InterruptedException {
        return PWRJ.getNonceOfAddress(credentials.getAddress());
    }

    /**
     * Retrieves the private key associated with the wallet's credentials.
     *
     * <p>Note: Exposing private keys can pose security risks. Ensure that this method
     * is used judiciously and data is kept secure.</p>
     *
     * @return The private key of the wallet in {@code BigInteger} format.
     * @throws IOException If there's an issue with network or stream handling.
     * @throws InterruptedException If the method execution is interrupted.
     */
    public BigInteger getPrivateKey() throws IOException, InterruptedException {
        return credentials.getEcKeyPair().getPrivateKey();
    }


    /**
     * Transfers PWR tokens to a specified address.
     *
     * @param to The recipient address.
     * @param amount The amount of PWR tokens to be transferred.
     * @param nonce The transaction count of the wallet address.
     * @return A string representation of the transaction status or hash.
     * @throws IOException If there's an issue with the network or stream handling.
     * @throws InterruptedException If the request is interrupted.
     * @throws RuntimeException For various transaction-related validation issues.
     */
    public String transferPWR(String to, long amount, int nonce) throws IOException, InterruptedException {
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
        byte[] signature = Signature.signMessage(txn, credentials.getEcKeyPair());

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
     * @return A string representation of the transaction status or hash.
     * @throws IOException If there's an issue with the network or stream handling.
     * @throws InterruptedException If the request is interrupted.
     */
    public String transferPWR(String to, long amount) throws IOException, InterruptedException {
        return transferPWR(to, amount, getNonce());
    }

    /**
     * Sends data to a specified VM on the PWR network.
     *
     * @param vmId The ID of the VM.
     * @param data The data to be sent.
     * @param nonce The transaction count of the wallet address.
     * @return A string representation of the transaction status or hash.
     * @throws IOException If there's an issue with the network or stream handling.
     * @throws InterruptedException If the request is interrupted.
     * @throws RuntimeException For various transaction-related validation issues.
     */
    public String sendVmDataTxn(long vmId, byte[] data, int nonce) throws IOException, InterruptedException {
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
        byte[] signature = Signature.signMessage(txn, credentials.getEcKeyPair());

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
     * @return A string representation of the transaction status or hash.
     * @throws IOException If there's an issue with the network or stream handling.
     * @throws InterruptedException If the request is interrupted.
     */
    public String sendVmDataTxn(long vmId, byte[] data) throws IOException, InterruptedException {
        return sendVmDataTxn(vmId, data, getNonce());
    }
}
