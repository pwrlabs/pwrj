package com.github.pwrlabs.pwrj.Utils;

import java.math.BigInteger;
import java.nio.charset.StandardCharsets;
import java.security.InvalidAlgorithmParameterException;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.KeySpec;
import javax.crypto.BadPaddingException;
import javax.crypto.Cipher;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.NoSuchPaddingException;
import javax.crypto.SecretKey;
import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.PBEKeySpec;
import javax.crypto.spec.SecretKeySpec;

/**
 * AES256 class.
 */
public class AES256 {
    private static final int ITERATION_COUNT = 65536;
    private static final int KEY_LENGTH = 256;
    private static final String SALT = "your-salt-value";

/**
 * encrypt method.
 * @param data parameter
 * @param password parameter
 * @return value
 * @throws NoSuchAlgorithmException exception
 * @throws InvalidKeySpecException exception
 * @throws NoSuchPaddingException exception
 * @throws InvalidKeyException exception
 * @throws InvalidAlgorithmParameterException exception
 * @throws IllegalBlockSizeException exception
 * @throws BadPaddingException exception
 */
    public static byte[] encrypt(byte[] data, String password) throws NoSuchAlgorithmException, InvalidKeySpecException, NoSuchPaddingException, InvalidKeyException, InvalidAlgorithmParameterException, IllegalBlockSizeException, BadPaddingException {
        SecretKeyFactory factory = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256");
        KeySpec keySpec = new PBEKeySpec(password.toCharArray(), "your-salt-value".getBytes(StandardCharsets.UTF_8), 65536, 256);
        SecretKey secretKey = new SecretKeySpec(factory.generateSecret(keySpec).getEncoded(), "AES");
        Cipher cipher = Cipher.getInstance("AES/CBC/PKCS5Padding");
        IvParameterSpec ivSpec = new IvParameterSpec(generateIV());
        cipher.init(1, secretKey, ivSpec);
        byte[] encryptedData = cipher.doFinal(data);
        byte[] iv = cipher.getIV();
        byte[] encryptedDataWithIV = new byte[iv.length + encryptedData.length];
        System.arraycopy(iv, 0, encryptedDataWithIV, 0, iv.length);
        System.arraycopy(encryptedData, 0, encryptedDataWithIV, iv.length, encryptedData.length);
        return encryptedDataWithIV;
    }

/**
 * decrypt method.
 * @param encryptedDataWithIV parameter
 * @param password parameter
 * @return value
 * @throws NoSuchAlgorithmException exception
 * @throws InvalidKeySpecException exception
 * @throws NoSuchPaddingException exception
 * @throws InvalidKeyException exception
 * @throws InvalidAlgorithmParameterException exception
 * @throws IllegalBlockSizeException exception
 * @throws BadPaddingException exception
 */
    public static byte[] decrypt(byte[] encryptedDataWithIV, String password) throws NoSuchAlgorithmException, InvalidKeySpecException, NoSuchPaddingException, InvalidKeyException, InvalidAlgorithmParameterException, IllegalBlockSizeException, BadPaddingException {
        SecretKeyFactory factory = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256");
        KeySpec keySpec = new PBEKeySpec(password.toCharArray(), "your-salt-value".getBytes(StandardCharsets.UTF_8), 65536, 256);
        SecretKey secretKey = new SecretKeySpec(factory.generateSecret(keySpec).getEncoded(), "AES");
        Cipher cipher = Cipher.getInstance("AES/CBC/PKCS5Padding");
        byte[] iv = new byte[16];
        System.arraycopy(encryptedDataWithIV, 0, iv, 0, iv.length);
        byte[] encryptedData = new byte[encryptedDataWithIV.length - iv.length];
        System.arraycopy(encryptedDataWithIV, iv.length, encryptedData, 0, encryptedData.length);
        IvParameterSpec ivSpec = new IvParameterSpec(iv);
        cipher.init(2, secretKey, ivSpec);
        return cipher.doFinal(encryptedData);
    }

    private static byte[] generateIV() {
        SecureRandom random = new SecureRandom();
        byte[] iv = new byte[16];
        random.nextBytes(iv);
        return iv;
    }

/**
 * main method.
 * @param args parameter
 */
    public static void main(String[] args) {
        BigInteger t = new BigInteger("9872156793");

        try {
            byte[] encryptedData = encrypt(t.toByteArray(), "testlol");
            BigInteger newT = new BigInteger(decrypt(encryptedData, "testlol"));
            System.out.println(newT);
        } catch (NoSuchAlgorithmException | InvalidKeySpecException | NoSuchPaddingException | InvalidAlgorithmParameterException | IllegalBlockSizeException | BadPaddingException | InvalidKeyException var4) {
            var4.printStackTrace();
        }

    }
}