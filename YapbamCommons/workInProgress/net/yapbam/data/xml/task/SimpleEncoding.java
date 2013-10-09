package net.yapbam.data.xml.task;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.security.GeneralSecurityException;

import javax.crypto.Cipher;
import javax.crypto.CipherOutputStream;
import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.PBEKeySpec;
import javax.crypto.spec.PBEParameterSpec;

public class SimpleEncoding {

	private static final byte[] SALT = new byte[]{ (byte)0xc7, (byte)0x23, (byte)0xa5, (byte)0xfc, (byte)0x7e, (byte)0x38, (byte)0xee, (byte)0x09};
	private static final String ALGORITHM = "PBEWITHMD5ANDDES"; //$NON-NLS-1$
	private static final PBEParameterSpec pbeParamSpec = new PBEParameterSpec(SALT, 16);

	/**
	 * @param args
	 */
	public static void main(String[] args) {
		String pwd = "xxx";
		String text = "Hello";
		File file = new File("out_digest.bin");
		try {
			InputStream in = new ByteArrayInputStream(text.getBytes("UTF-8"));
			try {
				encryptToFile(pwd, file, in);
				ByteArrayOutputStream out = new ByteArrayOutputStream();
				decryptFromFile(pwd, file, out);
				System.out.println ("From file: "+new String(out.toByteArray(), "UTF-8"));
			} finally {
				in.close();
			}
		} catch (Throwable e) {
			e.printStackTrace();
		}
	}

	public static void decryptFromFile(String pwd, File file, OutputStream out) throws GeneralSecurityException, IOException {
		InputStream in = new FileInputStream(file);
		try {
			DecrypterTask.verifyPassword(in, pwd);
			Cipher cipher = Cipher.getInstance(ALGORITHM);
			cipher.init(Cipher.DECRYPT_MODE, SecretKeyFactory.getInstance(ALGORITHM).generateSecret(new PBEKeySpec(pwd.toCharArray())), pbeParamSpec);
			CipherOutputStream cout = new CipherOutputStream(out, cipher);
			try {
				byte[] buffer = new byte[1024];
				for (int bytes_read = in.read(buffer); bytes_read!=-1; bytes_read = in.read(buffer)) {
					cout.write(buffer, 0, bytes_read);
				}
				cout.flush();
			} finally {
				cout.close();
			}
		} finally {
			in.close();
		}
	}

	public static void encryptToFile(String pwd, File file, InputStream in) throws GeneralSecurityException, IOException {
		FileOutputStream out = new FileOutputStream(file);
		try {
			out.write(EncrypterTask.getDigest(pwd));
			Cipher cipher = Cipher.getInstance(ALGORITHM);
			cipher.init(Cipher.ENCRYPT_MODE, SecretKeyFactory.getInstance(ALGORITHM).generateSecret(new PBEKeySpec(pwd.toCharArray())), pbeParamSpec);
			CipherOutputStream cout = new CipherOutputStream(out, cipher);
			byte[] buffer = new byte[1024];
			try {
				for (;;) {
					int bytes_read = in.read(buffer);
					if (bytes_read == -1) break;
					cout.write(buffer, 0, bytes_read);
				}
			} finally {
				cout.close();
			}
		} finally {
			out.close();
		}
	}
}
