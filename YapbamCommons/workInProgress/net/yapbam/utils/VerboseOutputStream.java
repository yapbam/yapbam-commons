package net.yapbam.utils;

import java.io.IOException;
import java.io.OutputStream;

public class VerboseOutputStream extends OutputStream {
	private boolean firstOfLine;
	private OutputStream out;
	
	public VerboseOutputStream(OutputStream stream) {
		this.out = stream;
		this.firstOfLine = true;
	}

	@Override
	public void write(int b) throws IOException {
		if (!firstOfLine) System.out.print (' ');
		System.out.print(String.format("%02X", (byte)b));
		firstOfLine = false;
		this.out.write(b);
	}

	/* (non-Javadoc)
	 * @see java.io.OutputStream#close()
	 */
	@Override
	public void close() throws IOException {
    try {
      flush();
    } catch (IOException ignored) {
    }
    out.close();
    if (!firstOfLine) System.out.println();
    System.out.println(">>> closed");
    firstOfLine = true;
	}

	/* (non-Javadoc)
	 * @see java.io.OutputStream#flush()
	 */
	@Override
	public void flush() throws IOException {
    if (!firstOfLine) System.out.println();
    System.out.println("> flush is called");
    firstOfLine = true;
		out.flush();
	}
}
