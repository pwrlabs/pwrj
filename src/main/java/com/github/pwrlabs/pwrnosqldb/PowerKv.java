package com.github.pwrlabs.pwrnosqldb;

import com.github.pwrlabs.pwrj.Utils.Hex;
import org.json.JSONObject;

import java.io.IOException;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;

public class PowerKv {
    private final HttpClient http = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(10)) // connect timeout
            .build();;
    private final String serverUrl = "https://pwrnosqlvida.pwrlabs.io/";
    private final String projectId;
    private final String secret;

    public PowerKv(String projectId, String secret) {
        if(projectId == null || projectId.isEmpty()) {
            throw new IllegalArgumentException("Project ID cannot be null or empty");
        }
        if(secret == null || secret.isEmpty()) {
            throw new IllegalArgumentException("Secret cannot be null or empty");
        }

        this.projectId = projectId;
        this.secret = secret;
    }

    public String getServerUrl() {
        return serverUrl;
    }

    public String getProjectId() {
        return projectId;
    }

    public boolean put(byte[] key, byte[] data) throws IOException, RuntimeException, InterruptedException {
        //curl POST serverUrl + "/storeData"

        String url = serverUrl + "/storeData";

        JSONObject payload = new JSONObject()
                .put("projectId", projectId)
                .put("secret", secret)
                .put("key", Hex.toHexString(key))
                .put("value", Hex.toHexString(data));

        HttpRequest req = HttpRequest.newBuilder()
                .uri(URI.create(url))
                .timeout(Duration.ofSeconds(10)) // overall request timeout (incl. read)
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(payload.toString(), StandardCharsets.UTF_8))
                .build();

        HttpResponse<String> resp;
        try {
            resp = http.send(req, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
        } catch (IOException | InterruptedException e) {
            throw e;
        }

        int code = resp.statusCode();
        String body = resp.body() == null ? "" : resp.body();

        // The server returns 200 on success; 400 with {"message": "..."} on error.
        if (code == 200) {
            return true;
        } else {
            String msg;
            try {
                msg = new JSONObject(body).optString("message", "HTTP " + code);
            } catch (Exception parseErr) {
                msg = "HTTP " + code + " — " + body;
            }
            throw new RuntimeException("storeData failed: " + msg);
        }
    }

    public boolean put(Object key, Object value) throws IOException, InterruptedException {
        if(key == null) throw new IllegalArgumentException("Key cannot be null");
        if(value == null) throw new IllegalArgumentException("Data cannot be null");

        byte[] keyBytes;
        byte[] dataBytes;

        if(key instanceof String) {
            keyBytes = ((String) key).getBytes(StandardCharsets.UTF_8);
        } else if(key instanceof byte[]) {
            keyBytes = (byte[]) key;
        } else if(key instanceof Number) {
            keyBytes = key.toString().getBytes(StandardCharsets.UTF_8);
        } else {
            throw new IllegalArgumentException("Key must be a String or byte[]");
        }

        if(value instanceof String) {
            dataBytes = ((String) value).getBytes(StandardCharsets.UTF_8);
        } else if(value instanceof byte[]) {
            dataBytes = (byte[]) value;
        } else if (value instanceof Number) {
            dataBytes = value.toString().getBytes(StandardCharsets.UTF_8);
        } else {
            throw new IllegalArgumentException("Data must be a String or byte[]");
        }

        return put(keyBytes, dataBytes);
    }

    public byte[] getValue(byte[] key) throws RuntimeException {
        if (key == null) throw new IllegalArgumentException("Key cannot be null");

        // Build URL: /getValue?projectId=...&key=<hex>
        String url = serverUrl + "/getValue"
                + "?projectId=" + URLEncoder.encode(projectId, StandardCharsets.UTF_8)
                + "&key=" + Hex.toHexString(key); // server accepts plain hex (no 0x)

        HttpRequest req = HttpRequest.newBuilder()
                .uri(URI.create(url))
                .timeout(Duration.ofSeconds(10))
                .GET()
                .build();

        HttpResponse<String> resp;
        try {
            resp = http.send(req, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
        } catch (IOException | InterruptedException e) {
            // Preserve interrupt status if interrupted
            if (e instanceof InterruptedException) Thread.currentThread().interrupt();
            throw new RuntimeException("GET /getValue failed (network/timeout)", e);
        }

        int code = resp.statusCode();
        String body = resp.body() == null ? "" : resp.body();

        if (code == 200) {
            // Expect: {"value":"<hex>"}
            try {
                String valueHex = new JSONObject(body).getString("value");
                // Accept both with/without 0x just in case
                if (valueHex.startsWith("0x") || valueHex.startsWith("0X")) {
                    valueHex = valueHex.substring(2);
                }
                return Hex.decode(valueHex);
            } catch (Exception parseErr) {
                throw new RuntimeException("Unexpected response shape from /getValue: " + body, parseErr);
            }
        } else {
            // Error format: {"message":"..."}
            String msg;
            try {
                msg = new JSONObject(body).optString("message", "HTTP " + code);
            } catch (Exception ignored) {
                msg = "HTTP " + code + " — " + body;
            }
            throw new RuntimeException("getValue failed: " + msg);
        }
    }

    /** Convenience overload mirroring put(Object, Object). */
    public byte[] getValue(Object key) {
        if (key == null) throw new IllegalArgumentException("Key cannot be null");
        byte[] keyBytes;
        if (key instanceof String) {
            keyBytes = ((String) key).getBytes(StandardCharsets.UTF_8);
        } else if (key instanceof byte[]) {
            keyBytes = (byte[]) key;
        } else if (key instanceof Number) {
            keyBytes = key.toString().getBytes(StandardCharsets.UTF_8);
        } else {
            throw new IllegalArgumentException("Key must be a String, byte[], or Number");
        }
        return getValue(keyBytes);
    }

    public String getStringValue(Object key) {
        byte[] data = getValue(key);
        return new String(data, StandardCharsets.UTF_8);
    }

    public Integer getIntValue(Object key) {
        byte[] data = getValue(key);
        String str = new String(data, StandardCharsets.UTF_8);
        return Integer.parseInt(str);
    }

    public Long getLongValue(Object key) {
        byte[] data = getValue(key);
        String str = new String(data, StandardCharsets.UTF_8);
        return Long.parseLong(str);
    }

    public Double getDoubleValue(Object key) {
        byte[] data = getValue(key);
        String str = new String(data, StandardCharsets.UTF_8);
        return Double.parseDouble(str);
    }

    public static void main(String[] args) throws IOException, InterruptedException {
        String projectId = "och9234bvlxwvhhkhbby";
        String projectSecret = "pwr_Hzxc0O3JoWqvIL20Za0rvCSkdRrGgrK4";

        PowerKv db = new PowerKv(projectId, projectSecret);

        byte[] key = "hello3".getBytes(StandardCharsets.UTF_8);
        byte[] data = "worldiiioo".getBytes(StandardCharsets.UTF_8);

        long startTime = System.currentTimeMillis();
        boolean success = db.put(key, data);
        long duration = System.currentTimeMillis() - startTime;
        System.out.println("Put success: " + success + " (took " + duration + " ms)");

        System.out.println("Retrieving value for key '" + new String(key) + "'...");
        byte[] retrieved = db.getValue(key);
        System.out.println("Retrieved value: '" + new String(retrieved, StandardCharsets.UTF_8) + "'");
    }

}

