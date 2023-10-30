package com.github.pwrlabs.pwrj.protocol;

import com.github.pwrlabs.pwrj.Block.Block;
import com.github.pwrlabs.pwrj.Transaction.Transaction;
import com.github.pwrlabs.pwrj.Transaction.TransferTxn;
import com.github.pwrlabs.pwrj.Transaction.VmDataTxn;
import com.github.pwrlabs.pwrj.Utils.Response;
import org.bouncycastle.util.encoders.Hex;
import org.json.JSONArray;
import org.json.JSONObject;
import org.web3j.crypto.Hash;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;

public class PWRJ {

    private static HttpClient client = HttpClient.newHttpClient();
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
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(rpcNodeUrl + "/feePerByte/"))
                .GET()
                .header("Accept", "application/json")
                .build();

        HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());

        if (response.statusCode() == 200) {
            JSONObject object = new JSONObject(response.body());
            if(!object.getString("status").equalsIgnoreCase("success")) {
                throw new RuntimeException("Failed with error message: " + object.getString("message"));
            } else {
                feePerByte = object.getJSONObject("data").getLong("feePerByte");
            }
        } else {
            throw new RuntimeException("Failed with HTTP error code : " + response.statusCode());
        }
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
    public static Response broadcastTxn(byte[] txn) throws IOException, InterruptedException {
        JSONObject object = new JSONObject();
        object.put("txn", Hex.toHexString(txn));

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(rpcNodeUrl + "/broadcast/"))
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(object.toString()))
                .build();

        HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
        if (response.statusCode() == 200) {
            JSONObject responseJson = new JSONObject(response.body());
            if (responseJson.getString("status").equalsIgnoreCase("success")) {
                return new Response(true, responseJson.getString("message"), null);
            } else {
                return new Response(false, null, responseJson.getString("message"));
            }
        } else {
            throw new RuntimeException("Failed with HTTP error code : " + response.statusCode());
        }
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
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(rpcNodeUrl + "/balanceOf/?userAddress=" + address))
                .GET()
                .header("Accept", "application/json")
                .build();

        HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());

        if (response.statusCode() == 200) {
            JSONObject object = new JSONObject(response.body());
            if(!object.getString("status").equalsIgnoreCase("success")) {
                throw new RuntimeException("Failed with error message: " + object.getString("message"));
            } else {
                return object.getJSONObject("data").getLong("balance");
            }
        } else {
            throw new RuntimeException("Failed with HTTP error code : " + response.statusCode());
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
    public static int getNonceOfAddress(String address) throws IOException, InterruptedException {
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(rpcNodeUrl + "/nonceOfUser/?userAddress=" + address))
                .GET()
                .header("Accept", "application/json")
                .build();

        HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());

        if (response.statusCode() == 200) {
            JSONObject object = new JSONObject(response.body());
            if(!object.getString("status").equalsIgnoreCase("success")) {
                throw new RuntimeException("Failed with error message: " + object.getString("message"));
            } else {
                return object.getJSONObject("data").getInt("nonce");
            }
        } else {
            throw new RuntimeException("Failed with HTTP error code : " + response.statusCode());
        }
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
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(rpcNodeUrl + "/block/?blockNumber=" + blockNumber))
                .GET()
                .header("Accept", "application/json")
                .build();

        HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());

        if (response.statusCode() == 200) {
            JSONObject object = new JSONObject(response.body());
            if(!object.getString("status").equalsIgnoreCase("success")) {
                throw new RuntimeException("Failed with error message: " + object.getString("message"));
            } else {
                JSONObject blockJson = object.getJSONObject("data");
                return new Block(blockJson);
            }
        } else {
            throw new RuntimeException("Failed with HTTP error code : " + response.statusCode());
        }
    }
}
