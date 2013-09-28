

import java.io.InputStream;
import java.io.OutputStream;
import java.util.concurrent.Callable;
import java.util.zip.DeflaterOutputStream;

public class CompressorCallable implements Callable<Void> {
	private static boolean TRACE = false;

	private InputStream in;
	private DeflaterOutputStream po;

	/** Compress input stream and output it to an output stream.
	 * @param in An input stream
	 * @param out An output stream
	 */
	CompressorCallable(InputStream in, OutputStream out) {
		this.in = in;
		this.po = new DeflaterOutputStream(out);
	}

	@Override
	public Void call() throws Exception {
		if (TRACE) System.out.println ("Start "+getClass().getName());
		byte[] buffer = new byte[512];
		int bytes_read;
		try {
			for (;;) {
				bytes_read = in.read(buffer);
				if (bytes_read == -1) break;
				po.write(buffer, 0, bytes_read);
			}
		} finally {
			in.close();
			po.flush();
			po.finish();
			po.close();
			if (TRACE) System.out.println ("Stop "+getClass().getName());
		}
		return null;
	}

}