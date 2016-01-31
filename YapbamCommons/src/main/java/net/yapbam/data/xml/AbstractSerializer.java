package net.yapbam.data.xml;

import java.io.*;
import java.security.AccessControlException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;
import java.util.concurrent.SynchronousQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

import net.yapbam.data.*;
import net.yapbam.data.xml.task.DecrypterTask;
import net.yapbam.data.xml.task.DeflaterTask;
import net.yapbam.data.xml.task.EncrypterTask;
import net.yapbam.data.xml.task.InflaterTask;
import net.yapbam.data.xml.task.PipeTask;
import net.yapbam.util.Crypto;

/** The class implements xml yapbam data serialization and deserialization to (or from) an URL.
 * Currently supported URL type are :<UL>
 * <LI> file.
 * </UL>
 */
public abstract class AbstractSerializer<T> {
	private static final boolean NEW_ENCODER_ON = true;

	/** The password encoded file header scheme.
	 * the * characters means "the ending version is coded there".
	 */
	private static final byte[] PASSWORD_ENCODED_FILE_HEADER = toBytes("<Yapbam password encoded file ***>"); //$NON-NLS-1$
	private static final String V1 = "1.0"; //$NON-NLS-1$
	private static final String V2 = "2.0"; //$NON-NLS-1$
	
	static {
		// A lot of code relies on the fact that V1 and V2 have the same length and that this length is the same as the number
		// of * in PASSWORD_ENCODED_FILE_HEADER
		// This code verifies it is always true
		int nb = 0;
		for (byte c : PASSWORD_ENCODED_FILE_HEADER) {
			if (c=='*') {
				nb++;
			}
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
	 * @param out The outputStream (Note that this stream is not closed by this method).
	 * @param password the password used to protect written data
	 * @param report a progress report
	 * @throws IOException if something goes wrong while writing
	 */
	public void write(final T data, OutputStream out, String password, final ProgressReport report) throws IOException {
		if (password!=null) {
			// If the file has to be protected by a password
			// outputs the magic bytes that will allow Yapbam to recognize the file is crypted.
			out.write(getHeader(NEW_ENCODER_ON?V2:V1));
			final PipedOutputStream xmlOutput = new PipedOutputStream();
			PipedInputStream compressorInput = new PipedInputStream(xmlOutput);
			
			PipedOutputStream compressorOutput = new PipedOutputStream();
			PipedInputStream encoderInput = new PipedInputStream(compressorOutput);
			
			ExecutorService service = new ThreadPoolExecutor(0, Integer.MAX_VALUE,0, TimeUnit.SECONDS, new SynchronousQueue<Runnable>());

			List<Future<? extends Object>> futures = new ArrayList<Future<? extends Object>>(3);
			Callable<Void> c = new Callable<Void>() {
				@Override
				public Void call() throws Exception {
					try {
						directWrite(data, xmlOutput, report);
						return null;
					} finally {
						xmlOutput.close();
					}
				}
			};
			futures.add(service.submit(c));
			futures.add(service.submit(new DeflaterTask(compressorInput, compressorOutput)));
      
			// As encryterTask closes its output stream (required to process the doFinal of the encryption cipher),
			// We can't pass it directly the out stream. So we will add an intermediate stream
			PipedOutputStream encrypterOutput = new PipedOutputStream();
			PipedInputStream entryWriterInput = new PipedInputStream(encrypterOutput);
			futures.add(service.submit(new EncrypterTask(encoderInput, encrypterOutput, password, !NEW_ENCODER_ON)));
			futures.add(service.submit(new PipeTask(entryWriterInput, out)));

			try {
				// Wait encoding is ended and gets the errors
				for (Future<? extends Object> future : futures) {
					future.get();
				}
			} catch (ExecutionException e) {
				Throwable cause = e.getCause();
				if (cause instanceof IOException) {
					throw (IOException)cause;
				} else {
					throw new RuntimeException(cause);
				}
			} catch (InterruptedException e) {
				throw new RuntimeException(e);
			}
		} else {
			directWrite(data, out, report);
		}
	}

	public abstract void directWrite(T data, OutputStream out, ProgressReport report) throws IOException;
	
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
	
	/** Reads global data.
	 * @param password The password of the data (null if the data is not password protected)
	 * @param in The input stream containing the data (if the stream is a stream on a zipped file, for instance created by write(GlobalData, ZipOutputStream, String, ProgressReport)
	 * the data is automatically unzipped from the first entry).
	 * @param report A progress report to observe the progress, or null
	 * @return The read data.
	 * @throws IOException If something goes wrong while reading
	 * @throws AccessControlException If the password is wrong. Note that if data is not password protected, password argument is ignored
	 * @throws UnsupportedFormatException If the format of data in the input stream is not supported
	 */
	public T read(final String password, InputStream in, final ProgressReport report) throws IOException, AccessControlException {
		SerializationData serializationData = getSerializationData(in);
		boolean encoded = serializationData.isPasswordRequired;
		if (encoded) {
			if (password==null) {
				throw new AccessControlException("Stream is encoded but password is null"); //$NON-NLS-1$
			}
			// Pass the header
			for (int i = 0; i < PASSWORD_ENCODED_FILE_HEADER.length; i++) {
				in.read();
			}
			
			// Read the file content
			if (! (serializationData.version.equals(V1) || serializationData.version.equals(V2))) {
				throw new UnsupportedFileVersionException("encoded "+serializationData.version);
			}
			
			PipedOutputStream decoderOutput = new PipedOutputStream();
			PipedInputStream deflaterInput = new PipedInputStream(decoderOutput);
			PipedOutputStream deflaterOutput = new PipedOutputStream();
			final PipedInputStream readerInput = new PipedInputStream(deflaterOutput);
			
			ExecutorService service = new ThreadPoolExecutor(0, Integer.MAX_VALUE,0, TimeUnit.SECONDS, new SynchronousQueue<Runnable>());
			
			try {
				Future<Void> decrypter = service.submit(new DecrypterTask(in, decoderOutput, password, serializationData.version.equals(V1)));
				Future<Void> inflater = service.submit(new InflaterTask(deflaterInput, deflaterOutput));
				Callable<T> c = new Callable<T>() {
					@Override
					public T call() throws Exception {
						try {
							return directRead(password, readerInput, report);
						} finally {
							readerInput.close();
						}
					}
				};
				Future<T> reader = service.submit(c);
	
				decrypter.get(); // Wait encoding is ended
				inflater.get(); // Wait encoding is ended
				
				return reader.get();
			} catch (ExecutionException e) {
				Throwable cause = e.getCause();
				if (cause instanceof IOException) {
					throw (IOException)cause;
				} else if (cause instanceof AccessControlException) {
					throw (AccessControlException)cause;
				} else {
					throw new RuntimeException(cause);
				}
			} catch (InterruptedException e) {
				throw new RuntimeException(e);
			}
		} else {
			// Stream is not encoded
			return directRead(password, in, report);
		}
	}
	
	/** Reads the data contained in a stream.
	 * @param password The stream password
	 * @param in The input stream where to read data
	 * @param report A progress report.
	 * @return The read data, or null if the operation is cancelled (by calling report.cancel() on another thread).
	 * @throws IOException if something goes wrong
	 */
	public abstract T directRead(String password, InputStream in, ProgressReport report) throws IOException;
	
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
	public boolean isPasswordOk(InputStream in, String password) throws IOException {
		SerializationData serializationData = getSerializationData(in);
		if (!serializationData.isPasswordRequired) {
			return password==null;
		} else {
			try {
				if (password==null) {
					return false;
				}
				// Pass the header
				for (int i = 0; i < PASSWORD_ENCODED_FILE_HEADER.length; i++) {
					in.read();
				}
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
	 * <br><b>WARNING:</b> This method leaves the stream with a non determinate number of read bytes if it
	 * does not support mark/reset methods. If it supports these methods (like BufferedInputStream), the stream remains
	 * unchanged.
	 * @param in the stream.
	 * @return A SerializationData instance
	 * @throws IOException
	 */
	private static SerializationData getSerializationData(InputStream in) throws IOException {
		if (in.markSupported()) {
			in.mark(PASSWORD_ENCODED_FILE_HEADER.length);
		}
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
		if (in.markSupported()) {
			// Reset the stream (getSerializationData doesn't guarantee the position of the stream)
			in.reset();
		}
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
}
