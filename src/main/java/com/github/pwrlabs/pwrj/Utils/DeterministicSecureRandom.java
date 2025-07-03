package com.github.pwrlabs.pwrj.Utils;

import java.nio.ByteBuffer;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;

/**
 * DeterministicSecureRandom class.
 */
public class DeterministicSecureRandom extends SecureRandom {
    private final MessageDigest digest;
    private final byte[] seed;
    private int counter = 0;

    public DeterministicSecureRandom(byte[] seed) throws NoSuchAlgorithmException {
        this.seed = seed.clone();
        this.digest = MessageDigest.getInstance("SHA-256");
    }

    @Override
/**
 * nextBytes method.
 * @param bytes parameter
 */
    public void nextBytes(byte[] bytes) {
        int index = 0;
        while (index < bytes.length) {
            digest.reset();
            digest.update(seed);
            digest.update(ByteBuffer.allocate(4).putInt(counter++).array());
            byte[] hash = digest.digest();

            int toCopy = Math.min(hash.length, bytes.length - index);
            System.arraycopy(hash, 0, bytes, index, toCopy);
            index += toCopy;
        }
    }
}