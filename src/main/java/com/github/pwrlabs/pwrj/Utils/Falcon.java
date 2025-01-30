package com.github.pwrlabs.pwrj.Utils;
import org.bouncycastle.crypto.AsymmetricCipherKeyPair;
import org.bouncycastle.jce.provider.BouncyCastleProvider;
import org.bouncycastle.pqc.crypto.falcon.*;

import java.security.SecureRandom;
import java.security.Security;

public class Falcon {
    private static final FalconParameters params512 = FalconParameters.falcon_512;
    private static final FalconParameters params1024 = FalconParameters.falcon_1024;

    static {
        Security.addProvider(new BouncyCastleProvider());
    }

    public static AsymmetricCipherKeyPair generateKeyPair512() {
        FalconKeyPairGenerator keyPairGen = new FalconKeyPairGenerator();
        FalconKeyGenerationParameters keyGenParams = new FalconKeyGenerationParameters(new SecureRandom(), params512);
        keyPairGen.init(keyGenParams);
        return keyPairGen.generateKeyPair();
    }

    public static  AsymmetricCipherKeyPair generateKeyPair1024() {
        FalconKeyPairGenerator keyPairGen = new FalconKeyPairGenerator();
        FalconKeyGenerationParameters keyGenParams = new FalconKeyGenerationParameters(new SecureRandom(), params1024);
        keyPairGen.init(keyGenParams);
        return keyPairGen.generateKeyPair();
    }

    public static byte[] sign(byte[] message, AsymmetricCipherKeyPair keyPair) {
        FalconSigner signer = new FalconSigner();
        FalconPrivateKeyParameters privateKey = (FalconPrivateKeyParameters) keyPair.getPrivate();
        signer.init(true, privateKey);
        return signer.generateSignature(message);
    }

    public static boolean verify512(byte[] message, byte[] signature, byte[] publicKey) {
        try {
            FalconPublicKeyParameters publicKeyParams = new FalconPublicKeyParameters(params512, publicKey);
            FalconSigner signer = new FalconSigner();
            signer.init(false, publicKeyParams);
            return signer.verifySignature(message, signature);
        } catch (Exception e) {
            // Handle any potential exceptions during key conversion or verification
            return false;
        }
    }

    public static boolean verify1024(byte[] message, byte[] signature, byte[] publicKey) {
        try {
            FalconPublicKeyParameters publicKeyParams = new FalconPublicKeyParameters(params1024, publicKey);
            FalconSigner signer = new FalconSigner();
            signer.init(false, publicKeyParams);
            return signer.verifySignature(message, signature);
        } catch (Exception e) {
            // Handle any potential exceptions during key conversion or verification
            return false;
        }
    }



    public static void main(String[] args) throws Exception {

        //generate falcon wallet and output public keyy length

        AsymmetricCipherKeyPair keyPair = generateKeyPair1024();
        FalconPrivateKeyParameters privateKey = (FalconPrivateKeyParameters) keyPair.getPrivate();
        FalconPublicKeyParameters publicKey = (FalconPublicKeyParameters) keyPair.getPublic();
        byte[] pk = publicKey.getH();
        System.out.println("Public key length: " + pk.length);

    }
}

