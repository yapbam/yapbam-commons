package net.yapbam.data.xml.task;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.OutputStream;

public class StdEncrypterEncoding {

	/**
	 * @param args
	 */
	public static void main(String[] args) {
		String pwd = "xxx";
		String text = "Hello";
		File file = new File("encrypter.bin");
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

	public static void decryptFromFile(String pwd, File file, OutputStream out) throws Exception {
		InputStream in = new FileInputStream(file);
		try {
			new DecrypterTask(in, out, pwd, false).call();
		} finally {
			in.close();
		}
	}

	public static void encryptToFile(String pwd, File file, InputStream in) throws Exception {
		FileOutputStream out = new FileOutputStream(file);
		try {
			new EncrypterTask(in, out, pwd, false).call();
		} finally {
			out.close();
		}
	}
}
