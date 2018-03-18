package net.yapbam.util;

import java.security.spec.*;
import java.util.*;
import javax.crypto.*;

/** A PBEWithMD5AndDES key that allows non ascii char in password.
 * <br>This class is copied from a <a href="http://stackoverflow.com/questions/809590/how-can-i-decode-a-pkcs5-encrypted-pkcs8-private-key-in-java">stackoverflow question</a>
 * <br><b>This class is now deprecated because it hangs with Android systems. The preferred solution is to use a Base64 encoding of the password before creating the key</b>.
 * @deprecated 
 */
@SuppressWarnings("serial")
@Deprecated
public class BinaryPBEKey implements SecretKey {
	private final byte[] key;

	/**
	 * Creates a PBE key from a given binary key.
	 * @param key The key.
	 * @throws InvalidKeySpecException if the key is invalid
	 */
	public BinaryPBEKey(byte[] key) throws InvalidKeySpecException {
		if (key == null) {
			this.key = new byte[0];
		} else {
			this.key = (byte[]) key.clone();
		}
	}

	@Override
	public byte[] getEncoded() {
		return (byte[]) key.clone();
	}

	@Override
	public String getAlgorithm() {
		return Crypto.ALGORITHM;
	}

	@Override
	public String getFormat() {
		return "RAW";
	}

	/**
	 * Calculates a hash code value for the object. Objects that are equal will
	 * also have the same hashcode.
	 */
	@Override
	public int hashCode() {
		int ret = 0;

		for (int xa = 1; xa < this.key.length; xa++) {
			ret += (this.key[xa] * xa);
		}
		ret ^= getAlgorithm().toUpperCase().hashCode();
		return ret;
	}

	@Override
	public boolean equals(Object obj) {
		if (obj == this) {
			return true;
		}
		if ((obj == null) || !obj.getClass().equals(getClass())) {
			return false;
		}

		BinaryPBEKey oth = (BinaryPBEKey) obj;

		if (!(oth.getAlgorithm().equalsIgnoreCase(getAlgorithm()))) {
			return false;
		}

		byte[] othkey = oth.getEncoded();
		boolean ret = Arrays.equals(key, othkey);
		Arrays.fill(othkey, (byte) 0);
		return ret;
	}

	@Override
	public void destroy() {
		Arrays.fill(this.key, (byte) 0);
	}

	/**
	 * Ensure that the password bytes of this key are zeroed out when there are no
	 * more references to it.
	 */
	@Override
	protected void finalize() throws Throwable {
		try {
			destroy();
		} finally {
			super.finalize();
		}
	}
}