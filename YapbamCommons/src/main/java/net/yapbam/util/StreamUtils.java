package net.yapbam.util;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

public abstract class StreamUtils {
	private StreamUtils() {
		// To prevent this class from being instantiated
	}

	public static void copy(InputStream in, OutputStream out, byte[] buffer) throws IOException {
		for (;;) {
			int bytesRead = in.read(buffer);
			if (bytesRead == -1) {
				break;
			}
			out.write(buffer, 0, bytesRead);
		}
		out.flush();
	}
}
