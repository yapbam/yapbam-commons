

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.UnsupportedEncodingException;
import java.security.GeneralSecurityException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.spec.InvalidKeySpecException;

import javax.crypto.Cipher;
import javax.crypto.CipherOutputStream;
import javax.crypto.SecretKey;
import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.PBEKeySpec;
import javax.crypto.spec.PBEParameterSpec;

import net.yapbam.util.Base64Encoder;

public class EncoderThread extends Thread implements Runnable {
	public static final String UTF8 = "UTF-8"; //$NON-NLS-1$
	static final String ALGORITHM = "PBEWITHMD5ANDDES"; //$NON-NLS-1$
	private static final byte[] SALT = new byte[]{ (byte)0xc7, (byte)0x23, (byte)0xa5, (byte)0xfc, (byte)0x7e, (byte)0x38, (byte)0xee, (byte)0x09};
	private static final PBEParameterSpec pbeParamSpec = new PBEParameterSpec(SALT, 16);

	InputStream in;
	CipherOutputStream po;
	Cipher cipher;

	/** Compress input stream and output it to an output stream.
	 * @param in An input stream
	 * @param out An output stream
	 */
	EncoderThread(InputStream in, OutputStream out, String password) throws GeneralSecurityException, IOException {
		this.in = in;
		out.write(getDigest(password));
		SecretKey pbeKey = getSecretKey(password);
		cipher = Cipher.getInstance(ALGORITHM);
		cipher.init(Cipher.ENCRYPT_MODE, pbeKey, pbeParamSpec);
		this.po = new CipherOutputStream(out, cipher);
	}
	
	private static SecretKey getSecretKey(String password) throws InvalidKeySpecException, NoSuchAlgorithmException {
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
	private static byte[] getDigest(String password) {
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

	public void run() {
		byte[] buffer = new byte[512];
		int bytes_read;
		try {
			for (;;) {
				bytes_read = in.read(buffer);
				if (bytes_read == -1) break;
				po.write(buffer, 0, bytes_read);
			}
			in.close();
			po.close();
		} catch (Exception e) {
			throw new RuntimeException(e);
		} finally {
			System.out.println ("EncoderThread ends");
		}
	}

}