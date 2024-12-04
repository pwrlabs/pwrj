package com.github.pwrlabs.pwrj.wallet;

import com.github.pwrlabs.pwrj.Utils.Falcon;
import com.github.pwrlabs.pwrj.protocol.PWRJ;
import org.bouncycastle.crypto.AsymmetricCipherKeyPair;

import java.math.BigInteger;

public class PWRFalcon512Wallet {

    private final AsymmetricCipherKeyPair keyPair;
    private PWRJ pwrj;

    public PWRFalcon512Wallet(PWRJ pwrj) {
        this.pwrj = pwrj;
        this.keyPair = Falcon.generateKeyPair512();
    }

    public PWRFalcon512Wallet(PWRJ pwrj, AsymmetricCipherKeyPair keyPair) {
        this.pwrj = pwrj;
        this.keyPair = keyPair;
    }



}
