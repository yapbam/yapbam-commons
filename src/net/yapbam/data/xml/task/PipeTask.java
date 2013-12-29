package net.yapbam.data.xml.task;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.concurrent.Callable;

import net.yapbam.util.StreamUtils;

/** A task that reads the content of an input stream and outputs it to an output stream.
 */
public class PipeTask implements Callable<Void> {
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
	public Void call() throws IOException {
		try {
			byte[] buffer = new byte[FilterTask.BUFFER_SIZE];
			StreamUtils.copy(in, out, buffer);
			return null;
		} finally {
			in.close();
		}
	}
}
