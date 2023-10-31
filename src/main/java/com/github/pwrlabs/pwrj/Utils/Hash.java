package com.github.pwrlabs.pwrj.Utils;

import org.bouncycastle.jcajce.provider.digest.Keccak;

public class Hash {
    private static Keccak.DigestKeccak keccak = new Keccak.Digest256();

    /**
     * Returns the Keccak-256 hash of {@code input}.
     *
     * @param input the input data
     * @return the hash digest
     */
    public static byte[] keccak256(byte[] input) {
        return keccak.digest(input);
    }
}
