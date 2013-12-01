package net.yapbam.util;

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.Proxy;
import java.net.URL;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

import net.yapbam.util.CheckSum;

/** Utility to download an URL to a file and compute the checksum of the downloaded file.
 * @author Jean-Marc Astesana
 * <BR>License : GPL v3
 */
public class SecureDownloader {
	public static class DownloadInfo {
		private String checkSum;
		private long length;
		private DownloadInfo(String checkSum, long length) {
			super();
			this.checkSum = checkSum;
			this.length = length;
		}
		/** Gets the MD5 checksum of the downloaded file.
		 * @return a String or null if the file was not completely downloaded.
		 * @see #download(URL, File)
		 */
		public String getCheckSum() {
			return checkSum;
		}
		/** Gets the number of bytes downloaded since the last call to download.
		 * @return a long.
		 * @see #download(URL, File)
		 */
		public long getDownloadedSize() {
			return length;
		}
	}

	private Proxy proxy;
	private boolean cancelled;
	
	/** Constructor.
	 * @param proxy The proxy to used to connect to the download site.
	 * @see java.net.Proxy
	 */
	public SecureDownloader(Proxy proxy) {
		this.proxy = proxy;
		this.cancelled = false;
	}
	
	/** Download an URL to a file.
	 * @param url The url to download.
	 * @param file The file where to store the URL content. Null if you don't want to save the file (if you just want to compute the MD5 hash).
	 * @return The download info or null if the download was cancelled.
	 * @throws IOException if there's a problem during the download.
	 */
	public synchronized DownloadInfo download(URL url, File file) throws IOException {
		InputStream in = url.openConnection(proxy).getInputStream();
		try {
			OutputStream out = file==null?null:new BufferedOutputStream(new FileOutputStream(file));
			try {
				return copy(in, out);
			} finally {
				if (out!=null) {
					out.flush();
					out.close();
				}
			}
		} finally {
			in.close();
		}
	}

	/** Copy the content of an input stream to an output stream.
	 * @param in The input stream
	 * @param out The output stream, or null to simply compute checksum without copying it.
	 * @return the check and length of the content of the input stream or null if the copy was cancelled
	 * @throws IOException
	 */
	public synchronized DownloadInfo copy(InputStream in, OutputStream out) throws IOException {
		long length = 0;
		this.cancelled = false;
		// First, create a digest to verify the checksum
		MessageDigest digest;
		try {
			digest = MessageDigest.getInstance("MD5");
		} catch (NoSuchAlgorithmException e) {
			throw new RuntimeException(e);
		}
		int size;
		byte[] buffer = new byte[8192];
		while ((size = in.read(buffer, 0, buffer.length)) != -1) {
			if (cancelled) {
				break;
			}
			if (out!=null) {
				out.write(buffer, 0, size);
			}
			if (size>0) {
				digest.update(buffer, 0, size);
				length += size;
				progress(length);
			}
		}
		return cancelled ? null : new DownloadInfo(CheckSum.toString(digest.digest()), length);
	}
	
	/** Reports the download progress.
	 * @param downloadedSize The currently downloaded size in bytes.
	 * <br>This method is called each time the download method get a block of data.
	 * <br>The default implementation does nothing.
	 * @see #download(URL, File)
	 */
	protected void progress(long downloadedSize) {
		// Default implementation does nothing
	}

	/** Tests whether the download has been canceled or not.
	 * @return true if the download has been canceled. Please note that each time the download method is call, this value is reset to false.
	 * @see #cancel()
	 */
//	public boolean isCancelled() {
//		return this.cancelled;
//	}
	
	/** Cancels the download.
	 */
	public void cancel() {
		this.cancelled = true;
	}
}
