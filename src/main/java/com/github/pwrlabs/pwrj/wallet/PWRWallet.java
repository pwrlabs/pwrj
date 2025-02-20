package com.github.pwrlabs.pwrj.wallet;

import com.github.pwrlabs.pwrj.Utils.AES256;
import com.github.pwrlabs.pwrj.Utils.Hex;
import com.github.pwrlabs.pwrj.protocol.PWRJ;
import com.github.pwrlabs.pwrj.protocol.TransactionBuilder;
import com.github.pwrlabs.pwrj.record.response.Response;
import com.github.pwrlabs.pwrj.record.transaction.ecdsa.VmDataTransaction;
import org.bouncycastle.jcajce.provider.digest.Keccak;
import org.bouncycastle.math.ec.FixedPointCombMultiplier;
import com.github.pwrlabs.pwrj.protocol.Signature;

import java.io.IOException;
import java.math.BigInteger;
import java.net.http.HttpClient;
import java.nio.ByteBuffer;

import org.bouncycastle.jce.provider.BouncyCastleProvider;
import org.bouncycastle.math.ec.ECPoint;
import org.json.JSONObject;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.*;
import java.util.Arrays;
import java.util.List;

public class PWRWallet {

    static {
        // Add BouncyCastle as a Security Provider if it's not already present
        if (Security.getProvider(BouncyCastleProvider.PROVIDER_NAME) == null) {
            Security.addProvider(new BouncyCastleProvider());
        }
    }

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

    public void storeWallet(String path, String password) throws Exception {
        byte[] privateKeyBytes = privateKey.toByteArray();
        byte[] encryptedPrivateKey = AES256.encrypt(privateKeyBytes, password);

        Files.write(Path.of(path), encryptedPrivateKey);
    }

    public BigInteger getPublicKey() {
        return publicKeyFromPrivate(privateKey);
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


    /**
     * Returns the signed transaction of Transfer PWR tokens to a specified address.
     *
     * @param to The recipient address.
     * @param amount The amount of PWR tokens to be transferred.
     * @param nonce The transaction count of the wallet address.
     * @return A byte array which represents the signed transaction of this method
     * @throws RuntimeException For various transaction-related validation issues.
     */
    public byte[] getSignedTransferPWRTransaction(String to, long amount, int nonce) {
        return getSignedTransaction(TransactionBuilder.getTransferPWRTransaction(to, amount, nonce, pwrj.getChainId()));
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
     * @throws RuntimeException For various transaction-related validation issues.
     */
    public Response transferPWR(String to, long amount, int nonce) {
        return pwrj.broadcastTransaction(getSignedTransferPWRTransaction(to, amount, nonce));
    }


    /**
     * Returns a signed transaction of joining the PWR network as a standby validator.
     *
     * @param ip The IP address of the validator.
     * @param nonce The transaction count of the wallet address.
     * @return A byte array that represents a signed transaction of this method
     */
    public byte[] getSignedJoinTransaction(String ip, int nonce) {
        return getSignedTransaction(TransactionBuilder.getJoinTransaction(ip, nonce, pwrj.getChainId()));
    }
    /**
     * Sends a transaction to join the PWR network as a standby validator.
     *
     * @param ip The IP address of the validator.
     * @param nonce The transaction count of the wallet address.
     * @return A Response object encapsulating the outcome of the transaction broadcast.
     *         On successful broadcast: Response(success=true, message=transactionHash, error=null).
     *         On failure: Response(success=false, message=null, error=errorMessage).
     */
    public Response join(String ip, int nonce) {
        return pwrj.broadcastTransaction(getSignedJoinTransaction(ip, nonce));
    }


    /**
     * Returns the signed transaction of Claim an active node spot on the PWR network.
     *
     * @param nonce The transaction count of the wallet address.
     * @return A byte array that represents the signed transaction of this method
     */
    public byte[] getSignedClaimActiveNodeSpotTransaction(int nonce) {
        return getSignedTransaction(TransactionBuilder.getClaimActiveNodeSpotTransaction(nonce, pwrj.getChainId()));
    }
    /**
     * Sends a transaction to claim an active node spot on the PWR network.
     *
     * @param nonce The transaction count of the wallet address.
     * @return A Response object encapsulating the outcome of the transaction broadcast.
     *         On successful broadcast: Response(success=true, message=transactionHash, error=null).
     *         On failure: Response(success=false, message=null, error=errorMessage).
     */
    public Response claimActiveNodeSpot(int nonce) {
        return pwrj.broadcastTransaction(getSignedClaimActiveNodeSpotTransaction(nonce));
    }



    /**
     * Returns the signed transaction of delegate PWR tokens to a specified validator.
     *
     * @param validator The validator address.
     * @param amount The amount of PWR tokens to be delegated.
     * @param nonce The transaction count of the wallet address.
     * @return A byte array that represents a signed transaction of the method
     */
    public byte[] getSignedDelegateTransaction(String validator, long amount, int nonce) throws IOException {
        return getSignedTransaction(TransactionBuilder.getDelegateTransaction(validator, amount, nonce, pwrj.getChainId()));
    }
    /**
     * Sends transaction to delegate PWR tokens to a specified validator.
     *
     * @param validator The validator address.
     * @param amount The amount of PWR tokens to be delegated.
     * @param nonce The transaction count of the wallet address.
     * @return A Response object encapsulating the outcome of the transaction broadcast.
     *         On successful broadcast: Response(success=true, message=transactionHash, error=null).
     *         On failure: Response(success=false, message=null, error=errorMessage).
     */
    public Response delegate(String validator, long amount, int nonce) throws IOException {
        return pwrj.broadcastTransaction(getSignedDelegateTransaction(validator, amount, nonce));
    }



    /**
     * Returns the signed transaction of withdraw PWR tokens from a specified validator.
     *
     * @param validator The validator address.
     * @param sharesAmount The amount of shares to be withdrawn.
     * @param nonce The transaction count of the wallet address.
     * @return A byte array that represents the signed transaction of this method
     */
    public byte[] getSignedWithdrawTransaction(String validator, long sharesAmount, int nonce) {
        return getSignedTransaction(TransactionBuilder.getWithdrawTransaction(validator, sharesAmount, nonce, pwrj.getChainId()));
    }
    /**
     * Sends a transaction to withdraw PWR tokens from a specified validator.
     *
     * @param validator The validator address.
     * @param sharesAmount The amount of shares to be withdrawn.
     * @param nonce The transaction count of the wallet address.
     * @return A Response object encapsulating the outcome of the transaction broadcast.
     *         On successful broadcast: Response(success=true, message=transactionHash, error=null).
     *         On failure: Response(success=false, message=null, error=errorMessage).
     */
    public Response withdraw(String validator, long sharesAmount, int nonce) throws IOException {
        return pwrj.broadcastTransaction(getSignedWithdrawTransaction(validator, sharesAmount, nonce));
    }


    /**
     * Returns the signed transaction of send data to a specified VM on the PWR network.
     *
     * @param vmId The ID of the VM.
     * @param data The data to be sent.
     * @param nonce The transaction count of the wallet address.
     * @return A byte array that represents a signed transaction of this method
     * @throws RuntimeException For various transaction-related validation issues.
     */
    public byte[] getSignedVmDataTransaction(long vmId, byte[] data, int nonce) {
        return getSignedTransaction(TransactionBuilder.getVmDataTransaction(vmId, data, nonce, pwrj.getChainId()));
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
     * @throws RuntimeException For various transaction-related validation issues.
     */
    public Response sendVmDataTransaction(long vmId, byte[] data, int nonce) {
        return pwrj.broadcastTransaction(getSignedVmDataTransaction(vmId, data, nonce));
    }



    /**
     * Returns a signed transaction of sending a transaction to claim a Virtual Machine ID on the PWR network, ensuring its owner 15% revenue of all transaction fees paid when transacting with this VM.
     *
     * @param vmId The ID of the VM.
     * @param nonce The transaction count of the wallet address.
     * @return A byte array that represents a signed transaction of this method
     */
    public byte[] getSignedClaimVmIdTransaction(long vmId, int nonce) {
        return getSignedTransaction(TransactionBuilder.getClaimVmIdTransaction(vmId, nonce, pwrj.getChainId()));
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
        return pwrj.broadcastTransaction(getSignedClaimVmIdTransaction(vmId, nonce));
    }



    /**
     * Returns a signed transaction of setting a guardian
     *
     * @param guardian the wallet address of the chosen guardian
     * @param expiryDate the expiry date after which the guardian will have revoked privileges
     * @param nonce the transaction count of the wallet address
     * @return a byte array with the outcome of this transaction
     */
    public byte[] getSignedSetGuardianTransaction(String guardian, long expiryDate, int nonce) {
        return getSignedTransaction(TransactionBuilder.getSetGuardianTransaction(guardian, expiryDate, nonce, pwrj.getChainId()));
    }
    /**
     * Sets a guardian
     *
     * @param guardian the wallet address of the chosen guardian
     * @param expiryDate the expiry date after which the guardian will have revoked privileges
     * @param nonce the transaction count of the wallet address
     * @return A Response object encapsulating the outcome of the transaction broadcast.
     *         On successful broadcast: Response(success=true, message=transactionHash, error=null).
     *         On failure: Response(success=false, message=errorMessage, error=null).
     */
    public Response setGuardian(String guardian, long expiryDate, int nonce) {
        return pwrj.broadcastTransaction(getSignedSetGuardianTransaction(guardian, expiryDate, nonce));
    }



    /**
     * Returns the signed transaction of removing/revoking a guardian wallet
     *
     * @param nonce the transaction count of the wallet address
     * @return a byte array with the outcome of the transaction
     */
    public byte[] getSignedRemoveGuardianTransaction(int nonce) {
        return getSignedTransaction(TransactionBuilder.getRemoveGuardianTransaction(nonce, pwrj.getChainId()));
    }
    /**
     * Sends transaction to remove/revoke a guardian wallet
     *
     * @param nonce the transaction count of the wallet address
     * @return A Response object encapsulating the outcome of the transaction broadcast.
     *         On successful broadcast: Response(success=true, message=transactionHash, error=null).
     *         On failure: Response(success=false, message=null, error=errorMessage).
     */
    public Response removeGuardian(int nonce) {
        return pwrj.broadcastTransaction(getSignedRemoveGuardianTransaction(nonce));
    }


    /**
     * Returns the signed transaction of approving a set of transactions for a guardian wallet.
     *
     * @param transactions The transactions to be approved.
     * @param nonce        The transaction nonce.
     * @return A byte array representing the signed transaction of this method.
     */
    public byte[] getSignedGuardianApprovalTransaction(List<byte[]> transactions, int nonce) {
        return getSignedTransaction(TransactionBuilder.getGuardianApprovalTransaction(transactions, nonce, pwrj.getChainId()));
    }

    /**
     * Returns the transaction of approving a set of transactions for a guardian wallet.
     *
     * @param transactions The transactions to be approved.
     * @param nonce        The transaction nonce.
     * @return A byte array representing the signed transaction of this method.
     */
    public Response sendGuardianApprovalTransaction(List<byte[]> transactions, int nonce) {
        return pwrj.broadcastTransaction(getSignedGuardianApprovalTransaction(transactions, nonce));
    }




    /**
     * Returns the signed transaction of send payable data to a specified VM on the PWR network.
     *
     * @param vmId The ID of the VM.
     * @param value The amount of PWR tokens to be sent.
     * @param data The data to be sent.
     * @param nonce The transaction count of the wallet address.
     * @return A byte array that represents a signed transaction of this method
     * @throws RuntimeException For various transaction-related validation issues.
     */
    public byte[] getSignedPayableVmDataTransaction(long vmId, long value, byte[] data, int nonce) {
        return getSignedTransaction(TransactionBuilder.getPayableVmDataTransaction(vmId, value, data, nonce, pwrj.getChainId()));
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
     * Returns the signed transaction of sending the transaction to remove validator. This transaction will only be available on testnets and not on mainnet.
     *
     * @param validator
     * @return a byte array representing the transaction of this method
     */
    public byte[] getSignedValidatorRemoveTransaction(String validator, int nonce) {
        return getSignedTransaction(TransactionBuilder.getValidatorRemoveTransaction(validator, nonce, pwrj.getChainId()));
    }
    /**
     * Sends the transaction to remove validator. This transaction will only be available on testnets and not on mainnet.
     *
     * @param validator
     * @return A Response object encapsulating the outcome of the transaction broadcast.
     *         On successful broadcast: Response(success=true, message=transactionHash, error=null).
     *         On failure: Response(success=false, message=null, error=errorMessage).
     */
    public Response sendValidatorRemoveTransaction(String validator, int nonce) {
        return pwrj.broadcastTransaction(getSignedValidatorRemoveTransaction(validator, nonce));
    }



    /**
     * Returns the signed transaction for approving a set of transactions for a specific VM.
     *
     * @param vmId         The ID of the VM.
     * @param transactions The transactions to be approved.
     * @param nonce        The transaction nonce.
     * @return A byte array representing the transaction of this method.
     * @throws RuntimeException     If the nonce is negative or there are no transactions to approve.
     */
    public byte[] getSignedConduitApprovalTransaction(long vmId, List<byte[]> transactions, int nonce) {
        return getSignedTransaction(TransactionBuilder.getConduitApprovalTransaction(vmId, transactions, nonce, pwrj.getChainId()));
    }
    /**
     * Sends the transaction for approving a set of transactions for a specific VM.
     *
     * @param vmId         The ID of the VM.
     * @param transactions The transactions to be approved.
     * @param nonce        The transaction nonce.
     * @return A byte array representing the transaction of this method.
     * @throws RuntimeException     If the nonce is negative or there are no transactions to approve.
     */
    public Response conduitApprove(long vmId, List<byte[]> transactions, int nonce) {
        return pwrj.broadcastTransaction(getSignedConduitApprovalTransaction(vmId, transactions, nonce));
    }


    /**
     * Returns the signed transaction for setting a list of conduits for a specific VM.
     *
     * @param vmId     The ID of the VM.
     * @param conduits The list of conduits to be set.
     * @param nonce    The transaction nonce.
     * @return A byte array representing the transaction of this method.
     * @throws RuntimeException     If the nonce is negative or there are no conduits to set.
     */
    public byte[] getSignedSetConduitTransaction(long vmId, List<byte[]> conduits, int nonce) {
        return getSignedTransaction(TransactionBuilder.getSetConduitsTransaction(vmId, conduits, nonce, pwrj.getChainId()));
    }
    /**
     * Sends the transaction for setting a list of conduits for a specific VM.
     *
     * @param vmId     The ID of the VM.
     * @param conduits The list of conduits to be set.
     * @param nonce    The transaction nonce.
     * @return A byte array representing the transaction of this method.
     * @throws RuntimeException     If the nonce is negative or there are no conduits to set.
     */
    public Response setConduits(long vmId, List<byte[]> conduits, int nonce) {
        return pwrj.broadcastTransaction(getSignedSetConduitTransaction(vmId, conduits, nonce));
    }


    /**
     * Returns the transaction for moving a specified amount of stake from one validator to another.
     *
     * @param sharesAmount  The amount of shares to be moved.
     * @param fromValidator The address of the validator to move stake from.
     * @param toValidator   The address of the validator to move stake to.
     * @param nonce         The transaction nonce.
     * @return A byte array representing the transaction of this method.
     */
    public byte[] getSignedMoveStakeTransaction(long sharesAmount, String fromValidator, String toValidator, int nonce) {
        return getSignedTransaction(TransactionBuilder.getMoveStakeTransaction(sharesAmount, fromValidator, toValidator, nonce, pwrj.getChainId()));
    }
    /**
     * Sends the transaction for moving a specified amount of stake from one validator to another.
     *
     * @param sharesAmount  The amount of shares to be moved.
     * @param fromValidator The address of the validator to move stake from.
     * @param toValidator   The address of the validator to move stake to.
     * @param nonce         The transaction nonce.
     * @return A Response object encapsulating the outcome of the transaction broadcast.
     *         On successful broadcast: Response(success=true, message=transactionHash, error=null).
     *         On failure: Response(success=false, message=null, error=errorMessage).
     */
    public Response moveStake(long sharesAmount, String fromValidator, String toValidator, int nonce) {
        return pwrj.broadcastTransaction(getSignedMoveStakeTransaction(sharesAmount, fromValidator, toValidator, nonce));
    }

    //
    public byte[] getSignedChangeEarlyWithdrawPenaltyProposalTxn(long withdrawalPenaltyTime, int withdrawalPenalty, String title, String description, int nonce) {
        return getSignedTransaction(TransactionBuilder.getChangeEarlyWithdrawPenaltyProposalTxn(withdrawalPenaltyTime, withdrawalPenalty, title, description, nonce, pwrj.getChainId()));
    }

    public Response createProposal_ChangeEarlyWithdrawalPenalty(long withdrawalPenaltyTime, int withdrawalPenalty, String title, String description, int nonce) {
        return pwrj.broadcastTransaction(getSignedChangeEarlyWithdrawPenaltyProposalTxn(withdrawalPenaltyTime, withdrawalPenalty, title, description, nonce));
    }

    //
    public byte[] getSignedChangeFeePerByteProposalTxn(long feePerByte, String title, String description, int nonce) {
        return getSignedTransaction(TransactionBuilder.getChangeFeePerByteProposalTxn(feePerByte, title, description, nonce, pwrj.getChainId()));
    }

    public Response createProposal_ChangeFeePerByte(long feePerByte, String title, String description, int nonce) {
        return pwrj.broadcastTransaction(getSignedChangeFeePerByteProposalTxn(feePerByte, title, description, nonce));
    }

    //
    public byte[] getSignedChangeMaxBlockSizeProposalTxn(int maxBlockSize, String title, String description, int nonce) {
        return getSignedTransaction(TransactionBuilder.getChangeMaxBlockSizeProposalTxn(maxBlockSize, title, description, nonce, pwrj.getChainId()));
    }

    public Response createProposal_ChangeMaxBlockSize(int maxBlockSize, String title, String description, int nonce) {
        return pwrj.broadcastTransaction(getSignedChangeMaxBlockSizeProposalTxn(maxBlockSize, title, description, nonce));
    }

    //
    public byte[] getSignedChangeMaxTxnProposalTxn(int maxTxnSize, String title, String description, int nonce) {
        return getSignedTransaction(TransactionBuilder.getChangeMaxTxnSizeProposalTxn(maxTxnSize, title, description, nonce, pwrj.getChainId()));
    }

    public Response createProposal_ChangeMaxTxnSizeSize(int maxTxnSize, String title, String description, int nonce) {
        return pwrj.broadcastTransaction(getSignedChangeMaxTxnProposalTxn(maxTxnSize, title, description, nonce));
    }

    //
    public byte[] getSignedChangeOverallBurnPercentageProposalTxn(int burnPercentage, String title, String description, int nonce) {
        return getSignedTransaction(TransactionBuilder.getChangeOverallBurnPercentageProposalTxn(burnPercentage, title, description, nonce, pwrj.getChainId()));
    }

    public Response createProposal_ChangeOverallBurnPercentage(int burnPercentage, String title, String description, int nonce) {
        return pwrj.broadcastTransaction(getSignedChangeOverallBurnPercentageProposalTxn(burnPercentage, title, description, nonce));
    }

    //
    public byte[] getSignedChangeRewardPerYearProposalTxn(long rewardPerYear, String title, String description, int nonce) {
        return getSignedTransaction(TransactionBuilder.getChangeRewardPerYearProposalTxn(rewardPerYear, title, description, nonce, pwrj.getChainId()));
    }

    public Response createProposal_ChangeRewardPerYear(long rewardPerYear, String title, String description, int nonce) {
        return pwrj.broadcastTransaction(getSignedChangeRewardPerYearProposalTxn(rewardPerYear, title, description, nonce));
    }

    //
    public byte[] getSignedChangeValidatorCountLimitProposalTxn(int validatorCountLimit, String title, String description, int nonce) {
        return getSignedTransaction(TransactionBuilder.getChangeValidatorCountLimitProposalTxn(validatorCountLimit, title, description, nonce, pwrj.getChainId()));
    }

    public Response createProposal_ChangeValidatorCountLimit(int validatorCountLimit, String title, String description, int nonce) {
        return pwrj.broadcastTransaction(getSignedChangeValidatorCountLimitProposalTxn(validatorCountLimit, title, description, nonce));
    }

    //
    public byte[] getSignedChangeValidatorJoiningFeeProposalTxn(long joiningFee, String title, String description, int nonce) {
        return getSignedTransaction(TransactionBuilder.getChangeValidatorJoiningFeeProposalTxn(joiningFee, title, description, nonce, pwrj.getChainId()));
    }

    public Response createProposal_ChangeValidatorJoiningFee(long joiningFee, String title, String description, int nonce) {
        return pwrj.broadcastTransaction(getSignedChangeValidatorJoiningFeeProposalTxn(joiningFee, title, description, nonce));
    }

    //
    public byte[] getSignedChangeVmIdClaimingFeeProposalTxn(long claimingFee, String title, String description, int nonce) {
        return getSignedTransaction(TransactionBuilder.getChangeVmIdClaimingFeeProposalTxn(claimingFee, title, description, nonce, pwrj.getChainId()));
    }

    public Response createProposal_ChangeVmIdClaimingFee(long claimingFee, String title, String description, int nonce) {
        return pwrj.broadcastTransaction(getSignedChangeVmIdClaimingFeeProposalTxn(claimingFee, title, description, nonce));
    }

    //
    public byte[] getSignedChangeVmOwnerTxnFeeShareProposalTxn(int feeShare, String title, String description, int nonce) {
        return getSignedTransaction(TransactionBuilder.getChangeVmOwnerTxnFeeShareProposalTxn(feeShare, title, description, nonce, pwrj.getChainId()));
    }

    public Response createProposal_ChangeVmOwnerTxnFeeShare(int feeShare, String title, String description, int nonce) {
        return pwrj.broadcastTransaction(getSignedChangeVmOwnerTxnFeeShareProposalTxn(feeShare, title, description, nonce));
    }

    //
    public byte[] getSignedOtherProposalTxn(String title, String description, int nonce) {
        return getSignedTransaction(TransactionBuilder.getOtherProposalTxn(title, description, nonce, pwrj.getChainId()));
    }

    public Response createProposal_OtherProposal(String title, String description, int nonce) {
        return pwrj.broadcastTransaction(getSignedOtherProposalTxn(title, description, nonce));
    }

    //
    public byte[] getSignedVoteOnProposalTxn(String proposalHash, byte vote, int nonce) {
        return getSignedTransaction(TransactionBuilder.getVoteOnProposalTxn(proposalHash, vote, nonce, pwrj.getChainId()));
    }

    public Response voteOnProposal(String proposalHash, byte vote, int nonce) {
        return pwrj.broadcastTransaction(getSignedVoteOnProposalTxn(proposalHash, vote, nonce));
    }

    public byte[] getSignedChangeIpTransaction(String newIp, int nonce) {
        return getSignedTransaction(TransactionBuilder.getChangeIpTxn(newIp, nonce, pwrj.getChainId()));
    }

    public Response changeIp(String newIp, int nonce) {
        return pwrj.broadcastTransaction(getSignedChangeIpTransaction(newIp, nonce));
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

    public static PWRWallet loadWallet(String path, String password, PWRJ pwrj) {
        try {
            byte[] encryptedPrivateKey = Files.readAllBytes(Path.of(path));
            byte[] privateKeyBytes = AES256.decrypt(encryptedPrivateKey, password);
            return new PWRWallet(privateKeyBytes, pwrj);
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    public static void processTransaction(VmDataTransaction transaction) {
        String sender = transaction.getSender();
        String hexData = transaction.getData();
        long timestampMs = transaction.getTimestamp();
        String humanReadableTimestamp = new java.text.SimpleDateFormat("dd/MM/yyyy HH:mm:ss").format(new java.util.Date(timestampMs));

        // Convert hex data to byte array
        byte[] data = Hex.decode(hexData.substring(2)); //Remove the 0x prefix

        //Convert to JSON Object
        JSONObject jsonObject = new JSONObject(new String(data, StandardCharsets.UTF_8));

        //Check actions
        for (String action: jsonObject.keySet()) {
            if(action.equalsIgnoreCase("sendMessage")) {
                String message = jsonObject.getString("sendMessage");

                //Output time of message and message
                System.out.println(humanReadableTimestamp + ": Message from " + sender + ": " + message);
            } else {
                //Unknown action... Ignore
            }
        }
    }
}
