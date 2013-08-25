package net.yapbam.utils;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.UnsupportedEncodingException;
import java.security.AccessControlException;
import java.security.GeneralSecurityException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.spec.InvalidKeySpecException;
import java.util.zip.DeflaterOutputStream;
import java.util.zip.InflaterInputStream;

import javax.crypto.Cipher;
import javax.crypto.CipherInputStream;
import javax.crypto.CipherOutputStream;
import javax.crypto.SecretKey;
import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.PBEKeySpec;
import javax.crypto.spec.PBEParameterSpec;

import net.yapbam.util.Base64Encoder;
import net.yapbam.util.Log;

public class Crypto2 {
	public static final String UTF8 = "UTF-8"; //$NON-NLS-1$
	static final String ALGORITHM = "PBEWITHMD5ANDDES"; //$NON-NLS-1$
	private static final byte[] SALT = new byte[]{ (byte)0xc7, (byte)0x23, (byte)0xa5, (byte)0xfc, (byte)0x7e, (byte)0x38, (byte)0xee, (byte)0x09};
	private static final PBEParameterSpec pbeParamSpec = new PBEParameterSpec(SALT, 16);

	private boolean useCompression;
	
	public Crypto2(boolean useCompression) {
		this.useCompression = useCompression;
	}
	
	public String doFileCycle(String message, String password) {
		try {
			File file = new File("toto.tmp");
			try {
				FileOutputStream out = new FileOutputStream("toto.tmp");
				try {
					OutputStream encoder = getPasswordProtectedOutputStream(password, out);
					encoder.write(message.getBytes("UTF8"));
					encoder.flush();
					encoder.close();
				} finally {
					out.close();
				}
				
				FileInputStream in = new FileInputStream(file);
				try {
					InputStream decoder = getPasswordProtectedInputStream(password, in);
					try {
						ByteArrayOutputStream out2 = new ByteArrayOutputStream();
						int nRead;
						byte[] data = new byte[16384];
						while ((nRead = decoder.read(data, 0, data.length)) != -1) {
						  out2.write(data, 0, nRead);
						}
						out.flush();
						return new String(out2.toByteArray(), "UTF8");
					} finally {
						decoder.close();
					}
				} finally {
					in.close();
				}
			} finally {
				file.delete();
			}
		} catch (Throwable e) {
			throw new RuntimeException(e);
		}
	}

	public String doCycle(String message, String password) {
		try {
			ByteArrayOutputStream out = new ByteArrayOutputStream();
			OutputStream encoder = getPasswordProtectedOutputStream(password, out);
			encoder.write(message.getBytes("UTF8"));
			encoder.close();
			out.flush();
			byte[] encodedBytes = out.toByteArray();
			
			log(toString(encodedBytes));
			
			InputStream decoder = getPasswordProtectedInputStream(password, new ByteArrayInputStream(encodedBytes));
			out.reset();
			int nRead;
			byte[] data = new byte[16384];
			while ((nRead = decoder.read(data, 0, data.length)) != -1) {
			  out.write(data, 0, nRead);
			}
			out.flush();
			return new String(out.toByteArray(), "UTF8");
		} catch (Throwable e) {
			throw new RuntimeException(e);
		}
	}
	
	public void log(String message) {
		Log.v(this, message);
		System.out.println (message);
	}
	
	/** Converts a byte array to an hex String.
	 * @param bytes The array of bytes
	 * @return a String
	 */
	private static String toString(byte[] bytes) {
		StringBuilder builder = new StringBuilder();
		for (byte b : bytes) {
			if (builder.length()>0) builder.append(' ');
			builder.append(String.format("%02X", b));
		}
		return builder.toString();
	}
	
	/** Encrypt an output stream.
	 * <br>The returned stream is deflated and encrypted with the password accordingly to the PBEWithMD5AndDES algorithm.
	 * @param password The password used to encrypt the stream.
	 * @param stream The stream to encrypt.
	 * @return A new stream that encrypt data written to it.
	 * @see #getPasswordProtectedInputStream(String, InputStream)
	 * @throws IOException
	 */
	public OutputStream getPasswordProtectedOutputStream (String password, OutputStream stream) throws IOException, GeneralSecurityException {
		stream.write(getDigest(password));
		SecretKey pbeKey = getSecretKey(password);
		Cipher cipher = Cipher.getInstance(ALGORITHM);
		cipher.init(Cipher.ENCRYPT_MODE, pbeKey, pbeParamSpec);
		stream = new CipherOutputStream(stream, cipher);
		if (useCompression) stream = new DeflaterOutputStream(stream);
		return stream;
	}

	/** Decrypt an input stream.
	 * <br>The data in the stream may have been encoded using a stream return by getPasswordProtectedOutputStream.
	 * @param password The password used to encrypt the data.
	 * @param stream The stream on the encrypted data
	 * @return a new InputStream, data read from this stream is decrypted.
	 * @see #getPasswordProtectedOutputStream(String, OutputStream)
	 * @throws IOException
	 * @throws GeneralSecurityException
	 */
	public InputStream getPasswordProtectedInputStream (String password, InputStream stream) throws IOException, AccessControlException, GeneralSecurityException {
		verifyPassword(stream, password);
		Cipher cipher = Cipher.getInstance(ALGORITHM);
		cipher.init(Cipher.DECRYPT_MODE, getSecretKey(password), pbeParamSpec);
		stream = new CipherInputStream(stream, cipher);
		if (useCompression) stream = new InflaterInputStream(stream);
		return stream;
	}
	
	private static SecretKey getSecretKey(String password) throws InvalidKeySpecException, NoSuchAlgorithmException {
		try {
			password = Base64Encoder.encode(password.getBytes(UTF8));
			return SecretKeyFactory.getInstance(ALGORITHM).generateSecret(new PBEKeySpec(password.toCharArray()));
		} catch (UnsupportedEncodingException e) {
			throw new RuntimeException(e);
		} 
	}
	
	private static void verifyPassword(InputStream stream, String password) throws IOException, AccessControlException {
		byte[] digest = getDigest(password);
		byte[] fileDigest = new byte[digest.length];
		for (int missing=fileDigest.length; missing>0; ) {
			missing -= stream.read(fileDigest, fileDigest.length-missing, missing);
		}
		if (!MessageDigest.isEqual(digest, fileDigest)) throw new AccessControlException("invalid password");
	}

	/** Gets the SHA digest of a password.
	 * @param password The password
	 * @return The password digest.
	 */
	public static byte[] getDigest(String password) {
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
}
