package net.yapbam.data.xml;

import java.io.*;
import java.security.AccessControlException;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;
import java.util.zip.ZipOutputStream;

import net.yapbam.data.*;

/** The class implements xml yapbam data serialization and deserialization to (or from) an URL.
 * Currently supported URL type are :<UL>
 * <LI> file.
 * </UL>
 */
public class Serializer extends AbstractSerializer<GlobalData> {
	private static final byte[] MAGIC_ZIP_BYTES = new byte[]{0x50, 0x4B, 0x03, 0x04};
	
	/** Saves the data to a stream.
	 * @param data The data to save
	 * @param out The outputStream (Note that this stream is not closed by this method).
	 * @param entryName the name of the zip entry where to put the content.
	 * @param report a progress report
	 * @throws IOException if something goes wrong while writing
	 */
	public void writeToZip(GlobalData data, ZipOutputStream out, String entryName, ProgressReport report) throws IOException {
		ZipEntry entry = new ZipEntry(entryName);
		out.putNextEntry(entry);
		write(data, out, report);
		out.closeEntry();
	}

	public void write(GlobalData data, OutputStream out, ProgressReport report) throws IOException {
		super.write(data, out, data.getPassword(), report);
	}

	public void directWrite(GlobalData data, OutputStream out, ProgressReport report) throws IOException {
		XMLSerializer xmlSerializer = new XMLSerializer(out);
		xmlSerializer.serialize((GlobalData) data, report);
		xmlSerializer.closeDocument();
	}
	
	/** Tests whether an input stream contains a zip file.
	 * @param in The stream to test.<br>Please note that this stream should support mark and reset.
	 * @return true if the stream contains a zipped file.<br>The stream position is unchanged.
	 * @throws IOException
	 */
 	private static boolean isZippedInputStream(InputStream in) throws IOException {
		in.mark(MAGIC_ZIP_BYTES.length);
		boolean isZipped = true;
		for (int i = 0; i < MAGIC_ZIP_BYTES.length-1; i++) {
			if (in.read()!=MAGIC_ZIP_BYTES[i]) {
				isZipped = false;
				break;
			}
		}
		in.reset();
		return isZipped;
	}

	/** Reads global data.
	 * @param password The password of the data (null if the data is not password protected)
	 * @param in The input stream containing the data (if the stream is a stream on a zipped file, for instance created by write(GlobalData, ZipOutputStream, String, ProgressReport)
	 * the data is automatically unzipped from the first entry).
	 * @param report A progress report to observe the progress, or null
	 * @return The data red.
	 * @throws IOException If something goes wrong while reading
	 * @throws AccessControlException If the password is wrong. Note that if data is not password protected, password argument is ignored
	 * @throws UnsupportedFormatException If the format of data in the input stream is not supported
	 */
	public GlobalData read(String password, InputStream in, ProgressReport report) throws IOException, AccessControlException {
		// Verify if the stream is encrypted or not
		if (!in.markSupported()) {
			// Ensure that we will be able to reset the stream after verifying that the stream is not encrypted
			in = new BufferedInputStream(in);
		}
		boolean isZipped = isZippedInputStream(in);
		if (isZipped) {
			in = new ZipInputStream(in);
			((ZipInputStream)in).getNextEntry();
			if (!in.markSupported()) {
				in = new BufferedInputStream(in);
			}
		}
		return super.read(password, in, report);
	}
	
	public GlobalData directRead(String password, InputStream in, ProgressReport report) throws IOException {
		GlobalData result = XMLSerializer.read(in, report);
		result.setPassword(password);
		return result;
	}
	
	public boolean isPasswordOk(InputStream in, String password) throws IOException {
		boolean isZipped = isZippedInputStream(in);
		if (isZipped) {
			in = new ZipInputStream(in);
			((ZipInputStream)in).getNextEntry();
			if (!in.markSupported()) {
				in = new BufferedInputStream(in);
			}
		}

		return super.isPasswordOk(in, password);
	}

}
