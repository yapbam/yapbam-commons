package net.yapbam.data.xml.task;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.security.AccessControlException;
import java.security.GeneralSecurityException;
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
	private CipherOutputStream po;
	private Cipher cipher;

	private String password;

	/** Compress input stream and output it to an output stream.
	 * @param in An input stream
	 * @param out An output stream
	 */
	public DecrypterTask(InputStream in, OutputStream out, String password) throws GeneralSecurityException, IOException {
		this.in = in;
		this.password = password;
		cipher = EncrypterTask.getCipher(Cipher.DECRYPT_MODE, password);
		this.po = new CipherOutputStream(out, cipher);
	}

	private static void verifyPassword(InputStream stream, String password) throws IOException, AccessControlException {
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
			verifyPassword(in, password);
			byte[] buffer = new byte[512];
			int bytes_read;
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