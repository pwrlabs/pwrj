package com.github.pwrlabs.pwrj.protocol;

import org.bouncycastle.asn1.x9.X9ECParameters;
import org.bouncycastle.asn1.x9.X9IntegerConverter;
import org.bouncycastle.crypto.digests.SHA256Digest;
import org.bouncycastle.crypto.ec.CustomNamedCurves;
import org.bouncycastle.crypto.params.ECDomainParameters;
import org.bouncycastle.crypto.signers.HMacDSAKCalculator;
import org.bouncycastle.math.ec.ECAlgorithms;
import org.bouncycastle.math.ec.ECPoint;
import org.bouncycastle.math.ec.FixedPointCombMultiplier;
import org.bouncycastle.math.ec.custom.sec.SecP256K1Curve;

import java.math.BigInteger;
import java.util.Arrays;

import org.bouncycastle.crypto.params.ECPrivateKeyParameters;
import org.bouncycastle.crypto.signers.ECDSASigner;

import static com.github.pwrlabs.pwrj.protocol.Signature.CURVE;
import static com.github.pwrlabs.pwrj.protocol.Signature.HALF_CURVE_ORDER;

public class Signature {

	public static final X9ECParameters CURVE_PARAMS = CustomNamedCurves.getByName("secp256k1");
	public static final ECDomainParameters CURVE;
	public static final BigInteger HALF_CURVE_ORDER;

	static {
		CURVE = new ECDomainParameters(CURVE_PARAMS.getCurve(), CURVE_PARAMS.getG(), CURVE_PARAMS.getN(), CURVE_PARAMS.getH());
		HALF_CURVE_ORDER = CURVE_PARAMS.getN().shiftRight(1);
	}

//	public static byte[] signMessage(byte[] message, ECKeyPair eckeypair) {
//		Sign.SignatureData sig =Sign.signMessage(message, eckeypair);
//		byte[] output = new byte[65];
//
//		System.arraycopy(sig.getR(), 0, output, 0, 32);
//		System.arraycopy(sig.getS(), 0, output, 32, 32);
//		System.arraycopy(sig.getV(), 0, output, 64, 1);
//
//		return output;
//	}

	public static byte[] signMessage(byte[] message, BigInteger privateKey) {
		BigInteger publicKey = publicKeyFromPrivate(privateKey);
		byte[] messageHash = Hash.sha3(message);

		ECDSASignature sig = sign(privateKey, messageHash);
		int recId = -1;

		int headerByte;
		for(headerByte = 0; headerByte < 4; ++headerByte) {
			BigInteger k = recoverFromSignature(headerByte, sig, messageHash);
			if (k != null && k.equals(publicKey)) {
				recId = headerByte;
				break;
			}
		}

		if (recId == -1) {
			throw new RuntimeException("Could not construct a recoverable key. Are your credentials valid?");
		} else {
			headerByte = recId + 27;
			byte[] v = new byte[]{(byte)headerByte};
			byte[] r = toBytesPadded(sig.r, 32);
			byte[] s = toBytesPadded(sig.s, 32);

			byte[] signature = new byte[65];
			//r,s,v
			System.arraycopy(r, 0, signature, 0, 32);
			System.arraycopy(s, 0, signature, 32, 32);
			System.arraycopy(v, 0, signature, 64, 1);

			return signature;
		}
	}


	public static BigInteger recoverFromSignature(int recId, ECDSASignature sig, byte[] message) {
		verifyPrecondition(recId >= 0, "recId must be positive");
		verifyPrecondition(sig.r.signum() >= 0, "r must be positive");
		verifyPrecondition(sig.s.signum() >= 0, "s must be positive");
		verifyPrecondition(message != null, "message cannot be null");
		BigInteger n = CURVE.getN();
		BigInteger i = BigInteger.valueOf((long)recId / 2L);
		BigInteger x = sig.r.add(i.multiply(n));
		BigInteger prime = SecP256K1Curve.q;
		if (x.compareTo(prime) >= 0) {
			return null;
		} else {
			ECPoint R = decompressKey(x, (recId & 1) == 1);
			if (!R.multiply(n).isInfinity()) {
				return null;
			} else {
				BigInteger e = new BigInteger(1, message);
				BigInteger eInv = BigInteger.ZERO.subtract(e).mod(n);
				BigInteger rInv = sig.r.modInverse(n);
				BigInteger srInv = rInv.multiply(sig.s).mod(n);
				BigInteger eInvrInv = rInv.multiply(eInv).mod(n);
				ECPoint q = ECAlgorithms.sumOfTwoMultiplies(CURVE.getG(), eInvrInv, R, srInv);
				byte[] qBytes = q.getEncoded(false);
				return new BigInteger(1, Arrays.copyOfRange(qBytes, 1, qBytes.length));
			}
		}
	}

	public static ECDSASignature sign(BigInteger privateKey, byte[] transactionHash) {
		ECDSASigner signer = new ECDSASigner(new HMacDSAKCalculator(new SHA256Digest()));
		ECPrivateKeyParameters privKey = new ECPrivateKeyParameters(privateKey, CURVE);
		signer.init(true, privKey);
		BigInteger[] components = signer.generateSignature(transactionHash);
		return (new ECDSASignature(components[0], components[1])).toCanonicalised();
	}

	private static ECPoint decompressKey(BigInteger xBN, boolean yBit) {
		X9IntegerConverter x9 = new X9IntegerConverter();
		byte[] compEnc = x9.integerToBytes(xBN, 1 + x9.getByteLength(CURVE.getCurve()));
		compEnc[0] = (byte)(yBit ? 3 : 2);
		return CURVE.getCurve().decodePoint(compEnc);
	}

	public static BigInteger publicKeyFromPrivate(BigInteger privKey) {
		ECPoint point = publicPointFromPrivate(privKey);
		byte[] encoded = point.getEncoded(false);
		return new BigInteger(1, Arrays.copyOfRange(encoded, 1, encoded.length));
	}

	public static ECPoint publicPointFromPrivate(BigInteger privKey) {
		if (privKey.bitLength() > CURVE.getN().bitLength()) {
			privKey = privKey.mod(CURVE.getN());
		}

		return (new FixedPointCombMultiplier()).multiply(CURVE.getG(), privKey);
	}

	public static byte[] toBytesPadded(BigInteger value, int length) {
		byte[] result = new byte[length];
		byte[] bytes = value.toByteArray();
		int bytesLength;
		byte srcOffset;
		if (bytes[0] == 0) {
			bytesLength = bytes.length - 1;
			srcOffset = 1;
		} else {
			bytesLength = bytes.length;
			srcOffset = 0;
		}

		if (bytesLength > length) {
			throw new RuntimeException("Input is too large to put in byte array of size " + length);
		} else {
			int destOffset = length - bytesLength;
			System.arraycopy(bytes, srcOffset, result, destOffset, bytesLength);
			return result;
		}
	}

	public static void verifyPrecondition(boolean assertionResult, String errorMessage) {
		if (!assertionResult) {
			throw new RuntimeException(errorMessage);
		}
	}

}

class ECDSASignature {
	public final BigInteger r;
	public final BigInteger s;

	public ECDSASignature(BigInteger r, BigInteger s) {
		this.r = r;
		this.s = s;
	}

	public boolean isCanonical() {
		return this.s.compareTo(HALF_CURVE_ORDER) <= 0;
	}

	public ECDSASignature toCanonicalised() {
		return !this.isCanonical() ? new ECDSASignature(this.r, CURVE.getN().subtract(this.s)) : this;
	}
}


