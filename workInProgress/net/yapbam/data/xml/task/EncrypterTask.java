package net.yapbam.data.xml.task;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.UnsupportedEncodingException;
import java.security.GeneralSecurityException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.spec.InvalidKeySpecException;
import java.util.concurrent.Callable;

import javax.crypto.Cipher;
import javax.crypto.CipherOutputStream;
import javax.crypto.SecretKey;
import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.PBEKeySpec;
import javax.crypto.spec.PBEParameterSpec;

import net.yapbam.util.Base64Encoder;

/** Encoding task.
 * <br>This task encodes an input stream and output it to an output stream.
 */
public class EncrypterTask implements Callable<Void> {
	private static boolean TRACE = false;
	public static final String UTF8 = "UTF-8"; //$NON-NLS-1$

	private static final byte[] SALT = new byte[]{ (byte)0xc7, (byte)0x23, (byte)0xa5, (byte)0xfc, (byte)0x7e, (byte)0x38, (byte)0xee, (byte)0x09};
	static final String ALGORITHM = "PBEWITHMD5ANDDES"; //$NON-NLS-1$
	static final PBEParameterSpec pbeParamSpec = new PBEParameterSpec(SALT, 16);

	private InputStream in;
	private CipherOutputStream po;
	private Cipher cipher;

	/** Constructor.
	 * @param in The input stream to encode
	 * @param out An output stream where to output the encoded stream
	 */
	public EncrypterTask(InputStream in, OutputStream out, String password) throws GeneralSecurityException, IOException {
		this.in = in;
		out.write(getDigest(password));
		cipher = getCipher(Cipher.ENCRYPT_MODE, password);
		this.po = new CipherOutputStream(out, cipher);
	}
	
	/** Creates a new cipher based on a password.
	 * @param mode The cipher mode (could be Cipher.ENCRYPT_MODE or Cipher.DECRYPT_MODE)
	 */
	static Cipher getCipher(int mode, String password) throws GeneralSecurityException {
		SecretKey pbeKey = getSecretKey(password);
		Cipher cipher = Cipher.getInstance(ALGORITHM);
		cipher.init(mode, pbeKey, pbeParamSpec);
		return cipher;
	}
	
	/** Gets the secret key cooresponding to a password.
	 * @param password A password
	 * @return a Secret key
	 * @throws InvalidKeySpecException
	 * @throws NoSuchAlgorithmException
	 */
	static SecretKey getSecretKey(String password) throws InvalidKeySpecException, NoSuchAlgorithmException {
		try {
			password = Base64Encoder.encode(password.getBytes(UTF8));
			return SecretKeyFactory.getInstance(ALGORITHM).generateSecret(new PBEKeySpec(password.toCharArray()));
		} catch (UnsupportedEncodingException e) {
			throw new RuntimeException(e);
		} 
	}
	
	/** Gets the SHA digest of a password.
	 * @param password The password
	 * @return The password digest.
	 */
	static byte[] getDigest(String password) {
		try {
			MessageDigest digest = MessageDigest.getInstance("SHA");
			digest.update(SALT);
			return digest.digest(password.getBytes(UTF8));
		} catch (NoSuchAlgorithmException e) {
			throw new RuntimeException(e);
		} catch (UnsupportedEncodingException e) {
			throw new RuntimeException(e);
		}
	}

	@Override
	public Void call() throws Exception {
		if (TRACE) System.out.println ("Start "+getClass().getName());
		byte[] buffer = new byte[512];
		int bytes_read;
		try {
			for (;;) {
				bytes_read = in.read(buffer);
				if (bytes_read == -1) break;
				po.write(buffer, 0, bytes_read);
			}
			return null;
		} finally {
			in.close();
			po.close();
			if (TRACE) System.out.println ("Stop "+getClass().getName());
		}
	}
}