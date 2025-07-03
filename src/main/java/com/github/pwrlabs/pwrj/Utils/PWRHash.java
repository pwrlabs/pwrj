package com.github.pwrlabs.pwrj.Utils;

import org.bouncycastle.jcajce.provider.digest.Keccak;
import org.bouncycastle.jce.provider.BouncyCastleProvider;

import java.security.Security;

import static com.github.pwrlabs.pwrj.Utils.NewError.errorIf;

/**
 * PWRHash class.
 */
public class PWRHash {

    static {
        Security.addProvider(new BouncyCastleProvider());
    }

/**
 * hash224 method.
 * @param input parameter
 * @return value
 */
    public static byte[] hash224(byte[] input) { 

        Keccak.DigestKeccak keccak224 = new Keccak.Digest224();
        return keccak224.digest(input);
    }

/**
 * hash256 method.
 * @param input parameter
 * @return value
 */
    public static byte[] hash256(byte[] input) { 

        errorIf(input == null, "Input is null");
        Keccak.DigestKeccak keccak256 = new Keccak.Digest256();
        return keccak256.digest(input);
    }

/**
 * hash256 method.
 * @param input1 parameter
 * @param input2 parameter
 * @return value
 */
    public static byte[] hash256(byte[] input1, byte[] input2) {
        Keccak.DigestKeccak keccak256 = new Keccak.Digest256();
        keccak256.update(input1, 0, input1.length);
        keccak256.update(input2, 0, input2.length);
        return keccak256.digest();
    }
}
