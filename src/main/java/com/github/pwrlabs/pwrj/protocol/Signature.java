package com.github.pwrlabs.pwrj.protocol;

import org.bouncycastle.util.encoders.Hex;
import org.web3j.crypto.ECKeyPair;
import org.web3j.crypto.Keys;
import org.web3j.crypto.Sign;
import org.web3j.crypto.Sign.SignatureData;

import java.math.BigInteger;
import java.security.SignatureException;
import java.util.Arrays;

public class Signature {
	
	public static byte[] signMessage(byte[] message, ECKeyPair eckeypair) {
		SignatureData sig =Sign.signMessage(message, eckeypair);
		byte[] output = new byte[65];
		
		System.arraycopy(sig.getR(), 0, output, 0, 32);
		System.arraycopy(sig.getS(), 0, output, 32, 32);
		System.arraycopy(sig.getV(), 0, output, 64, 1);
		
		return output;
	}

	public static byte[] getSigner(byte[] txn, byte[] signature) {
		try {
			SignatureData sigData = new SignatureData(signature[64], Arrays.copyOfRange(signature, 0, 32), Arrays.copyOfRange(signature, 32, 64));
			BigInteger pubKey = Sign.signedMessageToKey(txn, sigData);
			return Hex.decode(Keys.getAddress(pubKey));
		} catch (SignatureException e) {
			throw new RuntimeException("Failed to get signer from signature", e);
		}
	}

}
