package net.yapbam.data.xml;

import java.io.*;
import java.security.AccessControlException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;
import java.util.concurrent.SynchronousQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;
import java.util.zip.ZipOutputStream;

import net.yapbam.data.*;
import net.yapbam.data.xml.task.DecrypterTask;
import net.yapbam.data.xml.task.DeflaterTask;
import net.yapbam.data.xml.task.EncrypterTask;
import net.yapbam.data.xml.task.InflaterTask;
import net.yapbam.data.xml.task.PipeTask;
import net.yapbam.data.xml.task.ReaderTask;
import net.yapbam.data.xml.task.WriterTask;
import net.yapbam.util.Crypto;

/** The class implements xml yapbam data serialization and deserialization to (or from) an URL.
 * Currently supported URL type are :<UL>
 * <LI> file.
 * </UL>
 */
public class Serializer {
	private static final boolean NEW_ENCODER_ON = true;

	/** The password encoded file header scheme.
	 * the * characters means "the ending version is coded there".
	 */
	private static final byte[] PASSWORD_ENCODED_FILE_HEADER = toBytes("<Yapbam password encoded file ***>"); //$NON-NLS-1$
	private static final String V1 = "1.0"; //$NON-NLS-1$
	private static final String V2 = "2.0"; //$NON-NLS-1$
	private static final byte[] MAGIC_ZIP_BYTES = new byte[]{0x50, 0x4B, 0x03, 0x04};
	
	static {
		// A lot of code relies on the fact that V1 and V2 have the same length and that this length is the same as the number
		// of * in PASSWORD_ENCODED_FILE_HEADER
		// This code verifies it is always true
		int nb = 0;
		for (byte c : PASSWORD_ENCODED_FILE_HEADER) {
			if (c=='*') nb++;
		}
		try {
			if ((V1.getBytes(Crypto.UTF8).length!=nb) || (V2.getBytes(Crypto.UTF8).length!=nb) || (nb==0)) {
				throw new IllegalArgumentException("Encoded file headers versions have invalid lengths !"); //$NON-NLS-1$
			}
		} catch (UnsupportedEncodingException e) {
			throw new RuntimeException(e);
		}
	}

	private static byte[] toBytes(String magicString) {
		byte[] bytes;
		try {
			bytes = magicString.getBytes(Crypto.UTF8);
		} catch (UnsupportedEncodingException e) {
			bytes = null;
		}
		return bytes;
	}

	/** Saves the data to a stream.
	 * @param data The data to save
	 * @param out The outputStream
	 * @param entryName If you want the data to be wrapped in a zip: the name of the zip entry. Pass null to output the raw uncompressed data.
	 * @param report a progress report
	 * @throws IOException if something goes wrong while writing
	 */
	public static void write(GlobalData data, OutputStream out, String entryName, ProgressReport report) throws IOException {
		ZipEntry entry = null;
		if (entryName!=null) {
			out = new ZipOutputStream(out);
      entry = new ZipEntry(entryName);
			((ZipOutputStream)out).putNextEntry(entry);
		}
		String password = data.getPassword();
		if (password!=null) {
			// If the file has to be protected by a password
			if (NEW_ENCODER_ON) {
				// outputs the magic bytes that will allow Yapbam to recognize the file is crypted.
				out.write(getHeader(V2));
			} else {
				// outputs the magic bytes that will allow Yapbam to recognize the file is crypted.
				out.write(getHeader(V1));
			}
			PipedOutputStream xmlOutput = new PipedOutputStream();
			PipedInputStream compressorInput = new PipedInputStream(xmlOutput);
			
			PipedOutputStream compressorOutput = new PipedOutputStream();
			PipedInputStream encoderInput = new PipedInputStream(compressorOutput);
			
			ExecutorService service = new ThreadPoolExecutor(0, Integer.MAX_VALUE,0, TimeUnit.SECONDS,
          new SynchronousQueue<Runnable>());;

      List<Future<? extends Object>> futures = new ArrayList<Future<? extends Object>>(3);
      futures.add(service.submit(new WriterTask(data, xmlOutput)));
      futures.add(service.submit(new DeflaterTask(compressorInput, compressorOutput)));
      
      // As encryterTask closes its output stream (required to process the doFinal of the encryption cipher),
      // We can't pass it directly the out stream if we write in a zip entry (closeEntry fails if the underlying stream is closed).
      // In such a case, we will add an intermediate stream
      if (out instanceof ZipOutputStream) {
      	PipedOutputStream encrypterOutput = new PipedOutputStream();
      	PipedInputStream entryWriterInput = new PipedInputStream(encrypterOutput);
      	futures.add(service.submit(new EncrypterTask(encoderInput, encrypterOutput, password, !NEW_ENCODER_ON)));
      	futures.add(service.submit(new PipeTask(entryWriterInput, out)));
      } else {
      	futures.add(service.submit(new EncrypterTask(encoderInput, out, password, !NEW_ENCODER_ON)));
      }

      try {
	      // Wait encoding is ended and gets the errors
				for (Future<? extends Object> future : futures) {
					future.get();
				}
      } catch (ExecutionException e) {
      	Throwable cause = e.getCause();
				if (cause instanceof IOException) throw (IOException)cause;
      	throw new RuntimeException(cause);
      } catch (InterruptedException e) {
      	throw new RuntimeException(e);
			}
		} else {
			XMLSerializer xmlSerializer = new XMLSerializer(out);
			xmlSerializer.serialize(data, report);
			xmlSerializer.closeDocument();
		}

		if (out instanceof ZipOutputStream) {
			((ZipOutputStream) out).closeEntry();
			out.close();
		}
	}

	private static byte[] getHeader(String version) {
		int index = 0;
		byte[] result = new byte[PASSWORD_ENCODED_FILE_HEADER.length];
		for (int i = 0; i < result.length; i++) {
			if (PASSWORD_ENCODED_FILE_HEADER[i]!='*') {
				result[i] = PASSWORD_ENCODED_FILE_HEADER[i];
			} else {
				result[i] = (byte) version.charAt(index);
				index++;
			}
		}
		return result;
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
	 * @param in The input stream containing the data
	 * @param report A progress report to observe the progress, or null
	 * @return The data red.
	 * @throws IOException If something goes wrong while reading
	 * @throws AccessControlException If the password is wrong. Note that if data is not password protected, password argument is ignored
	 * @throws UnsupportedFormatException If the format of data in the input stream is not supported
	 */
	public static GlobalData read(String password, InputStream in, ProgressReport report) throws IOException, AccessControlException {
		// Verify if the stream is encrypted or not
		if (!in.markSupported()) {
			// Ensure that we will be able to reset the stream after verifying that the stream is not encrypted
			in = new BufferedInputStream(in);
		}
		boolean isZipped = isZippedInputStream(in);
		if (isZipped) {
			in = new ZipInputStream(in);
	    ((ZipInputStream)in).getNextEntry();
	    if (!in.markSupported()) in = new BufferedInputStream(in);
		}

		SerializationData serializationData = getSerializationData(in);
		boolean encoded = serializationData.isPasswordRequired;
		if (encoded) {
			if (password==null) throw new AccessControlException("Stream is encoded but password is null"); //$NON-NLS-1$
			// Pass the header
			for (int i = 0; i < PASSWORD_ENCODED_FILE_HEADER.length; i++) in.read();
			
			// Read the file content
			if (! (serializationData.version.equals(V1) || serializationData.version.equals(V2))) {
				throw new UnsupportedFileVersionException("encoded "+serializationData.version);
			}
			
			PipedOutputStream decoderOutput = new PipedOutputStream();
			PipedInputStream deflaterInput = new PipedInputStream(decoderOutput);
			PipedOutputStream deflaterOutput = new PipedOutputStream();
			PipedInputStream readerInput = new PipedInputStream(deflaterOutput);
			
			ExecutorService service = new ThreadPoolExecutor(0, Integer.MAX_VALUE,0, TimeUnit.SECONDS, new SynchronousQueue<Runnable>());
			
			try {
	      Future<Void> decrypter = service.submit(new DecrypterTask(in, decoderOutput, password, serializationData.version.equals(V1)));
	      Future<Void> inflater = service.submit(new InflaterTask(deflaterInput, deflaterOutput));
				Future<GlobalData> reader = service.submit(new ReaderTask(readerInput, password));
	
				decrypter.get(); // Wait encoding is ended
				inflater.get(); // Wait encoding is ended
				
				return reader.get();
			} catch (ExecutionException e) {
				Throwable cause = e.getCause();
				if (cause instanceof IOException) throw (IOException)cause;
				if (cause instanceof AccessControlException) throw (AccessControlException)cause;
				throw new RuntimeException(cause);
			} catch (InterruptedException e) {
				throw new RuntimeException(e);
			}
		} else {
			// Stream is not encoded
			return XMLSerializer.read(in, report);
		}
	}
	
	/** Tests whether a password is the right one for an input stream.
	 * @param in The stream containing Yapbam data
	 * @param password A password (null for no password)
	 * @return true if password is ok, false if not.<br>
	 * Please note that passing a non null password for an non protected stream returns false.<br>
	 * In order to test if a stream is protected or not, you may call this method with null password. If it returns true,
	 * the stream is not protected.<br>
	 * <b>WARNING</b>: This method leaves input stream in an undetermined state (you should close it and reopen it before passing it to read, for instance).
	 * @throws IOException If an I/O error occurred
	 */
	public static boolean isPasswordOk(InputStream in, String password) throws IOException {
		boolean isZipped = isZippedInputStream(in);
		if (isZipped) {
			in = new ZipInputStream(in);
	    ((ZipInputStream)in).getNextEntry();
	    if (!in.markSupported()) in = new BufferedInputStream(in);
		}

		SerializationData serializationData = getSerializationData(in);
		if (!serializationData.isPasswordRequired) {
			return password==null;
		} else {
			try {
				if (password==null) return false;
				// Pass the header
				for (int i = 0; i < PASSWORD_ENCODED_FILE_HEADER.length; i++) in.read();
				DecrypterTask.verifyPassword(in, password);
				return true;
			} catch (AccessControlException e) {
				return false;
			}
		}
	}

	
//	private static void showContent(InputStream in) throws IOException {
//		BufferedReader buf = new BufferedReader(new InputStreamReader(in));
//		try {
//			for (String line=buf.readLine();line!=null;line=buf.readLine()) {
//				System.out.println (line);
//			}
//			System.out.println ("End of stream");
//		} finally {
//			buf.close();
//		}
//	}
	
	/** Gets the data about a stream (what is its version, is it encoded or not, etc...).
	 * <br><b>WARNING:</b> This method leaves the stream with a non determinate number of red bytes if it
	 * does not support mark/reset methods. If it supports these methods (like BufferedInputStream), the stream remains
	 * unchanged.
	 * @param in the stream.
	 * @return A SerializationData instance
	 * @throws IOException
	 */
	private static SerializationData getSerializationData(InputStream in) throws IOException {
		if (in.markSupported()) in.mark(PASSWORD_ENCODED_FILE_HEADER.length);
		boolean isEncoded = true;
		StringBuilder encodingVersion = new StringBuilder();
		for (int i = 0; i < PASSWORD_ENCODED_FILE_HEADER.length; i++) {
			int c = in.read();
			if (PASSWORD_ENCODED_FILE_HEADER[i]=='*') {
				encodingVersion.append((char)c);
			} else {
				if (c!=PASSWORD_ENCODED_FILE_HEADER[i]) {
					isEncoded = false;
					break;
				}
			}
		}
		if (in.markSupported()) in.reset(); // Reset the stream (getSerializationData doesn't guarantee the position of the stream)
		return new SerializationData(isEncoded, encodingVersion.toString());
	}
	
	public static class SerializationData {
		private boolean isPasswordRequired;
		private String version;
		private SerializationData(boolean isEncoded, String version) {
			this.isPasswordRequired = isEncoded;
			this.version = version;
		}
		public boolean isPasswordRequired() {
			return isPasswordRequired;
		}
	}
	
/*	public void serialize(Filter filter) throws SAXException {
 //TODO
 
		atts.clear();
		if (filter.getDateFrom()!=null) atts.addAttribute(EMPTY,EMPTY,FILTER_DATE_FROM_ATTRIBUTE,CDATA,toString(filter.getDateFrom()));
		if (filter.getDateTo()!=null) atts.addAttribute(EMPTY,EMPTY,FILTER_DATE_TO_ATTRIBUTE,CDATA,toString(filter.getDateTo()));
		if (filter.getValueDateTo()!=null) atts.addAttribute(EMPTY,EMPTY,FILTER_VALUE_DATE_TO_ATTRIBUTE,CDATA,toString(filter.getValueDateTo()));
		if (filter.getValueDateFrom()!=null) atts.addAttribute(EMPTY,EMPTY,FILTER_VALUE_DATE_FROM_ATTRIBUTE,CDATA,toString(filter.getValueDateFrom()));
		if (filter.getMinAmount()!=0.0) atts.addAttribute(EMPTY,EMPTY,FILTER_AMOUNT_FROM_ATTRIBUTE,CDATA,Double.toString(filter.getMinAmount()));
		if (filter.getMaxAmount()!=Double.POSITIVE_INFINITY) atts.addAttribute(EMPTY,EMPTY,FILTER_AMOUNT_TO_ATTRIBUTE,CDATA,Double.toString(filter.getMaxAmount()));
		List<Account> accounts = filter.getValidAccounts();
		if (accounts!=null) {
			String[] strings = new String[accounts.size()];
			for (int i = 0; i < strings.length; i++) {
				strings[i] = accounts.get(i).getName();
			}
			atts.addAttribute(EMPTY, EMPTY, ACCOUNT_ATTRIBUTE, CDATA, ArrayUtils.toString(strings));
		}
		List<String> modes = filter.getValidModes();
		if (modes!=null) {
			atts.addAttribute(EMPTY, EMPTY, MODE_ATTRIBUTE, CDATA, ArrayUtils.toString(modes.toArray(new String[modes.size()])));
		}
		List<Category> categories = filter.getValidCategories();
		if (categories!=null) {
			String[] strings = new String[categories.size()];
			for (int i = 0; i < strings.length; i++) {
				strings[i] = categories.get(i).equals(Category.UNDEFINED)?EMPTY:categories.get(i).getName();
			}
			atts.addAttribute(EMPTY, EMPTY, CATEGORY_ATTRIBUTE, CDATA, ArrayUtils.toString(strings));
		}
		int mask = 0;
		if (filter.isOk(Filter.RECEIPTS)) mask += Filter.RECEIPTS;
		if (filter.isOk(Filter.EXPENSES)) mask += Filter.EXPENSES;
		if (filter.isOk(Filter.CHECKED)) mask += Filter.CHECKED;
		if (filter.isOk(Filter.NOT_CHECKED)) mask += Filter.NOT_CHECKED;
		if (mask!=(Filter.ALL)) atts.addAttribute(EMPTY, EMPTY, FILTER_ATTRIBUTE, CDATA, Integer.toString(mask));
		hd.startElement(EMPTY, EMPTY, FILTER_TAG, atts);
		if (filter.getDescriptionMatcher()!=null) serialize(filter.getDescriptionMatcher(), FILTER_DESCRIPTION_ID);
		if (filter.getCommentMatcher()!=null) serialize(filter.getCommentMatcher(), FILTER_COMMENT_ID);
		if (filter.getNumberMatcher()!=null) serialize(filter.getNumberMatcher(), FILTER_NUMBER_ID);
		if (filter.getStatementMatcher()!=null) serialize(filter.getStatementMatcher(), FILTER_STATEMENT_ID);
		hd.endElement(EMPTY,EMPTY,FILTER_TAG);
	}
*/
}
