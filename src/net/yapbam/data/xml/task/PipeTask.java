package net.yapbam.data.xml.task;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.concurrent.Callable;

/** A task that reads the content of an input stream and outputs it to an output stream.
 */
public class PipeTask implements Callable<Void> {
	private static boolean TRACE = false;
	private InputStream in;
	private OutputStream out;

	/** Constructor. 
	 * @param in The input stream (will be closed by the task)
	 * @param out The output stream (will remain opened)
	 */
	public PipeTask (InputStream in, OutputStream out) {
		this.in = in;
		this.out = out;
	}

	@Override
	public Void call() throws Exception {
		if (TRACE) System.out.println ("Start "+getClass().getName());
		try {
			byte[] buffer = new byte[FilterTask.BUFFER_SIZE];
			for (;;) {
				int bytes_read = in.read(buffer);
				if (bytes_read == -1) break;
				out.write(buffer, 0, bytes_read);
			}
			return null;
		} finally {
			in.close();
			if (TRACE) System.out.println ("Stop "+getClass().getName());
		}
	}
}
