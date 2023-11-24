package com.github.pwrlabs.pwrj.protocol;

import com.github.pwrlabs.pwrj.Block.Block;
import com.github.pwrlabs.pwrj.Utils.Hash;
import com.github.pwrlabs.pwrj.Utils.Response;
import com.github.pwrlabs.pwrj.wallet.PWRWallet;
import org.apache.http.impl.client.HttpClients;
import org.bouncycastle.jce.provider.BouncyCastleProvider;
import org.bouncycastle.util.encoders.Hex;
import org.json.JSONArray;
import org.json.JSONObject;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
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
    public static void updateFeePerByte() throws IOException, InterruptedException {
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
//        // Convert response entity to string
//        String responseBody = EntityUtils.toString(response.getEntity());
//
//        // Output the response
//        System.out.println(responseBody);
//
//        JSONObject object = new JSONObject();
//        object.put("txn", Hex.toHexString(txn));
//
//        HttpRequest request = HttpRequest.newBuilder()
//                .uri(URI.create(rpcNodeUrl + "/broadcast/"))
//                .header("Content-Type", "application/json")
//                .POST(HttpRequest.BodyPublishers.ofString(object.toString()))
//                .build();
//
//        try {
//            HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
//            if (response.statusCode() == 200) {
//                JSONObject responseJson = new JSONObject(response.body());
//                return new Response(true, "0x" + Hex.toHexString(Hash.sha3(txn)), null);
//            } else if (response.statusCode() == 400) {
//                JSONObject responseJson = new JSONObject(response.body());
//                return new Response(false, null, responseJson.getString("message"));
//            } else {
//                throw new RuntimeException("Failed with HTTP error code : " + response.statusCode());
//            }
//        } catch (Exception e) {
//            return new Response(false, null, e.getMessage());
//        }
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
     * Retrieves the latest block from the RPC node.
     *
     * <p>This method utilizes the {@link #getLatestBlockNumber()} method to get the number of the latest block
     * and then calls {@link #getBlockByNumber(long)} to fetch the block details.</p>
     *
     * @return The latest block.
     * @throws IOException If there's an issue with the network or stream handling.
     * @throws InterruptedException If the request is interrupted.
     * @throws RuntimeException If there are issues retrieving the latest block number or fetching the block details.
     */
    public static int getTotalValidatorsCount() throws IOException, InterruptedException {
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

//    public static int getActiveValidatorsCount() throws IOException, InterruptedException {
//        HttpRequest request = HttpRequest.newBuilder()
//                .uri(URI.create(rpcNodeUrl + "/activeValidatorsCount/"))
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
//    }

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
    public static Block getBlockByNumber(long blockNumber) throws IOException, InterruptedException {
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
            e.printStackTrace();
            return null;
        }
//
//        HttpRequest request = HttpRequest.newBuilder()
//                .uri(URI.create(rpcNodeUrl + "/block/?blockNumber=" + blockNumber))
//                .GET()
//                .header("Accept", "application/json")
//                .build();
//
//        HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
//
//        if (response.statusCode() == 200) {
//            JSONObject object = new JSONObject(response.body());
//            JSONObject blockJson = object.getJSONObject("block");
//            return new Block(blockJson);
//        } else {
//            throw new RuntimeException("Failed with HTTP error code : " + response.statusCode());
//        }
    }

}
