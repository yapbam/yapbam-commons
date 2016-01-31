package net.yapbam.data.xml.task;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.concurrent.Callable;

import net.yapbam.util.StreamUtils;

abstract class FilterTask implements Callable<Void> {
	static final int BUFFER_SIZE = 10240;

	protected InputStream in;
	protected OutputStream po;

	/** Construct a filter from an input stream to an output stream.
	 * @param in An input stream
	 * @param out An output stream
	 */
	public FilterTask(InputStream in, OutputStream out) {
		this.in = in;
		this.po = buildFilteredOutputStream(out);
	}
	
	public abstract OutputStream buildFilteredOutputStream(OutputStream out);

	@Override
	public Void call() throws IOException {
		try {
			StreamUtils.copy(in, po, new byte[BUFFER_SIZE]);
			return null;
		} finally {
			in.close();
			po.close();
		}
	}
}
