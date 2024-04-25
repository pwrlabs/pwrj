package com.github.pwrlabs.pwrj.protocol;

import com.github.pwrlabs.pwrj.record.block.Block;
import com.github.pwrlabs.pwrj.record.response.Response;
import com.github.pwrlabs.pwrj.record.response.TransactionForGuardianApproval;
import com.github.pwrlabs.pwrj.record.transaction.Transaction;
import com.github.pwrlabs.pwrj.record.transaction.VmDataTransaction;
import com.github.pwrlabs.pwrj.Utils.Hash;
import com.github.pwrlabs.pwrj.record.validator.Validator;
import org.apache.http.client.config.RequestConfig;
import org.apache.http.impl.client.*;
import org.bouncycastle.util.encoders.Hex;
import org.json.JSONArray;
import org.json.JSONObject;

import java.io.IOException;
import java.math.BigDecimal;
//import java.net.http.HttpClient;
//import java.net.http.HttpResponse;
import java.math.BigInteger;
import java.nio.charset.StandardCharsets;

import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.HttpResponse;
import org.apache.http.util.EntityUtils;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.StringEntity;

import java.util.*;

public class PWRJ {

    public PWRJ(String rpcNodeUrl) {
        this.rpcNodeUrl = rpcNodeUrl;

        try {
            JSONObject object = httpGet(rpcNodeUrl + "/chainId/");
            chainId = (byte) object.getInt("chainId");
        } catch (Exception e) {
            throw new RuntimeException("Failed to get chain ID from the RPC node: " + e.getMessage());
        }

        try {
            JSONObject object = httpGet(rpcNodeUrl + "/feePerByte/");
            feePerByte = object.getLong("feePerByte");
        } catch (Exception e) {
            throw new RuntimeException("Failed to get fee per byte from the RPC node: " + e.getMessage());
        }
    }
    private String rpcNodeUrl;
    private final byte chainId;
    private long feePerByte = 0;

    public static JSONObject httpGet(String url) throws IOException {
        // Set timeouts
        int connectionTimeout = 5 * 1000; // 5 seconds
        int socketTimeout = 5 * 1000; // 5 seconds

        // Create custom request configuration
        RequestConfig requestConfig = RequestConfig.custom()
                .setConnectTimeout(connectionTimeout)
                .setSocketTimeout(socketTimeout)
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

    public static JSONObject httpPost(String url, JSONObject body) throws IOException {
        // Set timeouts
        int connectionTimeout = 5 * 1000; // 5 seconds
        int socketTimeout = 5 * 1000; // 5 seconds

        // Create custom request configuration
        RequestConfig requestConfig = RequestConfig.custom()
                .setConnectTimeout(connectionTimeout)
                .setSocketTimeout(socketTimeout)
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

    public static String getVmIdAddress(long vmId) {
        String hexAddress = vmId >= 0 ? "1" : "0";
        if(vmId < 0) vmId = -vmId;
        String vmIdString = Long.toString(vmId);

        for(int i=0; i < 39 - vmIdString.length(); i++) {
            hexAddress += "0";
        }

        hexAddress += vmIdString;

        return "0x" + hexAddress;
    }

    public static boolean isVmAddress(String address) {
        if (address == null || (address.length() != 40 && address.length() != 42)) return false;
        if(address.startsWith("0x")) address = address.substring(2);
        if(!address.startsWith("0") && !address.startsWith("1")) return false;

        BigInteger maxLong = BigInteger.valueOf(Long.MAX_VALUE);
        BigInteger minLong = BigInteger.valueOf(Long.MIN_VALUE);

        BigInteger vmId = new BigInteger(address, 16);

        if(vmId.compareTo(maxLong) > 0 || vmId.compareTo(minLong) < 0) return false;

        return true;
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
        return chainId;
    }

    /**
     * Fetches the current fee-per-byte rate that's been set locally.
     *
     * @return The fee-per-byte rate.
     */
    public long getFeePerByte() throws IOException {
        return feePerByte;
    }

    public short getBlockchainVersion() throws IOException {
        return (short) httpGet(rpcNodeUrl + "/blockchainVersion/").getInt("blockchainVersion");
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
        return httpGet(rpcNodeUrl + "/nonceOfUser/?userAddress=" + address).getInt("nonce");
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
        return httpGet(rpcNodeUrl + "/balanceOf/?userAddress=" + address).getLong("balance");
    }

    public String getGuardianOfAddress(String address) throws IOException {
        JSONObject object = httpGet(rpcNodeUrl + "/guardianOf/?userAddress=" + address);

        if(object.getBoolean("isGuarded")) {
            return object.getString("guardian");
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
        return httpGet(rpcNodeUrl + "/blocksCount/").getLong("blocksCount");
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
    public Block getBlockByNumber(long blockNumber) throws IOException {
        return new Block(httpGet(rpcNodeUrl + "/block/?blockNumber=" + blockNumber).getJSONObject("block"));
    }

    public VmDataTransaction[] getVMDataTransactions(long startingBlock, long endingBlock, long vmId) throws IOException {
        JSONObject object = httpGet(rpcNodeUrl + "/getVmTransactions/?startingBlock=" + startingBlock + "&endingBlock=" + endingBlock + "&vmId=" + vmId);

        JSONArray Transactions = object.getJSONArray("transactions");
        VmDataTransaction[] TransactionsArray = new VmDataTransaction[Transactions.length()];

        for(int i = 0; i < Transactions.length(); i++) {
            JSONObject TransactionObject = Transactions.getJSONObject(i);
            VmDataTransaction Transaction = new VmDataTransaction(TransactionObject, TransactionObject.optLong("blockNumber", 0), TransactionObject.optLong("timestamp", 0), TransactionObject.optInt("positionInTheBlock", 0));
            TransactionsArray[i] = Transaction;
        }

        return TransactionsArray;
    }

    public VmDataTransaction[] getVMDataTransactionsFilterByBytePrefix(long startingBlock, long endingBlock, long vmId, byte[] prefix) throws IOException {
        JSONObject object = httpGet(rpcNodeUrl + "/getVmTransactionsSortByBytePrefix/?startingBlock=" + startingBlock + "&endingBlock=" + endingBlock + "&vmId=" + vmId + "&bytePrefix=" + Hex.toHexString(prefix));

        JSONArray Transactions = object.getJSONArray("transactions");
        VmDataTransaction[] TransactionsArray = new VmDataTransaction[Transactions.length()];

        for(int i = 0; i < Transactions.length(); i++) {
            JSONObject TransactionObject = Transactions.getJSONObject(i);
            VmDataTransaction Transaction = new VmDataTransaction(TransactionObject, TransactionObject.optLong("blockNumber", 0), TransactionObject.optLong("timestamp", 0), TransactionObject.optInt("positionInTheBlock", 0));
            TransactionsArray[i] = Transaction;
        }

        return TransactionsArray;
    }

    public TransactionForGuardianApproval isTransactionValidForGuardianApproval(String transaction) throws IOException {
        JSONObject object = httpPost(rpcNodeUrl + "/isTransactionValidForGuardianApproval/", new JSONObject().put("transaction", transaction));

        boolean valid = object.getBoolean("valid");
        if(valid) {
            return TransactionForGuardianApproval.builder()
                    .valid(true)
                    .guardianAddress(object.optString("guardian", "0x"))
                    .transaction(Transaction.fromJSON(object.getJSONObject("transaction"), 0, 0, 0))
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
    public TransactionForGuardianApproval isTransactionValidForGuardianApproval(byte[] Transaction) throws IOException {
        return isTransactionValidForGuardianApproval(Hex.toHexString(Transaction));
    }
    public long getActiveVotingPower() throws IOException {
        return httpGet(rpcNodeUrl + "/activeVotingPower/").getLong("activeVotingPower");
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
        return httpGet(rpcNodeUrl + "/totalValidatorsCount/").getInt("validatorsCount");
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
        return httpGet(rpcNodeUrl + "/standbyValidatorsCount/").getInt("validatorsCount");
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
        return httpGet(rpcNodeUrl + "/activeValidatorsCount/").getInt("validatorsCount");
    }

    /**
     * Queries the RPC node to get the total number of delegators.
     *
     * <p>If the RPC node returns an unsuccessful status or if there's any network error,
     * appropriate exceptions will be thrown.</p>
     *
     * @return The total number of delegators.
     */
    public int getTotalDelegatorsCount() throws IOException {
        return httpGet(rpcNodeUrl + "/totalDelegatorsCount/").getInt("delegatorsCount");
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
        JSONObject object = httpGet(rpcNodeUrl + "/allValidators/");
        JSONArray validators = object.getJSONArray("validators");
        List<Validator> validatorsList = new ArrayList<>();

        for(int i = 0; i < validators.length(); i++) {
            JSONObject validatorObject = validators.getJSONObject(i);
            //public Validator(String address, String ip, boolean badActor, long votingPower, long shares, int delegatorsCount) {
            long votingPower, totalShares;
            if(validatorObject.has("votingPower")) {
                votingPower = validatorObject.getLong("votingPower");
            } else {
                votingPower = 0L;
            }

            if(validatorObject.has("totalShares")) {
                totalShares = validatorObject.getLong("totalShares");
            } else {
                totalShares = 0L;
            }

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
        JSONObject object = httpGet(rpcNodeUrl + "/standbyValidators/");
        JSONArray validators = object.getJSONArray("validators");
        List<Validator> validatorsList = new ArrayList<>();

        for(int i = 0; i < validators.length(); i++) {
            JSONObject validatorObject = validators.getJSONObject(i);
            //public Validator(String address, String ip, boolean badActor, long votingPower, long shares, int delegatorsCount) {
            long votingPower, totalShares;
            if(validatorObject.has("votingPower")) {
                votingPower = validatorObject.getLong("votingPower");
            } else {
                votingPower = 0L;
            }

            if(validatorObject.has("totalShares")) {
                totalShares = validatorObject.getLong("totalShares");
            } else {
                totalShares = 0L;
            }

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
        JSONObject object = httpGet(rpcNodeUrl + "/activeValidators/");
        JSONArray validators = object.getJSONArray("validators");
        List<Validator> validatorsList = new ArrayList<>();

        for(int i = 0; i < validators.length(); i++) {
            JSONObject validatorObject = validators.getJSONObject(i);
            //public Validator(String address, String ip, boolean badActor, long votingPower, long shares, int delegatorsCount) {
            long votingPower, totalShares;
            if(validatorObject.has("votingPower")) {
                votingPower = validatorObject.getLong("votingPower");
            } else {
                votingPower = 0L;
            }

            if(validatorObject.has("totalShares")) {
                totalShares = validatorObject.getLong("totalShares");
            } else {
                totalShares = 0L;
            }

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
        JSONObject object = httpGet(rpcNodeUrl + "/delegateesOfUser/?userAddress=" + address);
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
                    .shares(validatorObject.optLong("totalShares", 0))
                    .delegatorsCount(validatorObject.optInt("delegatorsCount", 0))
                    .status(validatorObject.optString("status", "unknown"))
                    .build();

            validatorsList.add(validator);
        }
        return validatorsList;
    }
    public Validator getValidator(String validatorAddress) throws IOException {
        JSONObject object = httpGet(rpcNodeUrl + "/validator/?validatorAddress=" + validatorAddress);
        JSONObject validatorObject = object.getJSONObject("validator");
        Validator validator = Validator.builder()
                .address(validatorObject.optString("address", "0x"))
                .ip(validatorObject.optString("ip", ""))
                .isBadActor(validatorObject.optBoolean("badActor", false))
                .votingPower(validatorObject.optLong("votingPower", 0))
                .shares(validatorObject.optLong("totalShares", 0))
                .delegatorsCount(validatorObject.optInt("delegatorsCount", 0))
                .status(validatorObject.optString("status", "unknown"))
                .build();

        return validator;
    }
    public long getDelegatedPWR(String delegatorAddress, String validatorAddress) throws IOException {
        return httpGet(rpcNodeUrl + "/validator/delegator/delegatedPWROfAddress/?userAddress=" + delegatorAddress + "&validatorAddress=" + validatorAddress).getLong("delegatedPWR");
    }

    public long getSharesOfDelegator(String delegatorAddress, String validatorAddress) throws IOException {
        return httpGet(rpcNodeUrl + "/validator/delegator/sharesOfAddress/?userAddress=" + delegatorAddress + "&validatorAddress=" + validatorAddress).getLong("shares");
    }
    public BigDecimal getShareValue(String validator) throws IOException {
        return httpGet(rpcNodeUrl + "/validator/shareValue/?validatorAddress=" + validator).getBigDecimal("shareValue");
    }


    /**
     * Queries the RPC node to get the owner of a specific VM.
     *
     * <p>If the RPC node returns an unsuccessful status or if there's any network error,
     * appropriate exceptions will be thrown.</p>
     *
     * @param vmId The ID of the VM for which to fetch the owner.
     * @return The owner of the specified VM.
     * @throws IOException If there's an issue with the network or stream handling.
     * @throws RuntimeException If the RPC node returns an unsuccessful status or a non-200 HTTP response.
     */
    public String getOwnerOfVm(long vmId) throws IOException {
        JSONObject response = httpGet(rpcNodeUrl + "/ownerOfVmId/?vmId=" + vmId);

        if(response.optBoolean("claimed", false)) {
            return response.getString("owner");
        } else {
            return null;
        }
    }

    public List<Validator> getConduitsOfVm(long vmId) {
        try {
            JSONObject object = httpGet(rpcNodeUrl + "/conduitsOfVm/?vmId=" + vmId);
            JSONArray validators = object.getJSONArray("conduits");
            List<Validator> validatorsList = new ArrayList<>();

            for(int i = 0; i < validators.length(); i++) {
                JSONObject validatorObject = validators.getJSONObject(i);
                Validator validator = new Validator(validatorObject);
                validatorsList.add(validator);
            }

            return validatorsList;
        } catch (Exception e) {
            e.printStackTrace();
            return new ArrayList<>();
        }
    }




    /**
     * Fetches and updates the current fee per byte from the RPC node.
     *
     * <p>The PWR Chain determines transaction fees based on the transaction size in bytes.
     * This method queries the RPC node for the latest fee-per-byte rate and updates
     * the local {@code feePerByte} variable.</p>
     *
     * <p>If the RPC node returns an unsuccessful status or if there is any network error,
     * appropriate exceptions will be thrown. Ensure error handling is implemented when calling
     * this method.</p>
     *
     * @throws IOException If there's an issue with the network or stream handling.
     * @throws InterruptedException If the request is interrupted.
     * @throws RuntimeException If the RPC node returns an unsuccessful status or a non-200 HTTP response.
     */
    public void updateFeePerByte() throws IOException {
        feePerByte = httpGet(rpcNodeUrl + "/feePerByte/").getLong("feePerByte");
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

            HttpPost postRequest = new HttpPost(rpcNodeUrl + "/broadcast/");

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
                return new Response(true, "0x" + Hex.toHexString(Hash.sha3(transaction)), null);
            } else if (response.getStatusLine().getStatusCode() == 400) {
                JSONObject object = new JSONObject(EntityUtils.toString(response.getEntity()));
                System.out.println("broadcast response:" + object.toString());
                return new Response(false, null, object.optString("message", ""));
            } else {
                throw new RuntimeException("Failed with HTTP error code : " + response.getStatusLine().getStatusCode() + " " + EntityUtils.toString(response.getEntity()));
            }
        } catch (Exception e) {
            return new Response(false, null, e.getMessage());
        }
    }


    public Object getOrDefault(JSONObject jsonObject, String key, Object defaultValue) {
        return jsonObject.has(key) ? jsonObject.get(key) : defaultValue;
    }
}
