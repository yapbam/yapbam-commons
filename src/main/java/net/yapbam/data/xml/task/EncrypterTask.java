package net.yapbam.data.xml.task;

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
import net.yapbam.util.StreamUtils;

/** Encoding task.
 * <br>This task encodes an input stream and output it to an output stream.
 */
public class EncrypterTask implements Callable<Void> {
	public static final String UTF8 = "UTF-8"; //$NON-NLS-1$

	private static final byte[] SALT = new byte[]{ (byte)0xc7, (byte)0x23, (byte)0xa5, (byte)0xfc, (byte)0x7e, (byte)0x38, (byte)0xee, (byte)0x09};
	static final String ALGORITHM = "PBEWITHMD5ANDDES"; //$NON-NLS-1$
	static final PBEParameterSpec PBE_PARAM_SPEC = new PBEParameterSpec(SALT, 16);

	private InputStream in;
	private OutputStream out;
	private boolean compatibilityMode;
	private String password;

	/** Constructor.
	 * @param in The input stream to encode
	 * @param out An output stream where to output the encoded stream
	 * @param password The password to use to encrypt the data
	 * @param compatibilityMode true to use the compatibility mode
	 */
	public EncrypterTask(InputStream in, OutputStream out, String password, boolean compatibilityMode) {
		this.in = in;
		this.out = out;
		this.password = password;
		this.compatibilityMode = compatibilityMode;
	}
	
	/** Creates a new cipher based on a password.
	 * @param mode The cipher mode (could be Cipher.ENCRYPT_MODE or Cipher.DECRYPT_MODE)
	 * @param compatibilityMode 
	 */
	static Cipher getCipher(int mode, String password, boolean compatibilityMode) throws GeneralSecurityException {
		SecretKey pbeKey = getSecretKey(password, compatibilityMode);
		Cipher cipher = Cipher.getInstance(ALGORITHM);
		cipher.init(mode, pbeKey, PBE_PARAM_SPEC);
		return cipher;
	}
	
	/** Gets the secret key corresponding to a password.
	 * @param password A password
	 * @param compatibilityMode true to use an old yapbam style key (which was not compatible with Android)
	 * @return a Secret key
	 * @throws InvalidKeySpecException
	 * @throws NoSuchAlgorithmException
	 */
	@SuppressWarnings("deprecation")
	static SecretKey getSecretKey(String password, boolean compatibilityMode) throws InvalidKeySpecException, NoSuchAlgorithmException {
		try {
			if (compatibilityMode) {
				return new net.yapbam.util.BinaryPBEKey(password.getBytes(UTF8));
			} else {
				password = Base64Encoder.encode(password.getBytes(UTF8));
				return SecretKeyFactory.getInstance(ALGORITHM).generateSecret(new PBEKeySpec(password.toCharArray()));
			}
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
		// output password digest
		out.write(getDigest(password));
		Cipher cipher = getCipher(Cipher.ENCRYPT_MODE, password, compatibilityMode);
		CipherOutputStream po = new CipherOutputStream(out, cipher);
		try {
			StreamUtils.copy(in, po, new byte[FilterTask.BUFFER_SIZE]);
			return null;
		} finally {
			in.close();
			po.close();
			out.flush();
			out.close();
		}
	}
}