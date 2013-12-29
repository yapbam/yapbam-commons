package net.yapbam.data.xml.task;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.concurrent.Callable;

import net.yapbam.util.StreamUtils;

abstract class FilterTask implements Callable<Void> {
	static final int BUFFER_SIZE = 1024;
	private static boolean TRACE = false;

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
		if (TRACE) {
			System.out.println ("Start "+getClass().getName());
		}
		byte[] buffer = new byte[BUFFER_SIZE];
		try {
			StreamUtils.copy(in, po, buffer);
			return null;
		} finally {
			in.close();
			po.close();
			if (TRACE) {
				System.out.println ("Stop "+getClass().getName());
			}
		}
	}
}
