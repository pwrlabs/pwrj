package com.github.pwrlabs.pwrnosqldb;

import io.pwrlabs.util.encoders.ByteArrayWrapper;
import okhttp3.internal.http2.Settings;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Arrays;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.SynchronousQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

public class PwrBoSQLDBCached {
    private static final Logger logger = LoggerFactory.getLogger(PwrBoSQLDBCached.class);

    private final PwrNoSQLDB db;
    private final Map<ByteArrayWrapper, byte[]> cache = new ConcurrentHashMap<>();
    public final ThreadPoolExecutor nonDaemonExecutor = new ThreadPoolExecutor(
            1,
            1000000,
            1L,
            TimeUnit.SECONDS,
            new SynchronousQueue<Runnable>(),
            r -> {
                Thread t = new Thread(r);
                t.setName("NonDaemonThread-" + r.hashCode() + "-" + System.currentTimeMillis());
                t.setDaemon(false);  // Make non-daemon threads
                return t;
            }
    );
    public PwrBoSQLDBCached(String serverUrl, String projectId, String secret) {
        db = new PwrNoSQLDB(serverUrl, projectId, secret);
    }

    public void put(byte[] key, byte[] value) {
        byte[] oldValue = cache.put(new ByteArrayWrapper(key), value);

        // If oldValue is same as new value, no need to update db
        // If oldValue is null, it means this key is being inserted for the first time, so we need to update db
        if(oldValue == null || !Arrays.equals(oldValue, value)) {
            nonDaemonExecutor.execute(() -> {
                // Retry until success or cache is updated
                // If cache is updated, it means a new put request has been made for the same key
                while (Arrays.equals(cache.get(new ByteArrayWrapper(key)), value)) {
                    boolean success = false;
                    try {
                        success = db.put(key, value);
                    } catch (Exception e) {
                        e.printStackTrace();
                    }

                    if (success) {
                        logger.info("Successfully updated key on PWR Chain: " + new String(key));
                        return;
                    }
                    else {
                        logger.warn("Failed to update key on PWR Chain, retrying: " + new String(key));
                        byte[] _value = db.getValue(key);
                        if (_value != null && Arrays.equals(_value, value))
                            return; // Another thread has already updated the value
                        else {
                            try {
                                Thread.sleep(10);
                            } catch (InterruptedException e) {
                                e.printStackTrace();
                            }
                        }
                    }
                }
            });
        }
    }

    public void put(Object key, Object value) {
        if (key == null) throw new IllegalArgumentException("Key cannot be null");
        if (value == null) throw new IllegalArgumentException("Value cannot be null");

        byte[] keyBytes;
        byte[] valueBytes;

        if (key instanceof String) {
            keyBytes = ((String) key).getBytes();
        } else if (key instanceof byte[]) {
            keyBytes = (byte[]) key;
        } else if (key instanceof Number) {
            keyBytes = key.toString().getBytes();
        } else {
            throw new IllegalArgumentException("Key must be a String, byte[], or Number");
        }

        if (value instanceof String) {
            valueBytes = ((String) value).getBytes();
        } else if (value instanceof byte[]) {
            valueBytes = (byte[]) value;
        } else if (value instanceof Number) {
            valueBytes = value.toString().getBytes();
        } else {
            throw new IllegalArgumentException("Value must be a String, byte[], or Number");
        }

        put(keyBytes, valueBytes);
    }

    public byte[] getValue(byte[] key) {
        byte[] value = cache.get(new ByteArrayWrapper(key));
        if(value != null) return value;

        try {
            value = db.getValue(key);
            if(value != null) {
                cache.put(new ByteArrayWrapper(key), value);
            }
            return value;
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    public byte[] getValue(Object key) {
        if (key == null) throw new IllegalArgumentException("Key cannot be null");

        byte[] keyBytes;

        if (key instanceof String) {
            keyBytes = ((String) key).getBytes();
        } else if (key instanceof byte[]) {
            keyBytes = (byte[]) key;
        } else if (key instanceof Number) {
            keyBytes = key.toString().getBytes();
        } else {
            throw new IllegalArgumentException("Key must be a String, byte[], or Number");
        }

        return getValue(keyBytes);
    }

    public String getStringValue(Object key) {
        byte[] value = getValue(key);
        if(value == null) return null;
        return new String(value);
    }

    public Integer getIntValue(Object key) {
        byte[] value = getValue(key);
        if(value == null) return null;
        return Integer.parseInt(new String(value));
    }

    public Long getLongValue(Object key) {
        byte[] value = getValue(key);
        if(value == null) return null;
        return Long.parseLong(new String(value));
    }

    public Double getDoubleValue(Object key) {
        byte[] value = getValue(key);
        if(value == null) return null;
        return Double.parseDouble(new String(value));
    }

    public void shutdown() {
        nonDaemonExecutor.shutdown();
        try {
            if (!nonDaemonExecutor.awaitTermination(60, TimeUnit.SECONDS)) {
                nonDaemonExecutor.shutdownNow();
            }
        } catch (InterruptedException e) {
            nonDaemonExecutor.shutdownNow();
            Thread.currentThread().interrupt();
        }
    }

    public static void main(String[] args) {
        String serverUrl = "http://209.38.243.13:8080";
        String projectId = "och9234bvlxwvhhkhbby";
        String projectSecret = "pwr_Hzxc0O3JoWqvIL20Za0rvCSkdRrGgrK4";

        PwrBoSQLDBCached db = new PwrBoSQLDBCached(serverUrl, projectId, projectSecret);

        byte[] key = "hello4435".getBytes();
        byte[] data = "world445iiioo".getBytes();

        long startTime = System.currentTimeMillis();
        db.put(key, data);
        long duration = System.currentTimeMillis() - startTime;
        System.out.println("Put success (took " + duration + " ms)");

        System.out.println("Retrieving value for key '" + new String(key) + "'...");
        byte[] retrieved = db.getValue(key);
        System.out.println("Retrieved value: '" + new String(retrieved) + "'");
        db.shutdown();


    }
}
