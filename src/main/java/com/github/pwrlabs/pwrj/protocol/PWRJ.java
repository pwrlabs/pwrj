package com.github.pwrlabs.pwrj.protocol;

import com.github.pwrlabs.pwrj.Block.Block;
import com.github.pwrlabs.pwrj.Transaction.VmDataTxn;
import com.github.pwrlabs.pwrj.Utils.Hash;
import com.github.pwrlabs.pwrj.Utils.Response;
import com.github.pwrlabs.pwrj.Validator.Validator;
import org.apache.http.client.config.RequestConfig;
import org.apache.http.impl.client.*;
import org.bouncycastle.util.encoders.Hex;
import org.json.JSONArray;
import org.json.JSONObject;

import java.io.IOException;
import java.math.BigDecimal;
//import java.net.http.HttpClient;
//import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;

import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.HttpResponse;
import org.apache.http.util.EntityUtils;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.StringEntity;

import java.util.*;

public class PWRJ {

    private static String rpcNodeUrl;
    private static byte chainId = (byte) -1;
    private static long feePerByte = 0;

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

    /**
     * Sets the RPC node URL. This URL will be used for all RPC calls in the PWRJ library
     *
     * @param url The URL of the RPC node.
     */
    public static void setRpcNodeUrl(String url) {
        rpcNodeUrl = url;
    }

    /**
     * Retrieves the current RPC node URL being used.
     *
     * @return The URL of the RPC node.
     */
    public static String getRpcNodeUrl() {
        return rpcNodeUrl;
    }

    public static byte getChainId() throws IOException {
        if(chainId == -1) {
            JSONObject object = httpGet(rpcNodeUrl + "/chainId/");
            chainId = (byte) object.getInt("chainId");
        }

        return chainId;
    }

    /**
     * Fetches the current fee-per-byte rate that's been set locally.
     *
     * @return The fee-per-byte rate.
     */
    public static long getFeePerByte() throws IOException {
        if(feePerByte == 0) {
            JSONObject object = httpGet(rpcNodeUrl + "/feePerByte/");
            feePerByte = object.getLong("feePerByte");
        }

        return feePerByte;
    }

    public static short getBlockchainVersion() throws IOException {
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
    public static int getNonceOfAddress(String address) throws IOException {
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
    public static long getBalanceOfAddress(String address) throws IOException {
        return httpGet(rpcNodeUrl + "/balanceOf/?userAddress=" + address).getLong("balance");
    }

    public static String getGuardianOfAddress(String address) throws IOException {
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
    public static long getBlocksCount() throws IOException {
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
    public static long getLatestBlockNumber() throws IOException {
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
    public static Block getBlockByNumber(long blockNumber) throws IOException {
        return new Block(httpGet(rpcNodeUrl + "/block/?blockNumber=" + blockNumber).getJSONObject("block"));
    }

    public static VmDataTxn[] getVMDataTxns(long startingBlock, long endingBlock, long vmId) throws IOException {
        JSONObject object = httpGet(rpcNodeUrl + "/getVmTransactions/?startingBlock=" + startingBlock + "&endingBlock=" + endingBlock + "&vmId=" + vmId);

        JSONArray txns = object.getJSONArray("transactions");
        VmDataTxn[] txnsArray = new VmDataTxn[txns.length()];

        for(int i = 0; i < txns.length(); i++) {
            JSONObject txnObject = txns.getJSONObject(i);
            VmDataTxn txn = new VmDataTxn(txnObject);
            txnsArray[i] = txn;
        }

        return txnsArray;
    }

    public static VmDataTxn[] getVMDataTxnsFilterByBytePrefix(long startingBlock, long endingBlock, long vmId, byte[] prefix) throws IOException {
        JSONObject object = httpGet(rpcNodeUrl + "/getVmTransactionsSortByBytePrefix/?startingBlock=" + startingBlock + "&endingBlock=" + endingBlock + "&vmId=" + vmId + "&bytePrefix=" + Hex.toHexString(prefix));

        JSONArray txns = object.getJSONArray("transactions");
        VmDataTxn[] txnsArray = new VmDataTxn[txns.length()];

        for(int i = 0; i < txns.length(); i++) {
            JSONObject txnObject = txns.getJSONObject(i);
            VmDataTxn txn = new VmDataTxn(txnObject);
            txnsArray[i] = txn;
        }

        return txnsArray;
    }

    public static long getActiveVotingPower() throws IOException {
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
    public static int getTotalValidatorsCount() throws IOException {
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
    public static int getStandbyValidatorsCount() throws IOException {
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
    public static int getActiveValidatorsCount() throws IOException {
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
    public static int getTotalDelegatorsCount() throws IOException {
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
    public static List<Validator> getAllValidators() throws IOException {
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
                    .address("0x" + validatorObject.getString("address"))
                    .ip(validatorObject.getString("ip"))
                    .isBadActor(validatorObject.optBoolean("badActor", false))
                    .votingPower(votingPower)
                    .shares(totalShares)
                    .delegatorsCount(delegatorsCount)
                    .status(validatorObject.optString("status", "unknown"))
                    .build();

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
    public static List<Validator> getStandbyValidators() throws IOException {
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
                    .address("0x" + validatorObject.getString("address"))
                    .ip(validatorObject.getString("ip"))
                    .isBadActor(validatorObject.optBoolean("badActor", false))
                    .votingPower(votingPower)
                    .shares(totalShares)
                    .delegatorsCount(delegatorsCount)
                    .status("standby")
                    .build();
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
    public static List<Validator> getActiveValidators() throws IOException {
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
                    .address("0x" + validatorObject.getString("address"))
                    .ip(validatorObject.getString("ip"))
                    .isBadActor(validatorObject.getBoolean("badActor"))
                    .votingPower(votingPower)
                    .shares(totalShares)
                    .delegatorsCount(delegatorsCount)
                    .status("active")
                    .build();

            validatorsList.add(validator);
        }
        return validatorsList;
    }

    public static List<Validator> getDelegatees(String address) throws IOException {
        JSONObject object = httpGet(rpcNodeUrl + "/delegateesOfUser/?userAddress=" + address);
        JSONArray validators = object.getJSONArray("validators");
        List<Validator> validatorsList = new ArrayList<>();

        for(int i = 0; i < validators.length(); i++) {
            JSONObject validatorObject = validators.getJSONObject(i);
            //public Validator(String address, String ip, boolean badActor, long votingPower, long shares, int delegatorsCount) {
            Validator validator = Validator.builder()
                    .address("0x" + validatorObject.getString("address"))
                    .ip(validatorObject.getString("ip"))
                    .isBadActor(validatorObject.getBoolean("badActor"))
                    .votingPower(validatorObject.getLong("votingPower"))
                    .shares(validatorObject.getLong("totalShares"))
                    .delegatorsCount(validatorObject.getInt("delegatorsCount"))
                    .status("active")
                    .build();

            validatorsList.add(validator);
        }
        return validatorsList;
    }
    public static Validator getValidator(String validatorAddress) throws IOException {
        JSONObject object = httpGet(rpcNodeUrl + "/validator/?validatorAddress=" + validatorAddress);
        JSONObject validatorObject = object.getJSONObject("validator");
        Validator validator = Validator.builder()
                .address("0x" + validatorObject.getString("address"))
                .ip(validatorObject.getString("ip"))
                .isBadActor(validatorObject.getBoolean("badActor"))
                .votingPower(validatorObject.getLong("votingPower"))
                .shares(validatorObject.getLong("totalShares"))
                .delegatorsCount(validatorObject.getInt("delegatorsCount"))
                .status(validatorObject.getString("status"))
                .build();

        return validator;
    }
    public static long getDelegatedPWR(String delegatorAddress, String validatorAddress) throws IOException {
        return httpGet(rpcNodeUrl + "validator/delegator/delegatedPWROfAddress/?userAddress=" + delegatorAddress + "&validatorAddress=" + validatorAddress).getLong("delegatedPWR");
    }

    public static BigDecimal getShareValue(String validator) throws IOException {
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
    public static String getOwnerOfVm(long vmId) throws IOException {
        return httpGet(rpcNodeUrl + "/ownerOfVmId/?vmId=" + vmId).getString("owner");
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
    public static void updateFeePerByte() throws IOException {
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
     * @param txn The raw transaction bytes intended for broadcasting.

     * @throws IOException If an I/O error occurs when sending or receiving.
     * @throws InterruptedException If the send operation is interrupted.
     * @throws RuntimeException If the server responds with a non-200 HTTP status code.
     */
    public static Response broadcastTxn(byte[] txn) {
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
            json.put("txn", Hex.toHexString(txn));

            // Set up the header types needed to properly transfer JSON
            postRequest.setHeader("Accept", "application/json");
            postRequest.setHeader("Content-type", "application/json");
            postRequest.setEntity(new StringEntity(json.toString(), StandardCharsets.UTF_8));

            // Execute request
            HttpResponse response = client.execute(postRequest);

            if (response.getStatusLine().getStatusCode() == 200) {
                return new Response(true, "0x" + Hex.toHexString(Hash.sha3(txn)), null);
            } else if (response.getStatusLine().getStatusCode() == 400) {
                JSONObject object = new JSONObject(EntityUtils.toString(response.getEntity()));
                System.out.println("broadcast response:" + object.toString());
                return new Response(false, null, object.getString("message"));
            } else {
                throw new RuntimeException("Failed with HTTP error code : " + response.getStatusLine().getStatusCode());
            }
        } catch (Exception e) {
            return new Response(false, null, e.getMessage());
        }
    }


    public static Object getOrDefault(JSONObject jsonObject, String key, Object defaultValue) {
        return jsonObject.has(key) ? jsonObject.get(key) : defaultValue;
    }

    public static void main(String[] args) {
        //Tests for all the function
        try {
            setRpcNodeUrl("https://pwrrpc.pwrlabs.io");
            System.out.println(getChainId());
            System.out.println(getFeePerByte());
            System.out.println(getBlockchainVersion());
            System.out.println(getBlocksCount());
            System.out.println(getLatestBlockNumber());
            System.out.println(getBlockByNumber(10000));
            System.out.println(getActiveVotingPower());
            System.out.println(getTotalValidatorsCount());
            System.out.println(getStandbyValidatorsCount());
            System.out.println(getActiveValidatorsCount());
            System.out.println(getTotalDelegatorsCount());
            System.out.println(getAllValidators());
            System.out.println(getStandbyValidators());
            System.out.println(getActiveValidators());
            System.out.println(getVMDataTxns(1, 800, 10023));
            System.out.println(getOwnerOfVm(100));
            System.out.println(getNonceOfAddress("0xf6fe6a14b3aac06c2c102cf5f028df35157f9770"));
            System.out.println(getBalanceOfAddress("0xf6fe6a14b3aac06c2c102cf5f028df35157f9770"));
            System.out.println(getGuardianOfAddress("0xf6fe6a14b3aac06c2c102cf5f028df35157f9770"));
            System.out.println(getDelegatees("0xf6fe6a14b3aac06c2c102cf5f028df35157f9770"));
            System.out.println(getValidator("0xf6fe6a14b3aac06c2c102cf5f028df35157f9770"));
            System.out.println(getDelegatedPWR("0xf6fe6a14b3aac06c2c102cf5f028df35157f9770", "0xf6fe6a14b3aac06c2c102cf5f028df35157f9770"));
            System.out.println(getShareValue("0xf6fe6a14b3aac06c2c102cf5f028df35157f9770"));
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
