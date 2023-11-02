package com.github.pwrlabs.pwrj.protocol;

import com.github.pwrlabs.pwrj.Utils.Hash;
import com.github.pwrlabs.pwrj.wallet.PWRWallet;
import org.bouncycastle.crypto.params.ECDomainParameters;
import org.bouncycastle.jcajce.provider.digest.Keccak;
import org.bouncycastle.math.ec.ECCurve;
import org.bouncycastle.math.ec.ECPoint;
import org.bouncycastle.util.BigIntegers;
import org.bouncycastle.util.encoders.Hex;

import java.math.BigInteger;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.security.*;
import java.util.Arrays;

import org.bouncycastle.crypto.params.ECPrivateKeyParameters;
import org.bouncycastle.crypto.signers.ECDSASigner;
import org.bouncycastle.jce.provider.BouncyCastleProvider;
import org.bouncycastle.util.encoders.Hex;

import java.math.BigInteger;
import java.security.Security;
import org.bouncycastle.util.encoders.Hex;
import org.web3j.crypto.Credentials;
import org.web3j.crypto.ECKeyPair;
import org.web3j.crypto.Keys;
import org.web3j.crypto.Sign;
import org.web3j.crypto.Sign.SignatureData;

import java.math.BigInteger;
import java.nio.charset.StandardCharsets;
import java.security.InvalidAlgorithmParameterException;
import java.security.NoSuchAlgorithmException;
import java.security.NoSuchProviderException;
import java.security.SignatureException;
import java.util.Arrays;


public class Signature {

	public static byte[] signMessage(byte[] message, ECKeyPair eckeypair) {
		Sign.SignatureData sig =Sign.signMessage(message, eckeypair);
		byte[] output = new byte[65];

		System.arraycopy(sig.getR(), 0, output, 0, 32);
		System.arraycopy(sig.getS(), 0, output, 32, 32);
		System.arraycopy(sig.getV(), 0, output, 64, 1);

		return output;
	}

	public static byte[] signMessage(byte[] message, BigInteger privateKey) {
		return signMessage(message, ECKeyPair.create(privateKey));
	}

}
