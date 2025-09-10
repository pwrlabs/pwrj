package com.github.pwrlabs.pwrnosqldb;

import io.pwrlabs.util.encoders.ByteArrayWrapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.crypto.BadPaddingException;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.NoSuchPaddingException;
import java.security.InvalidAlgorithmParameterException;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.security.spec.InvalidKeySpecException;
import java.util.Arrays;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.SynchronousQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

public class PowerKvCached {
    private static final Logger logger = LoggerFactory.getLogger(PowerKvCached.class);

    private final PowerKv db;
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
    public PowerKvCached(String projectId, String secret) {
        db = new PowerKv(projectId, secret);
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
                        byte[] _value = null;
                        try {
                            _value = db.getValue(key);
                        } catch (Exception e) {
                            throw new RuntimeException(e);
                        }
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
        String projectId = "npu6o3uooiijkmnawvjced";
        String projectSecret = "pwr_h3MmbZSKSPuf9L523E0Y6g==";

        PowerKvCached db = new PowerKvCached(projectId, projectSecret);

        for(int t=0; t < 5; ++t) {
            byte[] key = ("hello4435" + t).getBytes();
            byte[] data = ("world445iiioo" + t).getBytes();

            long startTime = System.currentTimeMillis();
            db.put(key, data);
            long duration = System.currentTimeMillis() - startTime;
            System.out.println("Put success (took " + duration + " ms)");
        }

        for (int t=0; t < 5; ++t) {
            byte[] key = ("hello4435" + t).getBytes();
            System.out.println("Retrieving value for key '" + new String(key) + "'...");
            byte[] retrieved = db.getValue(key);
            System.out.println("Retrieved value: '" + new String(retrieved, java.nio.charset.StandardCharsets.UTF_8) + "'");
        }

        long startTime = System.currentTimeMillis();
        db.shutdown();
        long duration = System.currentTimeMillis() - startTime;

        System.out.println("Shutdown complete (took " + duration + " ms)");
    }
}
