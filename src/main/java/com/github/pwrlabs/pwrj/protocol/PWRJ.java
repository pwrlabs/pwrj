package com.github.pwrlabs.pwrj.protocol;

import com.github.pwrlabs.pwrj.Block.Block;
import com.github.pwrlabs.pwrj.Delegator.Delegator;
import com.github.pwrlabs.pwrj.Transaction.VmDataTxn;
import com.github.pwrlabs.pwrj.Utils.Hash;
import com.github.pwrlabs.pwrj.Utils.Response;
import com.github.pwrlabs.pwrj.Validator.Validator;
import com.github.pwrlabs.pwrj.wallet.PWRWallet;
import org.apache.http.client.config.RequestConfig;
import org.apache.http.impl.client.*;
import org.bouncycastle.jce.provider.BouncyCastleProvider;
import org.bouncycastle.util.encoders.Hex;
import org.json.JSONArray;
import org.json.JSONObject;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.math.BigDecimal;
import java.net.URI;
//import java.net.http.HttpClient;
import java.net.http.HttpRequest;
//import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.security.Security;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.HttpResponse;
import org.apache.http.util.EntityUtils;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.HttpResponse;
import org.apache.http.entity.StringEntity;
import org.apache.http.message.BasicNameValuePair;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.NameValuePair;
import org.apache.http.util.EntityUtils;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;

public class PWRJ {

    private static HttpClient client = HttpClients.createDefault();
    private static String rpcNodeUrl;
    private static long feePerByte = 100;

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

    /**
     * Fetches the current fee-per-byte rate that's been set locally.
     *
     * @return The fee-per-byte rate.
     */
    public static long getFeePerByte() {
        return feePerByte;
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
    public static int getNonceOfAddress(String address) throws IOException, InterruptedException {
        try {
            HttpGet request = new HttpGet(rpcNodeUrl + "/nonceOfUser/?userAddress=" + address);
            HttpResponse response = client.execute(request);

            //System.out.printf(EntityUtils.toString(response.getEntity()));

            if (response.getStatusLine().getStatusCode() == 200) {
                JSONObject object = new JSONObject(EntityUtils.toString(response.getEntity()));
                return object.getInt("nonce");
            } else if (response.getStatusLine().getStatusCode() == 400) {
                JSONObject object = new JSONObject(EntityUtils.toString(response.getEntity()));
                throw new RuntimeException("Failed with HTTP error 400 and message: " + object.getString("message"));
            } else {
                throw new RuntimeException("Failed with HTTP error code : " + response.getStatusLine().getStatusCode());
            }
        } catch (Exception e) {
            e.printStackTrace();
            return 0;
        }

//        HttpRequest request = HttpRequest.newBuilder()
//                //http://localhost:8085/nonceOfUser/?userAddress=0x2605c1ad496f428ab2b700edd257f0a378f83750
//                .uri(URI.create(rpcNodeUrl + "/nonceOfUser/?userAddress=" + address))
//                .GET()
//                .header("Accept", "application/json")
//                .build();
//
//        HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
//
//        if (response.statusCode() == 200) {
//            JSONObject object = new JSONObject(response.body());
//            return object.getInt("nonce");
//        } else if (response.statusCode() == 400) {
//            JSONObject object = new JSONObject(response.body());
//            throw new RuntimeException("Failed with HTTP error 400 and message: " + object.getString("message"));
//        } else {
//            throw new RuntimeException("Failed with HTTP error code : " + response.statusCode());
//        }
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
    public static long getBalanceOfAddress(String address) throws IOException, InterruptedException {
        try {
            HttpGet request = new HttpGet(rpcNodeUrl + "/balanceOf/?userAddress=" + address);
            HttpResponse response = client.execute(request);

            if (response.getStatusLine().getStatusCode() == 200) {
                JSONObject object = new JSONObject(EntityUtils.toString(response.getEntity()));
                return object.getLong("balance");
            } else if (response.getStatusLine().getStatusCode() == 400) {
                JSONObject object = new JSONObject(EntityUtils.toString(response.getEntity()));
                throw new RuntimeException("Failed with HTTP error 400 and message: " + object.getString("message"));
            } else {
                throw new RuntimeException("Failed with HTTP error code : " + response.getStatusLine().getStatusCode());
            }
        } catch (Exception e) {
            e.printStackTrace();
            return 0;
        }
//        HttpRequest request = HttpRequest.newBuilder()
//                .uri(URI.create(rpcNodeUrl + "/balanceOf/?userAddress=" + address))
//                .GET()
//                .header("Accept", "application/json")
//                .build();
//
//        HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
//
//        if (response.statusCode() == 200) {
//            JSONObject object = new JSONObject(response.body());
//            return object.getLong("balance");
//        } else if (response.statusCode() == 400) {
//            JSONObject object = new JSONObject(response.body());
//            throw new RuntimeException("Failed with HTTP error 400 and message: " + object.getString("message"));
//        } else {
//            throw new RuntimeException("Failed with HTTP error code : " + response.statusCode());
//        }
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
    public static long getBlocksCount() throws IOException, InterruptedException {
        try {
            HttpGet request = new HttpGet(rpcNodeUrl + "/blocksCount/");
            HttpResponse response = client.execute(request);

            if (response.getStatusLine().getStatusCode() == 200) {
                JSONObject object = new JSONObject(EntityUtils.toString(response.getEntity()));
                return object.getLong("blocksCount");
            } else if (response.getStatusLine().getStatusCode() == 400) {
                JSONObject object = new JSONObject(EntityUtils.toString(response.getEntity()));
                throw new RuntimeException("Failed with HTTP error 400 and message: " + object.getString("message"));
            } else {
                throw new RuntimeException("Failed with HTTP error code : " + response.getStatusLine().getStatusCode());
            }
        } catch (Exception e) {
            e.printStackTrace();
            return 0;
        }
//        HttpRequest request = HttpRequest.newBuilder()
//                .uri(URI.create(rpcNodeUrl + "/blocksCount/"))
//                .GET()
//                .header("Accept", "application/json")
//                .build();
//
//        HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
//
//
//        if (response.statusCode() == 200) {
//            JSONObject object = new JSONObject(response.body());
//            return object.getLong("blocksCount");
//        } else if (response.statusCode() == 400) {
//            JSONObject object = new JSONObject(response.body());
//            throw new RuntimeException("Failed with HTTP error 400 and message: " + object.getString("message"));
//        } else {
//            throw new RuntimeException("Failed with HTTP error code : " + response.statusCode());
//        }
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
    public static long getLatestBlockNumber() throws IOException, InterruptedException {
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
        try {
            HttpGet request = new HttpGet(rpcNodeUrl + "/block/?blockNumber=" + blockNumber);
            HttpResponse response = client.execute(request);

            if (response.getStatusLine().getStatusCode() == 200) {
                JSONObject object = new JSONObject(EntityUtils.toString(response.getEntity()));
                JSONObject blockJson = object.getJSONObject("block");
                return new Block(blockJson);
            } else if (response.getStatusLine().getStatusCode() == 400) {
                JSONObject object = new JSONObject(EntityUtils.toString(response.getEntity()));
                throw new RuntimeException("Failed with HTTP error 400 and message: " + object.getString("message"));
            } else {
                throw new RuntimeException("Failed with HTTP error code : " + response.getStatusLine().getStatusCode());
            }
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    public static VmDataTxn[] getVMDataTxns(long startingBlock, long endingBlock, long vmId) {
        try {
            HttpGet request = new HttpGet(rpcNodeUrl + "/getVmTransactions/?startingBlock=" + startingBlock + "&endingBlock=" + endingBlock + "&vmId=" + vmId);
            HttpResponse response = client.execute(request);

            if (response.getStatusLine().getStatusCode() == 200) {
                JSONObject object = new JSONObject(EntityUtils.toString(response.getEntity()));

                JSONArray txns = object.getJSONArray("transactions");
                VmDataTxn[] txnsArray = new VmDataTxn[txns.length()];

                for(int i = 0; i < txns.length(); i++) {
                    JSONObject txnObject = txns.getJSONObject(i);
                    VmDataTxn txn = new VmDataTxn(txnObject.getInt("size"), txnObject.getLong("blockNumber"), txnObject.getInt("positionInTheBlock"), txnObject.getLong("fee"), txnObject.getString("type"), txnObject.getString("from"), txnObject.getString("to"), txnObject.getString("nonceOrValidationHash"), txnObject.getString("hash"), txnObject.getLong("vmId"), txnObject.getString("data"));
                    txnsArray[i] = txn;
                }

                return txnsArray;
            } else if (response.getStatusLine().getStatusCode() == 400) {
                JSONObject object = new JSONObject(EntityUtils.toString(response.getEntity()));
                throw new RuntimeException("Failed with HTTP error 400 and message: " + object.getString("message") + " " + object.getString("error"));
            } else {
                throw new RuntimeException("Failed with HTTP error code : " + response.getStatusLine().getStatusCode());
            }
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    public static long getActiveVotingPower() {
        try {
            HttpGet request = new HttpGet(rpcNodeUrl + "/activeVotingPower/");
            HttpResponse response = client.execute(request);

            if (response.getStatusLine().getStatusCode() == 200) {
                JSONObject object = new JSONObject(EntityUtils.toString(response.getEntity()));
                return object.getLong("activeVotingPower");
            } else if (response.getStatusLine().getStatusCode() == 400) {
                JSONObject object = new JSONObject(EntityUtils.toString(response.getEntity()));
                throw new RuntimeException("Failed with HTTP error 400 and message: " + object.getString("message"));
            } else {
                throw new RuntimeException("Failed with HTTP error code : " + response.getStatusLine().getStatusCode());
            }
        } catch (Exception e) {
            e.printStackTrace();
            return 0;
        }
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
    public static int getTotalValidatorsCount() {
        try {
            HttpGet request = new HttpGet(rpcNodeUrl + "/totalValidatorsCount/");
            HttpResponse response = client.execute(request);

            if (response.getStatusLine().getStatusCode() == 200) {
                JSONObject object = new JSONObject(EntityUtils.toString(response.getEntity()));
                return object.getInt("validatorsCount");
            } else if (response.getStatusLine().getStatusCode() == 400) {
                JSONObject object = new JSONObject(EntityUtils.toString(response.getEntity()));
                throw new RuntimeException("Failed with HTTP error 400 and message: " + object.getString("message"));
            } else {
                throw new RuntimeException("Failed with HTTP error code : " + response.getStatusLine().getStatusCode());
            }
        } catch (Exception e) {
            e.printStackTrace();
            return 0;
        }
//        HttpRequest request = HttpRequest.newBuilder()
//                .uri(URI.create(rpcNodeUrl + "/totalValidatorsCount/"))
//                .GET()
//                .header("Accept", "application/json")
//                .build();
//
//        HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
//
//        if (response.statusCode() == 200) {
//            JSONObject object = new JSONObject(response.body());
//            return object.getInt("validatorsCount");
//        } else if (response.statusCode() == 400) {
//            JSONObject object = new JSONObject(response.body());
//            throw new RuntimeException("Failed with HTTP error 400 and message: " + object.getString("message"));
//        } else {
//            throw new RuntimeException("Failed with HTTP error code : " + response.statusCode());
//        }
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
    public static int getStandbyValidatorsCount() {
        try {
            HttpGet request = new HttpGet(rpcNodeUrl + "/standbyValidatorsCount/");
            HttpResponse response = client.execute(request);

            if (response.getStatusLine().getStatusCode() == 200) {
                JSONObject object = new JSONObject(EntityUtils.toString(response.getEntity()));
                return object.getInt("validatorsCount");
            } else if (response.getStatusLine().getStatusCode() == 400) {
                JSONObject object = new JSONObject(EntityUtils.toString(response.getEntity()));
                throw new RuntimeException("Failed with HTTP error 400 and message: " + object.getString("message"));
            } else {
                throw new RuntimeException("Failed with HTTP error code : " + response.getStatusLine().getStatusCode());
            }
        } catch (Exception e) {
            e.printStackTrace();
            return 0;
        }
//        HttpRequest request = HttpRequest.newBuilder()
//                .uri(URI.create(rpcNodeUrl + "/totalValidatorsCount/"))
//                .GET()
//                .header("Accept", "application/json")
//                .build();
//
//        HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
//
//        if (response.statusCode() == 200) {
//            JSONObject object = new JSONObject(response.body());
//            return object.getInt("validatorsCount");
//        } else if (response.statusCode() == 400) {
//            JSONObject object = new JSONObject(response.body());
//            throw new RuntimeException("Failed with HTTP error 400 and message: " + object.getString("message"));
//        } else {
//            throw new RuntimeException("Failed with HTTP error code : " + response.statusCode());
//        }
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
    public static int getActiveValidatorsCount() {
        try {
            HttpGet request = new HttpGet(rpcNodeUrl + "/activeValidatorsCount/");
            HttpResponse response = client.execute(request);

            if (response.getStatusLine().getStatusCode() == 200) {
                JSONObject object = new JSONObject(EntityUtils.toString(response.getEntity()));
                return object.getInt("validatorsCount");
            } else if (response.getStatusLine().getStatusCode() == 400) {
                JSONObject object = new JSONObject(EntityUtils.toString(response.getEntity()));
                throw new RuntimeException("Failed with HTTP error 400 and message: " + object.getString("message"));
            } else {
                throw new RuntimeException("Failed with HTTP error code : " + response.getStatusLine().getStatusCode());
            }
        } catch (Exception e) {
            e.printStackTrace();
            return 0;
        }
    }

    /**
     * Queries the RPC node to get the total number of delegators.
     *
     * <p>If the RPC node returns an unsuccessful status or if there's any network error,
     * appropriate exceptions will be thrown.</p>
     *
     * @return The total number of delegators.
     */
    public static int getTotalDelegatorsCount() {
        try {
            HttpGet request = new HttpGet(rpcNodeUrl + "/totalDelegatorsCount/");
            HttpResponse response = client.execute(request);

            if (response.getStatusLine().getStatusCode() == 200) {
                JSONObject object = new JSONObject(EntityUtils.toString(response.getEntity()));
                return object.getInt("delegatorsCount");
            } else if (response.getStatusLine().getStatusCode() == 400) {
                JSONObject object = new JSONObject(EntityUtils.toString(response.getEntity()));
                throw new RuntimeException("Failed with HTTP error 400 and message: " + object.getString("message"));
            } else {
                throw new RuntimeException("Failed with HTTP error code : " + response.getStatusLine().getStatusCode());
            }
        } catch (Exception e) {
            e.printStackTrace();
            return 0;
        }
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
    public static List<Validator> getAllValidators() {
        try {
            HttpGet request = new HttpGet(rpcNodeUrl + "/allValidators/");
            HttpResponse response = client.execute(request);

            if (response.getStatusLine().getStatusCode() == 200) {
                JSONObject object = new JSONObject(EntityUtils.toString(response.getEntity()));
                JSONArray validators = object.getJSONArray("validators");
                List<Validator> validatorsList = new ArrayList<>();

                for(int i = 0; i < validators.length(); i++) {
                    JSONObject validatorObject = validators.getJSONObject(i);
                    //public Validator(String address, String ip, boolean badActor, long votingPower, long shares, int delegatorsCount) {
                    Validator validator = new Validator("0x" + validatorObject.getString("address"), validatorObject.getString("ip"), validatorObject.getBoolean("badActor"), validatorObject.getLong("votingPower"), validatorObject.getLong("totalShares"), validatorObject.getInt("delegatorsCount"), validatorObject.getString("status"));
                    validatorsList.add(validator);
                }
                return validatorsList;
            } else if (response.getStatusLine().getStatusCode() == 400) {
                JSONObject object = new JSONObject(EntityUtils.toString(response.getEntity()));
                throw new RuntimeException("Failed with HTTP error 400 and message: " + object.getString("message"));
            } else {
                throw new RuntimeException("Failed with HTTP error code : " + response.getStatusLine().getStatusCode());
            }
        } catch (Exception e) {
            e.printStackTrace();
            return new LinkedList<>();
        }
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
    public static List<Validator> getStandbyValidators() {
        try {
            HttpGet request = new HttpGet(rpcNodeUrl + "/standbyValidators/");
            HttpResponse response = client.execute(request);

            if (response.getStatusLine().getStatusCode() == 200) {
                JSONObject object = new JSONObject(EntityUtils.toString(response.getEntity()));
                JSONArray validators = object.getJSONArray("validators");
                List<Validator> validatorsList = new ArrayList<>();

                for(int i = 0; i < validators.length(); i++) {
                    JSONObject validatorObject = validators.getJSONObject(i);
                    //public Validator(String address, String ip, boolean badActor, long votingPower, long shares, int delegatorsCount) {
                    Validator validator = new Validator("0x" + validatorObject.getString("address"), validatorObject.getString("ip"), validatorObject.getBoolean("badActor"), validatorObject.getLong("votingPower"), validatorObject.getLong("totalShares"), validatorObject.getInt("delegatorsCount"), "standby");
                    validatorsList.add(validator);
                }
                return validatorsList;
            } else if (response.getStatusLine().getStatusCode() == 400) {
                JSONObject object = new JSONObject(EntityUtils.toString(response.getEntity()));
                throw new RuntimeException("Failed with HTTP error 400 and message: " + object.getString("message"));
            } else {
                throw new RuntimeException("Failed with HTTP error code : " + response.getStatusLine().getStatusCode());
            }
        } catch (Exception e) {
            e.printStackTrace();
            return new LinkedList<>();
        }
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
    public static List<Validator> getActiveValidators() {
        try {
            HttpGet request = new HttpGet(rpcNodeUrl + "/activeValidators/");
            HttpResponse response = client.execute(request);

            if (response.getStatusLine().getStatusCode() == 200) {
                JSONObject object = new JSONObject(EntityUtils.toString(response.getEntity()));
                JSONArray validators = object.getJSONArray("validators");
                List<Validator> validatorsList = new ArrayList<>();

                for(int i = 0; i < validators.length(); i++) {
                    JSONObject validatorObject = validators.getJSONObject(i);
                    //public Validator(String address, String ip, boolean badActor, long votingPower, long shares, int delegatorsCount) {
                    Validator validator = new Validator("0x" + validatorObject.getString("address"), validatorObject.getString("ip"), validatorObject.getBoolean("badActor"), validatorObject.getLong("votingPower"), validatorObject.getLong("totalShares"), validatorObject.getInt("delegatorsCount"), "active");
                    validatorsList.add(validator);
                }
                return validatorsList;
            } else if (response.getStatusLine().getStatusCode() == 400) {
                JSONObject object = new JSONObject(EntityUtils.toString(response.getEntity()));
                throw new RuntimeException("Failed with HTTP error 400 and message: " + object.getString("message"));
            } else {
                throw new RuntimeException("Failed with HTTP error code : " + response.getStatusLine().getStatusCode());
            }
        } catch (Exception e) {
            e.printStackTrace();
            return new LinkedList<>();
        }
    }

    public static List<Validator> getDelegatees(String address) {
        try {
            HttpGet request = new HttpGet(rpcNodeUrl + "/activeValidators/");
            HttpResponse response = client.execute(request);

            if (response.getStatusLine().getStatusCode() == 200) {
                JSONObject object = new JSONObject(EntityUtils.toString(response.getEntity()));
                JSONArray validators = object.getJSONArray("validators");
                List<Validator> validatorsList = new ArrayList<>();

                for(int i = 0; i < validators.length(); i++) {
                    JSONObject validatorObject = validators.getJSONObject(i);
                    //public Validator(String address, String ip, boolean badActor, long votingPower, long shares, int delegatorsCount) {
                    Validator validator = new Validator("0x" + validatorObject.getString("address"), validatorObject.getString("ip"), validatorObject.getBoolean("badActor"), validatorObject.getLong("votingPower"), validatorObject.getLong("totalShares"), validatorObject.getInt("delegatorsCount"), "active");
                    validatorsList.add(validator);
                }
                return validatorsList;
            } else if (response.getStatusLine().getStatusCode() == 400) {
                JSONObject object = new JSONObject(EntityUtils.toString(response.getEntity()));
                throw new RuntimeException("Failed with HTTP error 400 and message: " + object.getString("message"));
            } else {
                throw new RuntimeException("Failed with HTTP error code : " + response.getStatusLine().getStatusCode());
            }
        } catch (Exception e) {
            e.printStackTrace();
            return new LinkedList<>();
        }
    }
    public static Validator getValidator(String validatorAddress) {
        try {
            HttpGet request = new HttpGet(rpcNodeUrl + "/validator/?validatorAddress=" + validatorAddress);
            HttpResponse response = client.execute(request);

            if (response.getStatusLine().getStatusCode() == 200) {
                JSONObject object = new JSONObject(EntityUtils.toString(response.getEntity()));
                JSONObject validatorObject = object.getJSONObject("validator");
                Validator validator = new Validator("0x" + validatorObject.getString("address"), validatorObject.getString("ip"), validatorObject.getBoolean("badActor"), validatorObject.getLong("votingPower"), validatorObject.getLong("totalShares"), validatorObject.getInt("delegatorsCount"), validatorObject.getString("status"));

                return validator;
            } else if (response.getStatusLine().getStatusCode() == 400) {
                return null;
            } else {
                throw new RuntimeException("Failed with HTTP error code : " + response.getStatusLine().getStatusCode());
            }
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }
    public static long getDelegatedPWR(String delegatorAddress, String validatorAddress) {
        try {
            HttpGet request = new HttpGet(rpcNodeUrl + "validator/delegator/delegatedPWROfAddress/?userAddress=" + delegatorAddress + "&validatorAddress=" + validatorAddress);
            HttpResponse response = client.execute(request);

            if (response.getStatusLine().getStatusCode() == 200) {
                JSONObject object = new JSONObject(EntityUtils.toString(response.getEntity()));

                return object.getLong("delegatedPWR");
            } else if (response.getStatusLine().getStatusCode() == 400) {
                JSONObject object = new JSONObject(EntityUtils.toString(response.getEntity()));

                throw new RuntimeException("Failed with HTTP error 400 and message: " + object.getString("message"));
            } else {
                throw new RuntimeException("Failed with HTTP error code : " + response.getStatusLine().getStatusCode());

            }
        } catch (Exception e) {
            e.printStackTrace();
            return 0;
        }
    }

    public static JSONObject getShareValue(List<String> validators, long blockNumber) {
        try {
            HttpPost postRequest = new HttpPost(rpcNodeUrl + "/getShareValue/");

            JSONArray validatorsArray = new JSONArray();
            for (String validator: validators) {
                validatorsArray.put(validator);
            }

            JSONObject json = new JSONObject();
            json.put("validators", validatorsArray);
            json.put("blockNumber", blockNumber);

            // Set up the header types needed to properly transfer JSON
            postRequest.setHeader("Accept", "application/json");
            postRequest.setHeader("Content-type", "application/json");
            postRequest.setEntity(new StringEntity(json.toString(), StandardCharsets.UTF_8));
            // Execute request
            HttpResponse response = client.execute(postRequest);

            if (response.getStatusLine().getStatusCode() == 200) {
                JSONObject object = new JSONObject(EntityUtils.toString(response.getEntity()));
                JSONObject shareValues = object.getJSONObject("shareValues");
                return shareValues;
            } else if (response.getStatusLine().getStatusCode() == 400) {
                JSONObject object = new JSONObject(EntityUtils.toString(response.getEntity()));
                System.out.printf(object.toString());
                throw new RuntimeException("Failed with HTTP error 400 and message: " + object.getString("message"));
            } else {
                throw new RuntimeException("Failed with HTTP error code : " + response.getStatusLine().getStatusCode());
            }
        } catch (Exception e) {
            e.printStackTrace();
            return new JSONObject();
        }
    }
    public static BigDecimal getShareValue(String validator) {
        try {
            HttpPost postRequest = new HttpPost(rpcNodeUrl + "/validator/shareValue/?validatorAddress=" + validator);

            // Set up the header types needed to properly transfer JSON
            postRequest.setHeader("Accept", "application/json");
            postRequest.setHeader("Content-type", "application/json");
            // Execute request
            HttpResponse response = client.execute(postRequest);

            if (response.getStatusLine().getStatusCode() == 200) {
                JSONObject object = new JSONObject(EntityUtils.toString(response.getEntity()));
                return object.getBigDecimal("shareValue");
            } else if (response.getStatusLine().getStatusCode() == 400) {
                JSONObject object = new JSONObject(EntityUtils.toString(response.getEntity()));
                System.out.printf(object.toString());
                throw new RuntimeException("Failed with HTTP error 400 and message: " + object.getString("message"));
            } else {
                throw new RuntimeException("Failed with HTTP error code : " + response.getStatusLine().getStatusCode());
            }
        } catch (Exception e) {
            e.printStackTrace();
            return BigDecimal.valueOf(0);
        }
    }

    public static void main(String[] args) {
        PWRJ.setRpcNodeUrl("https://pwrrpc.pwrlabs.io/");
        System.out.println(getShareValue("0x32b3f8d2f336b3ecd2a111606fdcbad838534ef1"));
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
    public static String getOwnerOfVm(long vmId) {
        try {
            HttpGet request = new HttpGet(rpcNodeUrl + "/ownerOfVmId/?vmId=" + vmId);
            HttpResponse response = client.execute(request);

            if (response.getStatusLine().getStatusCode() == 200) {
                JSONObject object = new JSONObject(EntityUtils.toString(response.getEntity()));

                return object.getString("owner");
            } else if (response.getStatusLine().getStatusCode() == 400) {
                JSONObject object = new JSONObject(EntityUtils.toString(response.getEntity()));
                throw new RuntimeException("Failed with HTTP error 400 and message: " + object.getString("message"));
            } else {
                throw new RuntimeException("Failed with HTTP error code : " + response.getStatusLine().getStatusCode());
            }
        } catch (Exception e) {
            e.printStackTrace();
            return "0x0000000000000000000000000000000000000000";
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
    public static void updateFeePerByte() {
        try {
            HttpGet request = new HttpGet(rpcNodeUrl + "/feePerByte/");
            HttpResponse response = client.execute(request);

            if (response.getStatusLine().getStatusCode() == 200) {
                JSONObject object = new JSONObject(EntityUtils.toString(response.getEntity()));
                feePerByte = object.getLong("feePerByte");
            } else if (response.getStatusLine().getStatusCode() == 400) {
                JSONObject object = new JSONObject(EntityUtils.toString(response.getEntity()));
                throw new RuntimeException("Failed with HTTP error 400 and message: " + object.getString("message"));
            } else {
                throw new RuntimeException("Failed with HTTP error code : " + response.getStatusLine().getStatusCode());
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

//        HttpRequest request = HttpRequest.newBuilder()
//                .uri(URI.create(rpcNodeUrl + "/feePerByte/"))
//                .GET()
//                .header("Accept", "application/json")
//                .build();
//
//        HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
//
//        if (response.statusCode() == 200) {
//            JSONObject object = new JSONObject(response.body());
//            feePerByte = object.getLong("feePerByte");
//        } else if (response.statusCode() == 400) {
//            JSONObject object = new JSONObject(response.body());
//            throw new RuntimeException("Failed with HTTP error 400 and message: " + object.getString("message"));
//        } else {
//            throw new RuntimeException("Failed with HTTP error code : " + response.statusCode());
//        }
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
                System.out.printf(object.toString());
                return new Response(false, null, object.getString("message"));
            } else {
                throw new RuntimeException("Failed with HTTP error code : " + response.getStatusLine().getStatusCode());
            }
        } catch (Exception e) {
            return new Response(false, null, e.getMessage());
        }
    }

}
