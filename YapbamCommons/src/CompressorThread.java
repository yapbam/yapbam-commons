

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.zip.DeflaterOutputStream;

public class CompressorThread  extends Thread implements Runnable {

	InputStream in;
	DeflaterOutputStream po;

	/** Compress input stream and output it to an output stream.
	 * @param in An input stream
	 * @param out An output stream
	 */
	CompressorThread(InputStream in, OutputStream out) {
		this.in = in;
		this.po = new DeflaterOutputStream(out);
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
			po.flush();
			po.finish();
			po.close();
		} catch (IOException e) {
			throw new RuntimeException(e);
		} finally {
			System.out.println ("CompressorThread ends");
		}
	}

}