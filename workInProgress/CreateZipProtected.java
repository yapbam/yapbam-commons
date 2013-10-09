import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.zip.ZipOutputStream;

import net.yapbam.data.GlobalData;
import net.yapbam.data.xml.Serializer;
import net.yapbam.data.xml.task.DecrypterTask;
import net.yapbam.data.xml.task.EncrypterTask;


public class CreateZipProtected {

	/**
	 * @param args
	 */
	public static void main(String[] args) {
		try {
//			FileOutputStream out = new FileOutputStream("out.bin");
//			try {
//				InputStream in = new ByteArrayInputStream("Hello".getBytes("UTF-8"));
//				new EncrypterTask(in, out, "gti", false).call();
//			} finally {
//				out.close();
//			}
			
			InputStream in = new FileInputStream("out.bin");
			try {
				ByteArrayOutputStream out2 = new ByteArrayOutputStream();
				new DecrypterTask(in, out2, "gti", false).call();
				byte[] result2 = out2.toByteArray();
				String cycled = new String(result2, "UTF-8");
				System.out.println (cycled);
			} finally {
				in.close();
			}
		
		
		
		
		
//			GlobalData data = new GlobalData();
//			data.setPassword("gti");
//			{
//			OutputStream out = new FileOutputStream("test.bin");
//			try {
//				Serializer.write(data, (ZipOutputStream) out, "content.txt", null);
//			} finally {
//				out.close();
//			}
//			}
		} catch (Throwable e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

}
