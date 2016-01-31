package net.yapbam.remote;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

/** A fake cache class that stores data into memory.
 * @author Jean-Marc Astesana
 * <BR>License : GPL v3
 */
public class MemoryCache implements Cache {
	private ByteArrayOutputStream writer;
	private byte[] byteArrays;
	private long timeStamp;
	private long tmpTimeStamp;

	public MemoryCache() {
		this.timeStamp = -1;
		this.tmpTimeStamp = -1;
	}
	
	@Override
	public OutputStream getOutputStream() throws IOException {
		this.writer = new ByteArrayOutputStream();
		this.tmpTimeStamp = System.currentTimeMillis();
		return writer;
	}

	@Override
	public InputStream getInputStream(boolean tmp) throws IOException {
		if (tmp) {
			if (writer==null) {
				throw new FileNotFoundException();
			} else {
				return new ByteArrayInputStream(writer.toByteArray());
			}
		} else {
			if (byteArrays==null) {
				throw new FileNotFoundException();
			} else {
				return new ByteArrayInputStream(byteArrays);
			}
		}
	}

	@Override
	public void commit() {
		this.byteArrays = writer.toByteArray();
		this.timeStamp = this.tmpTimeStamp;
	}

	@Override
	public boolean isEmpty() {
		return (byteArrays == null) && (writer==null);
	}

	@Override
	public long getTimeStamp() {
		return this.timeStamp;
	}
}
