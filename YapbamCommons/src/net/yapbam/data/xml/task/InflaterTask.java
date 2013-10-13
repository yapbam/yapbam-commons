package net.yapbam.data.xml.task;


import java.io.InputStream;
import java.io.OutputStream;
import java.util.concurrent.Callable;
import java.util.zip.InflaterOutputStream;

/** Inflation task.
 * <br>This task inflates an input stream and output it to an output stream.
 */
public class InflaterTask implements Callable<Void> {
	private static boolean TRACE = false;

	private InputStream in;
	private InflaterOutputStream po;

	/** Compress input stream and output it to an output stream.
	 * @param in An input stream
	 * @param out An output stream
	 */
	public InflaterTask(InputStream in, OutputStream out) {
		this.in = in;
		this.po = new InflaterOutputStream(out);
	}

	@Override
	public Void call() throws Exception {
		if (TRACE) System.out.println ("Start "+getClass().getName());
		byte[] buffer = new byte[PipeTask.BUFFER_SIZE];
		int bytes_read;
		try {
			for (;;) {
				bytes_read = in.read(buffer);
				if (bytes_read == -1) break;
				po.write(buffer, 0, bytes_read);
			}
			return null;
		} finally {
			in.close();
			po.flush();
			po.finish();
			po.close();
			if (TRACE) System.out.println ("Stop "+getClass().getName());
		}
	}

}