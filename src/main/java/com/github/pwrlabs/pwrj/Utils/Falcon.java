package com.github.pwrlabs.pwrj.Utils;
import org.bouncycastle.crypto.AsymmetricCipherKeyPair;
import org.bouncycastle.jce.provider.BouncyCastleProvider;
import org.bouncycastle.pqc.crypto.falcon.*;

import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.security.Security;

/**
 * Falcon class.
 */
public class Falcon {
    private static final FalconParameters params512 = FalconParameters.falcon_512;
    private static final FalconParameters params1024 = FalconParameters.falcon_1024;

    static {
        Security.addProvider(new BouncyCastleProvider());
    }

/**
 * generateKeyPair512 method.
 * @return value
 */
    public static AsymmetricCipherKeyPair generateKeyPair512() {
        FalconKeyPairGenerator keyPairGen = new FalconKeyPairGenerator();
        FalconKeyGenerationParameters keyGenParams = new FalconKeyGenerationParameters(new SecureRandom(), params512);
        keyPairGen.init(keyGenParams);
        return keyPairGen.generateKeyPair();
    }

    /**
     * Generate a deterministic Falcon-512 key pair from a seed
     * @param seed The seed bytes to derive the key pair from
     * @return The generated key pair
     */
/**
 * generateKeyPair512FromSeed method.
 * @param seed parameter
 * @return value
 */
    public static AsymmetricCipherKeyPair generateKeyPair512FromSeed(byte[] seed) {
        // Create a deterministic pseudo-random generator from the seed
        try {
            // Get SHA1PRNG instance which has more predictable behavior with seeds
            SecureRandom deterministicRandom = new DeterministicSecureRandom(seed);
            byte[] bytes = new byte[48];
            deterministicRandom.nextBytes(bytes);

            deterministicRandom = new DeterministicSecureRandom(seed);
            deterministicRandom.setSeed(seed);


            FalconKeyPairGenerator keyPairGen = new FalconKeyPairGenerator();
            FalconKeyGenerationParameters keyGenParams = new FalconKeyGenerationParameters(deterministicRandom, params512);
            keyPairGen.init(keyGenParams);
            return keyPairGen.generateKeyPair();
        } catch (NoSuchAlgorithmException e) {
            e.printStackTrace();
            throw new RuntimeException("SHA1PRNG not available", e);
        }
    }
/**
 * generateKeyPair1024 method.
 * @return value
 */
    public static  AsymmetricCipherKeyPair generateKeyPair1024() {
        FalconKeyPairGenerator keyPairGen = new FalconKeyPairGenerator();
        FalconKeyGenerationParameters keyGenParams = new FalconKeyGenerationParameters(new SecureRandom(), params1024);
        keyPairGen.init(keyGenParams);
        return keyPairGen.generateKeyPair();
    }

/**
 * sign method.
 * @param message parameter
 * @param keyPair parameter
 * @return value
 */
    public static byte[] sign(byte[] message, AsymmetricCipherKeyPair keyPair) {
        FalconSigner signer = new FalconSigner();
        FalconPrivateKeyParameters privateKey = (FalconPrivateKeyParameters) keyPair.getPrivate();
        signer.init(true, privateKey);
        return signer.generateSignature(message);
    }

/**
 * verify512 method.
 * @param message parameter
 * @param signature parameter
 * @param publicKey parameter
 * @return value
 */
    public static boolean verify512(byte[] message, byte[] signature, byte[] publicKey) {
        try {
            // Handle the case where the public key includes the 0x09 prefix
            byte[] processedKey = publicKey;
            if (publicKey.length == 897 && publicKey[0] == 0x09) {
                processedKey = new byte[publicKey.length - 1];
                System.arraycopy(publicKey, 1, processedKey, 0, processedKey.length);
            }

            FalconPublicKeyParameters publicKeyParams = new FalconPublicKeyParameters(params512, processedKey);
            FalconSigner signer = new FalconSigner();
            signer.init(false, publicKeyParams);
            return signer.verifySignature(message, signature);
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }

    //TODO check for any prefix mismatch with other tools and fix it like we did in 512
/**
 * verify1024 method.
 * @param message parameter
 * @param signature parameter
 * @param publicKey parameter
 * @return value
 */
    public static boolean verify1024(byte[] message, byte[] signature, byte[] publicKey) {
        try {
            FalconPublicKeyParameters publicKeyParams = new FalconPublicKeyParameters(params1024, publicKey);
            FalconSigner signer = new FalconSigner();
            signer.init(false, publicKeyParams);
            return signer.verifySignature(message, signature);
        } catch (Exception e) {
            e.printStackTrace();
            // Handle any potential exceptions during key conversion or verification
            return false;
        }
    }

}

