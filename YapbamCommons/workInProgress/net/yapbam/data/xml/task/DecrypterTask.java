package net.yapbam.data.xml.task;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.security.AccessControlException;
import java.security.MessageDigest;
import java.util.concurrent.Callable;

import javax.crypto.Cipher;
import javax.crypto.CipherOutputStream;

/** Decryption task.
 * <br>This task decrypts an input stream and output it to an output stream.
 */
public class DecrypterTask implements Callable<Void> {
	private static boolean TRACE = false;

	private InputStream in;
	private OutputStream out;
	private Cipher cipher;
	private boolean compatibilityMode;

	private String password;


	/** Compress input stream and output it to an output stream.
	 * @param in An input stream
	 * @param out An output stream
	 * @param compatibilityMode 
	 */
	public DecrypterTask(InputStream in, OutputStream out, String password, boolean compatibilityMode) {
		this.in = in;
		this.out = out;
		this.password = password;
		this.compatibilityMode = compatibilityMode;
	}

	public static void verifyPassword(InputStream stream, String password) throws IOException, AccessControlException {
		byte[] digest = EncrypterTask.getDigest(password);
		byte[] fileDigest = new byte[digest.length];
		for (int missing=fileDigest.length; missing>0; ) {
			missing -= stream.read(fileDigest, fileDigest.length-missing, missing);
		}
		if (!MessageDigest.isEqual(digest, fileDigest)) throw new AccessControlException("invalid password");
	}

	@Override
	public Void call() throws Exception {
		try {
			if (TRACE) System.out.println ("Start "+getClass().getName());
			cipher = EncrypterTask.getCipher(Cipher.DECRYPT_MODE, password, compatibilityMode);
			this.out = new CipherOutputStream(out, cipher);
			verifyPassword(in, password);
			byte[] buffer = new byte[512];
			int bytes_read;
			for (;;) {
				bytes_read = in.read(buffer);
				if (bytes_read == -1) break;
				out.write(buffer, 0, bytes_read);
			}
			return null;
		} finally {
			in.close();
			out.close();
			if (TRACE) System.out.println ("Stop "+getClass().getName());
		}
	}
}