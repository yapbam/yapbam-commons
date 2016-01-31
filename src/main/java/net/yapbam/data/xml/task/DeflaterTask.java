package net.yapbam.data.xml.task;

import java.io.InputStream;
import java.io.OutputStream;
import java.util.zip.DeflaterOutputStream;

/** Deflation task.
 * <br>This task compresses an input stream and output it to an output stream.
 */
public class DeflaterTask extends FilterTask {

	/** Constructor.
	 * @param in An input stream
	 * @param out The output stream where to output the compressed data
	 */
	public DeflaterTask(InputStream in, OutputStream out) {
		super (in, out);
	}

	@Override
	public OutputStream buildFilteredOutputStream(OutputStream out) {
		return new DeflaterOutputStream(out);
	}
}