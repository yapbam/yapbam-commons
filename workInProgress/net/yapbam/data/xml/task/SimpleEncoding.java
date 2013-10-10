package net.yapbam.data.xml.task;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.UnsupportedEncodingException;
import java.security.GeneralSecurityException;
import java.util.Arrays;

import javax.crypto.Cipher;
import javax.crypto.CipherOutputStream;
import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.PBEKeySpec;
import javax.crypto.spec.PBEParameterSpec;

import net.yapbam.util.Base64Encoder;

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
			cipher.init(Cipher.DECRYPT_MODE, SecretKeyFactory.getInstance(ALGORITHM).generateSecret(getKeySpec(pwd)), pbeParamSpec);
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
			cipher.init(Cipher.ENCRYPT_MODE, SecretKeyFactory.getInstance(ALGORITHM).generateSecret(getKeySpec(pwd)), pbeParamSpec);
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

	private static PBEKeySpec getKeySpec(String pwd) throws UnsupportedEncodingException {
		System.out.println (pwd);
		byte[] bytes = pwd.getBytes("UTF-8");
		System.out.println (Arrays.toString(bytes));
		if (!"[120, 120, 120]".equals(Arrays.toString(bytes))) throw new IllegalArgumentException("getBytes is wrong: "+Arrays.toString(bytes));
		pwd = Base64Encoder.encode(bytes);
		System.out.println (pwd+" ("+Arrays.toString(pwd.getBytes("UTF-8"))+")");
		if (!pwd.equals("eHh4")) throw new IllegalArgumentException("base64 is wrong: "+pwd+" ("+Arrays.toString(pwd.getBytes("UTF-8"))+")");
		return new PBEKeySpec(pwd.toCharArray());
	}
}
