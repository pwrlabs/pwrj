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
import java.security.*;
import java.util.Arrays;

import org.bouncycastle.crypto.params.ECPrivateKeyParameters;
import org.bouncycastle.crypto.signers.ECDSASigner;
import org.bouncycastle.jce.provider.BouncyCastleProvider;
import org.bouncycastle.util.encoders.Hex;

import java.math.BigInteger;
import java.security.Security;

public class Signature {


	//Do not touch, keep in this order
	private static final ECDomainParameters SECP256K1_CURVE;

	static {
		ECCurve curve = new ECCurve.Fp(
				new BigInteger("fffffffffffffffffffffffffffffffffffffffffffffffffffffffefffffc2f", 16), // q
				new BigInteger("0000000000000000000000000000000000000000000000000000000000000000", 16), // a
				new BigInteger("0000000000000000000000000000000000000000000000000000000000000007", 16)); // b
		ECPoint G = curve.createPoint(
				new BigInteger("55066263022277343669578718895168534326250603453777594175500187360389116729240"),
				new BigInteger("32670510020758816978083085130507043184471273380659243275938904335757337482424"));
		BigInteger n = new BigInteger("fffffffffffffffffffffffffffffffebaaedce6af48a03bbfd25e8cd0364141", 16);
		BigInteger h = BigInteger.ONE;
		SECP256K1_CURVE = new ECDomainParameters(curve, G, n, h);
	}

	/**
	 * Signs a message using the ECDSA (Elliptic Curve Digital Signature Algorithm) with the given private key.
	 *
	 * @param message      The message to be signed as a byte array.
	 * @param privateKey   The private key used for signing as a BigInteger.
	 * @return             A byte array representing the signature of the message.
	 */
	public static byte[] signMessage(byte[] message, BigInteger privateKey) {
		ECDSASigner signer = new ECDSASigner();
		ECPrivateKeyParameters privateKeyParameters = new ECPrivateKeyParameters(privateKey, SECP256K1_CURVE);
		signer.init(true, privateKeyParameters);

		byte[] messageHash = Hash.keccak256(message);

		// Sign the hash
		BigInteger[] signatureComponents = signer.generateSignature(messageHash);
		int recId = 0;
		if (signatureComponents[0].toByteArray()[0] > 0x80 || signatureComponents[1].toByteArray()[0] > 0x80) {
			recId = 1;
		}

		byte v = (byte) (recId + 27);
		byte[] r = BigIntegers.asUnsignedByteArray(32, signatureComponents[0]);
		byte[] s = BigIntegers.asUnsignedByteArray(32, signatureComponents[1]);

		ByteBuffer signature = ByteBuffer.allocate(65);
		signature.put(r);
		signature.put(s);
		signature.put(v);

		return signature.array();
	}

}
