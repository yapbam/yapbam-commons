package net.yapbam.data.xml.task;


import java.io.InputStream;
import java.io.OutputStream;
import java.util.zip.InflaterOutputStream;

/** Inflation task.
 * <br>This task inflates an input stream and output it to an output stream.
 */
public class InflaterTask extends FilterTask {

	/** Constructor.
	 * @param in An input stream
	 * @param out The output stream where to output the uncompressed data
	 */
	public InflaterTask(InputStream in, OutputStream out) {
		super (in, out);
	}

	@Override
	public OutputStream buildFilteredOutputStream(OutputStream out) {
		return new InflaterOutputStream(out);
	}
}