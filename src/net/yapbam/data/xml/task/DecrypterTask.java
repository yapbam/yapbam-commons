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
			int nb = stream.read(fileDigest, fileDigest.length-missing, missing);
			if (nb==-1) {
				throw new IOException("end of stream reached before end of password digest");
			}
			missing -= nb;
		}
		if (!MessageDigest.isEqual(digest, fileDigest)) {
			throw new AccessControlException("invalid password");
		}
	}

	@Override
	public Void call() throws Exception {
		if (TRACE) {
			System.out.println ("Start "+getClass().getName());
		}
		try {
			verifyPassword(in, password);
			Cipher cipher = EncrypterTask.getCipher(Cipher.DECRYPT_MODE, password, compatibilityMode);
			this.out = new CipherOutputStream(out, cipher);
			byte[] buffer = new byte[FilterTask.BUFFER_SIZE];
			for (int bytesRead = in.read(buffer); bytesRead!=-1; bytesRead = in.read(buffer)) {
				out.write(buffer, 0, bytesRead);
			}
			out.flush();
			return null;
		} finally {
			in.close();
			out.close();
			if (TRACE) {
				System.out.println ("Stop "+getClass().getName());
			}
		}
	}
}