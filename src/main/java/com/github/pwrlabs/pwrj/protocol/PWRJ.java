package com.github.pwrlabs.pwrj.protocol;

import com.github.pwrlabs.pwrj.interfaces.VidaTransactionHandler;
import com.github.pwrlabs.pwrj.entities.Block;
import com.github.pwrlabs.pwrj.record.response.EarlyWithdrawPenaltyResponse;
import com.github.pwrlabs.pwrj.record.response.Response;
import com.github.pwrlabs.pwrj.record.response.TransactionForGuardianApproval;
import com.github.pwrlabs.pwrj.entities.FalconTransaction;
import com.github.pwrlabs.pwrj.entities.WithdrawalOrder;
import com.github.pwrlabs.pwrj.entities.Validator;
import io.pwrlabs.util.encoders.BiResult;
import io.pwrlabs.util.encoders.ByteArrayWrapper;
import kotlin.Pair;
import lombok.Getter;
import lombok.Setter;
import org.apache.http.client.config.RequestConfig;
import org.apache.http.impl.client.*;
import org.bouncycastle.util.encoders.Hex;
import org.jetbrains.annotations.NotNull;
import org.json.JSONArray;
import org.json.JSONObject;

import java.io.IOException;
import java.math.BigDecimal;
//import java.net.http.HttpClient;
//import java.net.http.HttpResponse;
import java.math.BigInteger;
import java.math.RoundingMode;
import java.nio.charset.StandardCharsets;

import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.HttpResponse;
import org.apache.http.util.EntityUtils;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.StringEntity;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.web3j.crypto.Hash;

import java.util.*;

public class PWRJ {
    private static final Logger logger = LoggerFactory.getLogger(PWRJ.class);
    @Getter @Setter
    private int soTimeout = 20000, connectionTimeout = 20000;

    public PWRJ(String rpcNodeUrl) {
        this.rpcNodeUrl = rpcNodeUrl;
    }


    private String rpcNodeUrl;
    private byte chainId = (byte) -1;
    private long ecdsaVerificationFee = 10000;

    public JSONObject httpGet(String url) throws IOException {
        // Create custom request configuration
        RequestConfig requestConfig = RequestConfig.custom()
                .setConnectTimeout(connectionTimeout)
                .setSocketTimeout(soTimeout)
                .build();

        // Use custom configuration
        CloseableHttpClient client = HttpClients.custom()
                .setDefaultRequestConfig(requestConfig)
                .build();

        HttpGet request = new HttpGet(url);
        HttpResponse response = client.execute(request);

        if (response.getStatusLine().getStatusCode() == 200) {
            JSONObject object = new JSONObject(EntityUtils.toString(response.getEntity()));

            return object;
        } else if (response.getStatusLine().getStatusCode() == 400) {
            JSONObject object = new JSONObject(EntityUtils.toString(response.getEntity()));
            throw new RuntimeException("Failed with HTTP error 400 and message: " + object.getString("message"));
        } else {
            throw new RuntimeException("Failed with HTTP error code : " + response.getStatusLine().getStatusCode());
        }
    }

    public JSONObject httpPost(String url, @NotNull JSONObject body) throws IOException {
        // Create custom request configuration
        RequestConfig requestConfig = RequestConfig.custom()
                .setConnectTimeout(connectionTimeout)
                .setSocketTimeout(soTimeout)
                .build();

        // Use custom configuration
        CloseableHttpClient client = HttpClients.custom()
                .setDefaultRequestConfig(requestConfig)
                .build();

        HttpPost postRequest = new HttpPost(url);
        postRequest.setHeader("Accept", "application/json");
        postRequest.setHeader("Content-type", "application/json");
        postRequest.setEntity(new StringEntity(body.toString(), StandardCharsets.UTF_8));

        HttpResponse response = client.execute(postRequest);

        if (response.getStatusLine().getStatusCode() == 200) {
            JSONObject object = new JSONObject(EntityUtils.toString(response.getEntity()));

            return object;
        } else if (response.getStatusLine().getStatusCode() == 400) {
            JSONObject object = new JSONObject(EntityUtils.toString(response.getEntity()));

            throw new RuntimeException("Failed with HTTP error 400 and message: " + object.getString("message"));
        } else {
            throw new RuntimeException("Failed with HTTP error code : " + response.getStatusLine().getStatusCode());
        }
    }

    public byte[] getVidaIdAddressBytea(long vidaId) {
        String hexAddress = vidaId >= 0 ? "1" : "0";
        if (vidaId < 0) vidaId = -vidaId;
        String vidaIdString = Long.toString(vidaId);

        for (int i = 0; i < 39 - vidaIdString.length(); i++) {
            hexAddress += "0";
        }

        hexAddress += vidaIdString;

        return Hex.decode(hexAddress);
    }

    public String getVidaIdAddress(long vidaId) {
        return Hex.toHexString(getVidaIdAddressBytea(vidaId));
    }

    public static boolean isVidaAddress(String address) {
        try {
            if (address == null || (address.length() != 40 && address.length() != 42)) return false;
            if (address.startsWith("0x")) address = address.substring(2);
            if (!address.startsWith("0") && !address.startsWith("1")) return false;

            boolean negative = address.startsWith("0");
            if (!negative) address = address.substring(1);

            BigInteger maxLong = BigInteger.valueOf(Long.MAX_VALUE);
            BigInteger minLong = BigInteger.valueOf(Long.MIN_VALUE);

            BigInteger vidaId = new BigInteger(address);
            if (negative) vidaId = vidaId.negate();

            if (vidaId.compareTo(maxLong) > 0 || vidaId.compareTo(minLong) < 0) return false;

            return true;
        } catch (NumberFormatException e) {
            return false;
        }
    }

    /**
     * Retrieves the current RPC node URL being used.
     *
     * @return The URL of the RPC node.
     */
    public String getRpcNodeUrl() {
        return rpcNodeUrl;
    }

    public byte getChainId() {
        if(chainId == (byte)-1) {
            try {
                JSONObject object = httpGet(rpcNodeUrl + "/chainId");
                chainId = (byte) object.getInt("chainId");
            } catch (Exception e) {
                throw new RuntimeException("Failed to get chain ID from the RPC node: " + e.getMessage());
            }
        }

        return chainId;
    }

    public void setChainId(byte id) {
        chainId = id;
    }

    /**
     * Fetches the current fee-per-byte rate that's been set locally.
     *
     * @return The fee-per-byte rate.
     */
    public long getFeePerByte() throws IOException {
        return httpGet(rpcNodeUrl + "/feePerByte").getLong("feePerByte");
    }

    public short getBlockchainVersion() throws IOException {
        return (short) httpGet(rpcNodeUrl + "/blockchainVersion").getInt("blockchainVersion");
    }


    public byte[] getPublicKeyOfAddress(String address) {
        try {
            JSONObject object = httpGet(rpcNodeUrl + "/publicKeyOfAddress?address=" + address);
            String publicKey = object.getString("falconPublicKey");
            if(publicKey == null || publicKey.equalsIgnoreCase("null")) return null;
            if(publicKey.startsWith("0x")) publicKey = publicKey.substring(2);
            return Hex.decode(publicKey);
        } catch (Exception e) {
            return null;
        }
    }

    /**
     * Queries the RPC node to get the nonce of a specific address.
     *
     * <p>The nonce is a count of the number of transactions sent from the sender's address.
     * If the RPC node returns an unsuccessful status or if there's any network error,
     * appropriate exceptions will be thrown.</p>
     *
     * @param address The address for which to fetch the nonce.
     * @return The nonce of the specified address.
     * @throws IOException If there's an issue with the network or stream handling.
     * @throws InterruptedException If the request is interrupted.
     * @throws RuntimeException If the RPC node returns an unsuccessful status or a non-200 HTTP response.
     */
    public int getNonceOfAddress(String address) throws IOException {
        return httpGet(rpcNodeUrl + "/nonceOfUser?userAddress=" + address).getInt("nonce");
    }

    /**
     * Queries the RPC node to obtain the balance of a specific address.
     *
     * <p>If the RPC node returns an unsuccessful status or if there is any network error,
     * appropriate exceptions will be thrown.</p>
     *
     * @param address The address for which to fetch the balance.
     * @return The balance of the specified address.
     * @throws IOException If there's an issue with the network or stream handling.
     * @throws InterruptedException If the request is interrupted.
     * @throws RuntimeException If the RPC node returns an unsuccessful status or a non-200 HTTP response.
     */
    public long getBalanceOfAddress(String address) throws IOException {
        return httpGet(rpcNodeUrl + "/balanceOf?userAddress=" + address).getLong("balance");
    }

    /**
     * Retrieves guardian information for a specified wallet address.
     *
     * @param address The wallet address to check for guardian information
     * @return A BiResult containing the guardian's address (String) and expiry date (Long timestamp),
     *         or null if the address is not guarded
     * @throws IOException If there is an error during the HTTP request to the RPC node
     */
    public BiResult<String, Long> getGuardianOfAddress(String address) throws IOException {
        JSONObject object = httpGet(rpcNodeUrl + "/guardianOf?userAddress=" + address);

        if(object.getBoolean("isGuarded")) {
            return new BiResult<>(object.getString("guardian"), object.getLong("expiryDate"));
        } else {
            return null;
        }
    }

    /**
     * Retrieves the total count of blocks from the RPC node.
     *
     * <p>This method sends a GET request to the RPC node to fetch the total number of blocks.
     * If the RPC node returns a non-200 HTTP response or an unsuccessful status in the JSON response,
     * appropriate exceptions will be thrown.</p>
     *
     * @return The total count of blocks as reported by the RPC node.
     * @throws IOException If there's an issue with the network or stream handling.
     * @throws InterruptedException If the request is interrupted.
     * @throws RuntimeException If the RPC node returns an unsuccessful status or a non-200 HTTP response.
     */
    public long getBlocksCount() throws IOException {
        return httpGet(rpcNodeUrl + "/blocksCount").getLong("blocksCount");
    }

    public int getMaxBlockSize() throws IOException {
        return httpGet(rpcNodeUrl + "/maxBlockSize").getInt("maxBlockSize");
    }

    public int getMaxTransactionSize() throws IOException {
        return httpGet(rpcNodeUrl + "/maxTransactionSize").getInt("maxTransactionSize");
    }

    public int getValidatorCountLimit() throws IOException {
        return httpGet(rpcNodeUrl + "/validatorCountLimit").getInt("validatorCountLimit");
    }

    public int getValidatorSlashingFee() throws IOException {
        return httpGet(rpcNodeUrl + "/validatorSlashingFee").getInt("validatorSlashingFee");
    }

    public int getVidaOwnerTransactionFeeShare() throws IOException {
        return httpGet(rpcNodeUrl + "/vidaOwnerTransactionFeeShare").getInt("vidaOwnerTransactionFeeShare");
    }

    public int getBurnPercentage() throws IOException {
        return httpGet(rpcNodeUrl + "/burnPercentage").getInt("burnPercentage");
    }

    public int getValidatorOperationalFee() throws IOException {
        return httpGet(rpcNodeUrl + "/validatorOperationalFee").getInt("validatorOperationalFee");
    }

    public long getBlockNumber() throws IOException {
        return httpGet(rpcNodeUrl + "/blockNumber").getLong("blockNumber");
    }

    public long getBlockTimestamp() throws IOException {
        return httpGet(rpcNodeUrl + "/blockTimestamp").getLong("blockTimestamp");
    }

    public long getTotalVotingPower() throws IOException {
        return httpGet(rpcNodeUrl + "/totalVotingPower").getLong("totalVotingPower");
    }

    public long getPwrRewardsPerYear() throws IOException {
        return httpGet(rpcNodeUrl + "/pwrRewardsPerYear").getLong("pwrRewardsPerYear");
    }

    public long getWithdrawalLockTime() throws IOException {
        return httpGet(rpcNodeUrl + "/withdrawalLockTime").getLong("withdrawalLockTime");
    }

    public long getValidatorJoiningFee() throws IOException {
        return httpGet(rpcNodeUrl + "/validatorJoiningFee").getLong("validatorJoiningFee");
    }

    public long getMaxGuardianTime() throws IOException {
        return httpGet(rpcNodeUrl + "/maxGuardianTime").getLong("maxGuardianTime");
    }

    public long getVidaIdClaimingFee() throws IOException {
        return httpGet(rpcNodeUrl + "/vidaIdClaimingFee").getLong("vidaIdClaimingFee");
    }

    public long getProposalFee() throws IOException {
        return httpGet(rpcNodeUrl + "/proposalFee").getLong("proposalFee");
    }

    public long getProposalValidityTime() throws IOException {
        return httpGet(rpcNodeUrl + "/proposalValidityTime").getLong("proposalValidityTime");
    }

    public long getMinimumDelegatingAmount() throws IOException {
        return httpGet(rpcNodeUrl + "/minimumDelegatingAmount").getLong("minimumDelegatingAmount");
    }

    /**
     * Retrieves the number of the latest block from the RPC node.
     *
     * <p>This method utilizes the {@link #getBlocksCount()} method to get the total count of blocks
     * and then subtracts one to get the latest block number.</p>
     *
     * @return The number of the latest block.
     * @throws IOException If there's an issue with the network or stream handling.
     * @throws InterruptedException If the request is interrupted.
     * @throws RuntimeException If there are issues retrieving the total count of blocks.
     */
    public long getLatestBlockNumber() throws IOException {
        return getBlocksCount() - 1;
    }

    /**
     * Queries the RPC node to retrieve block details for a specific block number.
     *
     * <p>If the RPC node returns an unsuccessful status or if there's any network error,
     * appropriate exceptions will be thrown.</p>
     *
     * @param blockNumber The block number for which to fetch the block details.
     * @return The Block object representing the block details.
     * @throws IOException If there's an issue with the network or stream handling.
     * @throws InterruptedException If the request is interrupted.
     * @throws RuntimeException If the RPC node returns an unsuccessful status or a non-200 HTTP response.
     */
    public Block getBlockByNumber(long blockNumber) throws Exception {
        return new Block(httpGet(rpcNodeUrl + "/block?blockNumber=" + blockNumber).getJSONObject("block"));
    }

    public BiResult<Block, List<FalconTransaction.PayableVidaDataTxn>> getBlockWithViDataTransactionsOnly(long blockNumber, long vidaId) throws Exception {
        JSONObject object = httpGet(rpcNodeUrl + "/blockWithVidaDataTransactions?blockNumber=" + blockNumber + "&vidaId=" + vidaId);

        Block block = new Block(object.getJSONObject("block"));
        JSONArray transactionsArray = object.getJSONArray("transactions");

        List<FalconTransaction.PayableVidaDataTxn> transactions = new ArrayList<>();
        for (int i = 0; i < transactionsArray.length(); i++) {
            JSONObject transactionObject = transactionsArray.getJSONObject(i);
            FalconTransaction transaction = FalconTransaction.fromJson(transactionObject);

            if(transaction instanceof FalconTransaction.PayableVidaDataTxn payableVidaDataTxn) {
                transactions.add(payableVidaDataTxn);
            } else {
                throw new IllegalArgumentException("Transaction is not of type PayableVidaDataTxn");
            }
        }

        return new BiResult<>(block, transactions);
    }

    public FalconTransaction getTransactionByHash(String hash) throws Exception {
        JSONObject object = httpGet(rpcNodeUrl + "/transactionByHash?transactionHash=" + hash).getJSONObject("transaction");
        return FalconTransaction.fromJson(object);
    }

    public List<FalconTransaction> getTransactionsByHashes(List<String> hashes) throws Exception {
        JSONArray hashesArray = new JSONArray();
        for (String hash : hashes) {
            hashesArray.put(hash);
        }

        JSONArray transactionsArray = httpPost(rpcNodeUrl + "/getTransactionsByHashes", new JSONObject().put("transactionHashes", hashesArray)).getJSONArray("transactions");

        List<FalconTransaction> transactions = new ArrayList<>();
        for (int i = 0; i < transactionsArray.length(); i++) {
            JSONObject transactionObject = transactionsArray.getJSONObject(i);
            FalconTransaction transaction = FalconTransaction.fromJson(transactionObject);
            transactions.add(transaction);
        }

        return transactions;
    }

    public String getProposalStatus(String proposalHash) throws IOException {
        return httpGet(rpcNodeUrl + "/proposalStatus?proposalHash=" + proposalHash).getString("status");
    }

    public FalconTransaction.PayableVidaDataTxn[] getVidaDataTransactions(long startingBlock, long endingBlock, long vidaId) throws IOException {
        JSONObject object = httpGet(rpcNodeUrl + "/getVidaTransactions?startingBlock=" + startingBlock + "&endingBlock=" + endingBlock + "&vidaId=" + vidaId);

        JSONArray Transactions = object.getJSONArray("transactions");
        FalconTransaction.PayableVidaDataTxn[] TransactionsArray = new FalconTransaction.PayableVidaDataTxn[Transactions.length()];

        for(int i = 0; i < Transactions.length(); i++) {
            JSONObject TransactionObject = Transactions.getJSONObject(i);
            TransactionsArray[i] = (FalconTransaction.PayableVidaDataTxn) FalconTransaction.fromJson(TransactionObject);
        }

        return TransactionsArray;
    }

    public FalconTransaction.PayableVidaDataTxn[] getVidaDataTransactionsFilterByBytePrefix(long startingBlock, long endingBlock, long vidaId, byte[] prefix) throws IOException {
        JSONObject object = httpGet(rpcNodeUrl + "/getVidaTransactionsSortByBytePrefix?startingBlock=" + startingBlock + "&endingBlock=" + endingBlock + "&vidaId=" + vidaId + "&bytePrefix=" + Hex.toHexString(prefix));

        JSONArray Transactions = object.getJSONArray("transactions");
        FalconTransaction.PayableVidaDataTxn[] TransactionsArray = new FalconTransaction.PayableVidaDataTxn[Transactions.length()];

        for(int i = 0; i < Transactions.length(); i++) {
            JSONObject TransactionObject = Transactions.getJSONObject(i);
            TransactionsArray[i] = (FalconTransaction.PayableVidaDataTxn) FalconTransaction.fromJson(TransactionObject);
        }

        return TransactionsArray;
    }

    public TransactionForGuardianApproval isTransactionValidForGuardianApproval(String transaction) throws Exception {
        JSONObject object = httpPost(rpcNodeUrl + "/isTransactionValidForGuardianApproval", new JSONObject().put("transaction", transaction));

        boolean valid = object.getBoolean("valid");
        if(valid) {
            return TransactionForGuardianApproval.builder()
                    .valid(true)
                    .guardianAddress(object.optString("guardian", "0x"))
                    .transaction(FalconTransaction.fromJson(object))
                    .build();
        } else {
            return TransactionForGuardianApproval.builder()
                    .valid(false)
                    .errorMessage(object.getString("error"))
                    .transaction(null)
                    .guardianAddress(object.optString("guardian", "0x"))
                    .build();
        }
    }
    public TransactionForGuardianApproval isTransactionValidForGuardianApproval(byte[] Transaction) throws Exception {
        return isTransactionValidForGuardianApproval(Hex.toHexString(Transaction));
    }
    public long getActiveVotingPower() throws IOException {
        return httpGet(rpcNodeUrl + "/activeVotingPower").getLong("activeVotingPower");
    }

    /**
     * Queries the RPC node to get the total number of validators (standby & active).
     *
     * <p>If the RPC node returns an unsuccessful status or if there's any network error,
     * appropriate exceptions will be thrown.</p>
     *
     * @return The total number of validators.
     * @throws IOException If there's an issue with the network or stream handling.
     * @throws InterruptedException If the request is interrupted.
     * @throws RuntimeException If the RPC node returns an unsuccessful status or a non-200 HTTP response.
     */
    public int getTotalValidatorsCount() throws IOException {
        return httpGet(rpcNodeUrl + "/totalValidatorsCount").getInt("validatorsCount");
    }

    /**
     * Queries the RPC node to get the total number of standby validators.
     *
     * <p>If the RPC node returns an unsuccessful status or if there's any network error,
     * appropriate exceptions will be thrown.</p>
     *
     * @return The total number of validators.
     * @throws IOException If there's an issue with the network or stream handling.
     * @throws InterruptedException If the request is interrupted.
     * @throws RuntimeException If the RPC node returns an unsuccessful status or a non-200 HTTP response.
     */
    public int getStandbyValidatorsCount() throws IOException {
        return httpGet(rpcNodeUrl + "/standbyValidatorsCount").getInt("validatorsCount");
    }

    /**
     * Queries the RPC node to get the total number of active validators.
     *
     * <p>If the RPC node returns an unsuccessful status or if there's any network error,
     * appropriate exceptions will be thrown.</p>
     *
     * @return The total number of validators.
     * @throws IOException If there's an issue with the network or stream handling.
     * @throws InterruptedException If the request is interrupted.
     * @throws RuntimeException If the RPC node returns an unsuccessful status or a non-200 HTTP response.
     */
    public int getActiveValidatorsCount() throws IOException {
        return httpGet(rpcNodeUrl + "/activeValidatorsCount").getInt("validatorsCount");
    }

    /**
     * Queries the RPC node to get the list of all validators (standby & active).
     *
     * <p>If the RPC node returns an unsuccessful status or if there's any network error,
     * appropriate exceptions will be thrown.</p>
     *
     * @return The list of all validators.
     * @throws IOException If there's an issue with the network or stream handling.
     * @throws InterruptedException If the request is interrupted.
     * @throws RuntimeException If the RPC node returns an unsuccessful status or a non-200 HTTP response.
     */
    public List<Validator> getAllValidators() throws IOException {
        JSONObject object = httpGet(rpcNodeUrl + "/allValidators");
        JSONArray validators = object.getJSONArray("validators");
        List<Validator> validatorsList = new ArrayList<>();

        for(int i = 0; i < validators.length(); i++) {
            JSONObject validatorObject = validators.getJSONObject(i);
            //public Validator(String address, String ip, boolean badActor, long votingPower, long shares, int delegatorsCount) {
            long votingPower = validatorObject.optLong("votingPower", 0L);
            BigInteger totalShares = validatorObject.optBigInteger("totalShares", BigInteger.valueOf(0));

            int delegatorsCount;
            if(validatorObject.has("delegatorsCount")) {
                delegatorsCount = validatorObject.getInt("delegatorsCount");
            } else {
                delegatorsCount = 0;
            }

            Validator validator = Validator.builder()
                    .address(validatorObject.getString("address"))
                    .ip(validatorObject.getString("ip"))
                    .isBadActor(validatorObject.optBoolean("badActor", false))
                    .votingPower(votingPower)
                    .shares(totalShares)
                    .delegatorsCount(delegatorsCount)
                    .status(validatorObject.optString("status", "unknown"))
                    .build();

            validatorsList.add(validator);
        }
        return validatorsList;
    }

    /**
     * Queries the RPC node to get the list of all standby validators.
     *
     * <p>If the RPC node returns an unsuccessful status or if there's any network error,
     * appropriate exceptions will be thrown.</p>
     *
     * @return The list of all validators.
     * @throws IOException If there's an issue with the network or stream handling.
     * @throws InterruptedException If the request is interrupted.
     * @throws RuntimeException If the RPC node returns an unsuccessful status or a non-200 HTTP response.
     */
    public List<Validator> getStandbyValidators() throws IOException {
        JSONObject object = httpGet(rpcNodeUrl + "/standbyValidators");
        JSONArray validators = object.getJSONArray("validators");
        List<Validator> validatorsList = new ArrayList<>();

        for(int i = 0; i < validators.length(); i++) {
            JSONObject validatorObject = validators.getJSONObject(i);
            //public Validator(String address, String ip, boolean badActor, long votingPower, long shares, int delegatorsCount) {
            long votingPower = validatorObject.optLong("votingPower", 0L);
            BigInteger totalShares = validatorObject.optBigInteger("totalShares", BigInteger.valueOf(0));

            int delegatorsCount;
            if(validatorObject.has("delegatorsCount")) {
                delegatorsCount = validatorObject.getInt("delegatorsCount");
            } else {
                delegatorsCount = 0;
            }

            Validator validator = Validator.builder()
                    .address(validatorObject.getString("address"))
                    .ip(validatorObject.getString("ip"))
                    .isBadActor(validatorObject.optBoolean("badActor", false))
                    .votingPower(votingPower)
                    .shares(totalShares)
                    .delegatorsCount(delegatorsCount)
                    .status("standby")
                    .build();

            validatorsList.add(validator);
        }
        return validatorsList;
    }

    /**
     * Queries the RPC node to get the list of all active validators.
     *
     * <p>If the RPC node returns an unsuccessful status or if there's any network error,
     * appropriate exceptions will be thrown.</p>
     *
     * @return The list of all validators.
     * @throws IOException If there's an issue with the network or stream handling.
     * @throws InterruptedException If the request is interrupted.
     * @throws RuntimeException If the RPC node returns an unsuccessful status or a non-200 HTTP response.
     */
    public List<Validator> getActiveValidators() throws IOException {
        JSONObject object = httpGet(rpcNodeUrl + "/activeValidators");
        JSONArray validators = object.getJSONArray("validators");
        List<Validator> validatorsList = new ArrayList<>();

        for(int i = 0; i < validators.length(); i++) {
            JSONObject validatorObject = validators.getJSONObject(i);
            //public Validator(String address, String ip, boolean badActor, long votingPower, long shares, int delegatorsCount) {
            long votingPower = validatorObject.optLong("votingPower", 0L);
            BigInteger totalShares = validatorObject.optBigInteger("totalShares", BigInteger.valueOf(0));

            int delegatorsCount;
            if(validatorObject.has("delegatorsCount")) {
                delegatorsCount = validatorObject.getInt("delegatorsCount");
            } else {
                delegatorsCount = 0;
            }

            Validator validator = Validator.builder()
                    .address(validatorObject.getString("address"))
                    .ip(validatorObject.getString("ip"))
                    .isBadActor(validatorObject.optBoolean("badActor", false))
                    .votingPower(votingPower)
                    .shares(totalShares)
                    .delegatorsCount(delegatorsCount)
                    .status("active")
                    .build();

            validatorsList.add(validator);
        }
        return validatorsList;
    }

    public List<Validator> getDelegatees(String address) throws IOException {
        JSONObject object = httpGet(rpcNodeUrl + "/delegateesOfUser?userAddress=" + address);
        System.out.println(object);
        JSONArray validators = object.getJSONArray("validators");
        List<Validator> validatorsList = new ArrayList<>();

        for(int i = 0; i < validators.length(); i++) {
            JSONObject validatorObject = validators.getJSONObject(i);
            //public Validator(String address, String ip, boolean badActor, long votingPower, long shares, int delegatorsCount) {
            Validator validator = Validator.builder()
                    .address(validatorObject.optString("address", "0x"))
                    .ip(validatorObject.optString("ip", ""))
                    .isBadActor(validatorObject.optBoolean("badActor", false))
                    .votingPower(validatorObject.optLong("votingPower", 0))
                    .shares(validatorObject.optBigInteger("totalShares", BigInteger.valueOf(0)))
                    .delegatorsCount(validatorObject.optInt("delegatorsCount", 0))
                    .status(validatorObject.optString("status", "unknown"))
                    .build();

            validatorsList.add(validator);
        }
        return validatorsList;
    }

    public Validator getValidator(String validatorAddress) throws IOException {
        JSONObject object = httpGet(rpcNodeUrl + "/validator?validatorAddress=" + validatorAddress);
        JSONObject validatorObject = object.getJSONObject("validator");
        Validator validator = Validator.builder()
                .address(validatorObject.optString("address", "0x"))
                .ip(validatorObject.optString("ip", ""))
                .isBadActor(validatorObject.optBoolean("badActor", false))
                .votingPower(validatorObject.optLong("votingPower", 0))
                .shares(validatorObject.optBigInteger("totalShares", BigInteger.valueOf(0)))
                .delegatorsCount(validatorObject.optInt("delegatorsCount", 0))
                .status(validatorObject.optString("status", "unknown"))
                .build();

        return validator;
    }

    public long getDelegatedPWR(String delegatorAddress, String validatorAddress) throws IOException {
        return httpGet(rpcNodeUrl + "/validator/delegator/delegatedPWROfAddress?userAddress=" + delegatorAddress + "&validatorAddress=" + validatorAddress).getLong("delegatedPWR");
    }

    public long getSharesOfDelegator(String delegatorAddress, String validatorAddress) throws IOException {
        return httpGet(rpcNodeUrl + "/validator/delegator/sharesOfAddress?userAddress=" + delegatorAddress + "&validatorAddress=" + validatorAddress).getLong("shares");
    }

    public BigDecimal getShareValue(String validator) throws IOException {
        return httpGet(rpcNodeUrl + "/validator/shareValue?validatorAddress=" + validator).getBigDecimal("shareValue");
    }


    /**
     * Queries the RPC node to get the owner of a specific VIDA.
     *
     * <p>If the RPC node returns an unsuccessful status or if there's any network error,
     * appropriate exceptions will be thrown.</p>
     *
     * @param vidaId The ID of the VIDA for which to fetch the owner.
     * @return The owner of the specified VIDA.
     * @throws IOException If there's an issue with the network or stream handling.
     * @throws RuntimeException If the RPC node returns an unsuccessful status or a non-200 HTTP response.
     */
    public String getOwnerOfVida(long vidaId) throws IOException {
        JSONObject response = httpGet(rpcNodeUrl + "/ownerOfVidaId?vidaId=" + vidaId);

        if(response.optBoolean("claimed", false)) {
            return response.getString("owner");
        } else {
            return null;
        }
    }

    public List<String> getVidaSponsoredAddresses(long vidaId) {
    
        try {
            JSONObject object = httpGet(rpcNodeUrl + "/vidaSponsoredAddresses?vidaId=" + vidaId);
            JSONArray addresses = object.getJSONArray("sponsoredAddresses");
            List<String> addressesList = new ArrayList<>();

            for(int i = 0; i < addresses.length(); i++) {
                addressesList.add(addresses.getString(i));
            }

            return addressesList;
        } catch (Exception e) {
            e.printStackTrace();
            return new ArrayList<>();
        }
    }

    public List<String> getVidaAllowedSenders(long vidaId) {
        try {
            JSONObject object = httpGet(rpcNodeUrl + "/vidaAllowedSenders?vidaId=" + vidaId);
            JSONArray addresses = object.getJSONArray("allowedSenders");
            List<String> addressesList = new ArrayList<>();

            for(int i = 0; i < addresses.length(); i++) {
                addressesList.add(addresses.getString(i));
            }

            return addressesList;
        } catch (Exception e) {
            e.printStackTrace();
            return new ArrayList<>();
        }
    }

    public Boolean isVidaPrivate(long vidaId) {
        try {
            JSONObject response = httpGet(rpcNodeUrl + "/isVidaPrivate?vidaId=" + vidaId);
            return response.getBoolean("isPrivate");
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    /**
     * Retrieves a mapping of conduit addresses and their corresponding voting power for a specific VIDA.
     *
     * This method makes an HTTP GET request to the RPC node to fetch all conduits associated with the provided
     * VIDA ID. The response is parsed to extract each conduit's address and voting power, which are then
     * stored in a map.
     *
     * @param vidaId The unique identifier of the VIDA whose conduits are to be retrieved
     * @return A Map where each key is a ByteArrayWrapper containing a conduit address and each value is the
     *         corresponding voting power as a Long
     * @throws IOException If an error occurs during the HTTP request or response handling
     */
    public Map<ByteArrayWrapper /*Conduit address*/, Long/*Voting Power*/> getConduitsOfVida(long vidaId) throws IOException {
        JSONObject object = httpGet(rpcNodeUrl + "/conduitsOfVida?vidaId=" + vidaId);

        JSONArray conduits = object.getJSONArray("conduits");

        Map<ByteArrayWrapper, Long> conduitsMap = new HashMap<>();
        for (int i = 0; i < conduits.length(); i++) {
            JSONObject conduitObject = conduits.getJSONObject(i);

            String address = conduitObject.getString("address");
            if (address.startsWith("0x")) address = address.substring(2);

            ByteArrayWrapper conduitAddress = new ByteArrayWrapper(Hex.decode(address));
            long votingPower = conduitObject.getLong("votingPower");

            conduitsMap.put(conduitAddress, votingPower);
        }

        return conduitsMap;
    }

    public boolean isOwnerAllowedToTransferPWRFromVida(long vidaId) throws IOException {
        return httpGet(rpcNodeUrl + "/isOwnerAllowedToTransferPWRFromVida?vidaId=" + vidaId).getBoolean("allowed");
    }

    public EarlyWithdrawPenaltyResponse getEarlyWithdrawPenalty(long withdrawTime) throws IOException {
        JSONObject response = httpGet(rpcNodeUrl + "/earlyWithdrawPenalty?withdrawTime=" + withdrawTime);

        boolean earlyWithdrawAvailable = response.getBoolean("earlyWithdrawAvailable");
        long penalty = earlyWithdrawAvailable ? response.getLong("penalty") : 0;

        return new EarlyWithdrawPenaltyResponse(earlyWithdrawAvailable, penalty);
    }

    public Map<Long, Long> getAllEarlyWithdrawPenalties() throws IOException {
        JSONObject response = httpGet(rpcNodeUrl + "/allEarlyWithdrawPenalties");

        JSONObject penaltiesObj = response.getJSONObject("earlyWithdrawPenalties");
        Map<Long, Long> penalties = new HashMap<>();

        for (String key : penaltiesObj.keySet()) {
            long withdrawTime = Long.parseLong(key);
            long penalty = penaltiesObj.getLong(key);
            penalties.put(withdrawTime, penalty);
        }

        return penalties;
    }

    public WithdrawalOrder getWithdrawalOrder(byte[] withdrawalHash) throws IOException {
        JSONObject object = httpGet(rpcNodeUrl + "/withdrawalOrder?withdrawalHash=" + Hex.toHexString(withdrawalHash));
        boolean withdrawalOrderFound = object.getBoolean("withdrawalOrderFound");
        if (!withdrawalOrderFound) {
            return null;
        }

        return new WithdrawalOrder(object.getJSONObject("withdrawalOrder"));
    }

    /**
     * Retrieves transactions from a specific block that match the provided identifiers.
     *
     * @param blockNumber The block number to retrieve transactions from
     * @param identifiers A list of transaction identifiers to filter by
     * @return A pair containing the Block object and the list of matching FalconTransaction objects
     * @throws IOException If there's an issue with the network or stream handling
     * @throws Exception If there's an error parsing the response
     */
    public BiResult<Block, List<FalconTransaction>> getTransactionsByIdentifiers(long blockNumber, List<Integer> identifiers) throws Exception {
        JSONObject body = new JSONObject();
        body.put("blockNumber", blockNumber);

        JSONArray identifiersArray = new JSONArray();
        for (Integer identifier : identifiers) {
            identifiersArray.put(identifier);
        }
        body.put("identifiers", identifiersArray);

        JSONObject response = httpPost(rpcNodeUrl + "/getTransactionsByIdentifiers", body);

        Block block = new Block(response.getJSONObject("block"));
        JSONArray transactionsArray = response.getJSONArray("transactions");

        List<FalconTransaction> transactions = new ArrayList<>();
        for (int i = 0; i < transactionsArray.length(); i++) {
            JSONObject transactionObject = transactionsArray.getJSONObject(i);
            FalconTransaction transaction = FalconTransaction.fromJson(transactionObject);
            transactions.add(transaction);
        }

        return new BiResult<>(block, transactions);
    }

    /**
     * Broadcasts a transaction to the network via a specified RPC node.
     *
     * <p>This method serializes the provided transaction as a hex string and sends
     * it to the RPC node for broadcasting. Upon successful broadcast, the transaction
     * hash is returned. In case of any issues during broadcasting, appropriate exceptions
     * are thrown to indicate the error.</p>
     *
     * @param transaction The raw transaction bytes intended for broadcasting.

     * @throws IOException If an I/O error occurs when sending or receiving.
     * @throws InterruptedException If the send operation is interrupted.
     * @throws RuntimeException If the server responds with a non-200 HTTP status code.
     */
    public Response broadcastTransaction(byte[] transaction) {
        try {
            // Timeout configuration
            int timeout = 3 * 1000; // 3 seconds in milliseconds
            RequestConfig config = RequestConfig.custom()
                    .setConnectTimeout(timeout)      // setting connection timeout
                    .setConnectionRequestTimeout(timeout)
                    .setSocketTimeout(timeout).build();

            // Create HttpClient with the timeout
            CloseableHttpClient client = HttpClientBuilder.create().setDefaultRequestConfig(config).build();

            HttpPost postRequest = new HttpPost(rpcNodeUrl + "/broadcast");

            JSONObject json = new JSONObject();
            json.put("transaction", Hex.toHexString(transaction));
            //TODO remove this in future updates
            json.put("txn", Hex.toHexString(transaction));

            // Set up the header types needed to properly transfer JSON
            postRequest.setHeader("Accept", "application/json");
            postRequest.setHeader("Content-type", "application/json");
            postRequest.setEntity(new StringEntity(json.toString().toLowerCase(), StandardCharsets.UTF_8));

            // Execute request
            HttpResponse response = client.execute(postRequest);

            if (response.getStatusLine().getStatusCode() == 200) {
                //System.out.println("Status code: " + response.getStatusLine().getStatusCode());
                return new Response(true, "0x" + Hex.toHexString(Hash.sha3(transaction)), null);
            } else if (response.getStatusLine().getStatusCode() == 400) {
                System.out.println("Status code: " + response.getStatusLine().getStatusCode());
                JSONObject object = new JSONObject(EntityUtils.toString(response.getEntity()));
                System.out.println("broadcast response:" + object.toString());
                return new Response(false, null, object.optString("message", ""));
            } else {
                throw new RuntimeException("Failed with HTTP error code : " + response.getStatusLine().getStatusCode() + " " + EntityUtils.toString(response.getEntity()));
            }
        } catch (Exception e) {
            e.printStackTrace();
            return new Response(false, null, e.getMessage());
        }
    }

    public Object getOrDefault(JSONObject jsonObject, String key, Object defaultValue) {
        return jsonObject.has(key) ? jsonObject.get(key) : defaultValue;
    }

    public VidaTransactionSubscription subscribeToVidaTransactions(PWRJ pwrj, long vidaId, long startingBlock, VidaTransactionHandler handler, long pollInterval) throws IOException {
        VidaTransactionSubscription i = new VidaTransactionSubscription(pwrj, vidaId, startingBlock, handler, pollInterval);
        i.start();
        return i;
    }

    public VidaTransactionSubscription subscribeToVidaTransactions(PWRJ pwrj, long vidaId, long startingBlock, VidaTransactionHandler handler) throws IOException {
        VidaTransactionSubscription sub = subscribeToVidaTransactions(pwrj, vidaId, startingBlock, handler, 100);
        sub.start();
        return sub;
    }

    public static Map<ByteArrayWrapper /*Validator Address*/, Long /*APY*/> calculateActiveValidatorsApy() throws Exception {
        PWRJ pwrj = new PWRJ("https://pwrrpc.pwrlabs.io");

        List<Validator> activeValidators = pwrj.getActiveValidators();
        long latestBlockNumber = pwrj.getLatestBlockNumber();
        long latestBlockTimeStamp = pwrj.getBlockTimestamp();
        long sevenDaysInMs = 7 * 24 * 60 * 60 * 1000;
        long yearInMs = 365 * 24 * 60 * 60 * 1000;

        Map<ByteArrayWrapper /*ValidatorAddress*/, BigInteger /*Realtime shares per spark*/> validatorSharesPerSpark = new HashMap<>();
        Map<ByteArrayWrapper /*Validator Address*/, BiResult<Long /*Timestamp*/, BigInteger /*Shares Per Spark*/>> validatorData = new HashMap<>();

        long blockToCheck = latestBlockNumber;
        while (true) {
            blockToCheck -= 1;
            if(blockToCheck < 1) break;

            Block block = pwrj.getBlockByNumber(blockToCheck);
            if(block.getTimestamp() < latestBlockTimeStamp - sevenDaysInMs) break;

            byte[] proposer = io.pwrlabs.util.encoders.Hex.decode(block.getProposer().startsWith("0x") ? block.getProposer().substring(2) : block.getProposer());
            validatorData.put(new ByteArrayWrapper(proposer), new BiResult<>(block.getTimestamp(), block.getNewSharesPerSpark()));

            System.out.println("Checked block " + block.getBlockNumber());
        }

        Map<ByteArrayWrapper /*Validator Address*/, Long /*APY*/> validatorSevenDayYield = new HashMap<>();
        for (ByteArrayWrapper validatorAddress: validatorData.keySet()) {
            BiResult<Long, BigInteger> data = validatorData.get(validatorAddress);

            long currentTimeStamp = latestBlockTimeStamp;
            BigInteger currentSharesPerSpark = validatorSharesPerSpark.get(validatorAddress);

            long earliestTimeStamp = data.getFirst();
            BigInteger earliestSharesPerSpark = data.getSecond();

            // Skip if missing data or invalid values
            if (currentSharesPerSpark == null || earliestSharesPerSpark == null ||
                    earliestSharesPerSpark.compareTo(BigInteger.ZERO) <= 0) {
                continue;
            }

            // Calculate time difference in milliseconds
            long timeDiffMs = currentTimeStamp - earliestTimeStamp;

            // Skip if time difference is too small
            if (timeDiffMs <= 0) {
                continue;
            }

            // Calculate growth: (currentShares - earliestShares) / earliestShares
            BigDecimal sharesDiff = new BigDecimal(currentSharesPerSpark.subtract(earliestSharesPerSpark));
            BigDecimal growth = sharesDiff.divide(new BigDecimal(earliestSharesPerSpark), 18, RoundingMode.HALF_UP);

            // Calculate APY using compound interest formula: ((1 + growth) ^ (yearInMs / timeDiffMs)) - 1
            BigDecimal timeRatio = new BigDecimal(yearInMs).divide(new BigDecimal(timeDiffMs), 18, RoundingMode.HALF_UP);

            // Calculate APY
            double apy = Math.pow(1 + growth.doubleValue(), timeRatio.doubleValue()) - 1;

            // Convert to basis points (1% = 100 basis points)
            long apyInBps = (long)(apy * 10000);

            validatorSevenDayYield.put(validatorAddress, apyInBps);
        }

        return validatorSevenDayYield;
    }

    public static void main(String[] args) throws Exception {
        Map<ByteArrayWrapper, Long> apy = calculateActiveValidatorsApy();

        //output results
        for (Map.Entry<ByteArrayWrapper, Long> entry : apy.entrySet()) {
            System.out.println("Validator Address: " + Hex.toHexString(entry.getKey().data()) + ", APY: " + entry.getValue() + " bps");
        }
    }
}
