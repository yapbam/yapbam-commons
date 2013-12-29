package net.yapbam.remote;

import java.net.*;
import java.io.*;

import net.yapbam.remote.Cache;
import net.yapbam.util.StreamUtils;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.text.*;
import java.util.*;

/**
 * Currency converter based on an Internet foreign exchange rates source.
 * <br>
 * <br>The <b>convert()</b> methods perform currency conversions using either double values or 64-bit long integer values.
 * <br>Long values should be preferred in order to avoid problems associated with floating point arithmetics.
 * <br><br>A local cache file is used for storing exchange rates to reduce network latency and allow offline mode.
 * <br>
 * <br>This converter is compatible with Java Desktop and Android.
 *  
 * @version 1.0 2013-12-16
 * @author Jean-Marc Astesana (based on an original code from <b>Thomas Knierim</br>)
 */
public abstract class AbstractRemoteResource <T extends RemoteData> {
	private Logger logger;
	private Proxy proxy;
	private Cache cache;
	private T data;
	private long lastTryCacheRefresh;
	private boolean isSynchronized;

	/**
	 * Constructor.
	 * <br>The instance is initialized using the cached data, no remote access is done.
	 * This guarantees a (relatively) short execution time and allows it to be executed on the UI thread.
	 * <br>Once initialized this instance should be update with the {@link #update()} method 
	 * (on a background thread in a gui context).
	 * <br>If access to cache fails or if cache is corrupted, the instance is created as if there is no cache.
	 * @param proxy The proxy to use to get the data (Proxy.NoProxy to not use any proxy)
	 * @param cache A cache instance, or null to use no cache
	 * @see #update()
	 */
	protected AbstractRemoteResource(Proxy proxy, Cache cache) {
		this.proxy = proxy;
		this.data = null;
		this.cache = cache==null?new MemoryCache():cache;
		try {
			// Try to read the cache file
			this.data = parse(cache, false);
			this.isSynchronized = !isDataExpired();
		} catch (Exception e) {
			// Cache parsing failed, maybe cache file is not present or is corrupted. 
			// We will call update without try/catch clause to throw exceptions if data can't be red.
			getLogger().warn("Parse failed", e);
		}
		this.lastTryCacheRefresh = this.cache.getTimeStamp();
	}

	/** Gets a logger.
	 * <br>This logger is used by the class to log events.
	 */
	protected Logger getLogger() {
		if (logger==null) {
			logger = LoggerFactory.getLogger(getClass());
		}
		return logger;
	}

	/**
	 * Gets the time stamp of the data as ms since January 1, 1970, 00:00:00 GMT.
	 * <br>This is used by {@link #isDataExpired()} to determine if server should be asked for new data
	 * @return a positive long or a negative number if the data structure has not yet been initialized.
	 * @see #getRefreshTimeStamp()
	 */
	public long getTimeStamp() {
		return data==null ? -1 : this.data.getTimeStamp();
	}
	
	/** Gets the last successful refresh date as ms since January 1, 1970, 00:00:00 GMT.
	 * <br>Note this can be different of {@link #getTimeStamp()}. For instance, last time we contact the server
	 * (let say at 10 o'clock) it returned data that was updated some time ago (let say at 9 o'clock).
	 * In such a case, this method will return 10 o'clock and {@link #getTimeStamp()} 9 o'clock.
	 * @return
	 */
	public long getRefreshTimeStamp() {
		return cache.getTimeStamp();
	}
	
	/** Tests whether this converter is synchronized with web server.
	 * @return true if the rates are up to date
	 * @see #update()
	 */
	public boolean isSynchronized() {
		return this.isSynchronized;
	}

	/**
	 * Makes the cache up to date.
	 * <br>If it is not, downloads again cache file and parse data into internal data structure.
	 * <br>After this method is called, {@link #isSynchronized()} always return true (even if server has not been called).
	 * @return true if the web server was called.
	 * @throws ParseException If an error occurs while parsing the XML cache file.
	 * @see #forceUpdate()
	 * @see #isDataExpired()
	 */
	public boolean update() throws IOException, ParseException {
		boolean connect = isDataExpired();
		if (connect) {
			forceUpdate();
		}
		this.isSynchronized = true;
		return connect;
	}
	
	/**
	 * Forces the cache to be refreshed.
	 * <br>Always downloads again cache file and parse data into internal data structure.
	 * @throws IOException If cache file cannot be read/written or if URL cannot be opened.
	 * @throws ParseException If an error occurs while parsing the XML cache file.
	 * @see #update()
	 */
	public void forceUpdate() throws IOException, ParseException {
		long start = System.currentTimeMillis();
		refreshCacheFile();
		getLogger().debug("refresh cache: {}ms",Long.toString(System.currentTimeMillis()-start));
		start = System.currentTimeMillis();
		this.data = parse(cache, true);
		getLogger().debug("parse: {}ms",Long.toString(System.currentTimeMillis()-start));
		start = System.currentTimeMillis();
		cache.commit();
		getLogger().debug("commit: {}ms",Long.toString(System.currentTimeMillis()-start));
	}

	/**
	 * Checks whether data needs to be updated.
	 * <br>This method is called by {@link #update()} before calling the server. If this method returns false,
	 * the server is not called. Please note that {@link #forceUpdate()} does not use this method and always calls the server.
	 * <br>The default implementation suppose that data is published once a day except during week-end.
	 * <br>You can override this method in order to change this behavior.
	 * <br>Remember that:<ul>
	 * <li>{@link #getTimeStamp()} can return a negative number. In such a case, that method should return true.</li>
	 * <li>It is a good practice to use {@link #getLastRefreshTimeStamp()} in order to limit calls to the remote server.
	 * <br>For example, you can return false if the last server call was less than a minute.</li>
	 * </ul>
	 * @return true if data needs to be updated, false otherwise.
	 */
	protected boolean isDataExpired() {
		if (getTimeStamp() < 0) {
			return true;
		}
		// If we connect to server since less than one minute ... do nothing
		// This could happen if server doesn't refresh its rates since the last time we
		// updated the cache file (and more than the "standard" cache expiration time defined below)
		if (System.currentTimeMillis() - lastTryCacheRefresh < 60000) {
			return false;
		}
		
		final int tolerance = 12;
		Calendar cal = Calendar.getInstance(TimeZone.getTimeZone("GMT")); //$NON-NLS-1$
		long hoursOld = (cal.getTimeInMillis() - getTimeStamp()) / (1000 * 60 * 60);
		cal.setTimeInMillis(getTimeStamp());
		// hypothetical: data is never published on Saturday and Sunday
		int hoursValid = 24 + tolerance;
		if (cal.get(Calendar.DAY_OF_WEEK) == Calendar.FRIDAY) {
			hoursValid = 72;
		} else if (cal.get(Calendar.DAY_OF_WEEK) == Calendar.SATURDAY) {
			hoursValid = 48; 
		}

		return hoursOld > hoursValid;
	}
	
	/**
	 * Gets the last data refresh attempt time stamp as ms since January 1, 1970, 00:00:00 GMT.
	 * <br>This is used by {@link #isDataExpired()} to determine if server should be asked for new data.
	 * @return a positive long.
	 * @see #isDataExpired()
	 */
	protected final long getLastRefreshTimeStamp() {
		return lastTryCacheRefresh;
	}
	
	protected abstract URL getSourceURL(); 

	/**
	 * (Re-) download the XML cache file and store it in a temporary location.
	 * 
	 * @throws IOException
	 *           If (1) URL cannot be opened, or (2) if cache file cannot be
	 *           opened, or (3) if a read/write error occurs.
	 */
	private void refreshCacheFile() throws IOException {
		lastTryCacheRefresh = System.currentTimeMillis();
		getLogger().trace("Connecting to {}", getSourceURL());
		InputStream in = getSourceStream();
		try {
			OutputStream out = cache.getOutputStream();
			try {
				StreamUtils.copy(in, out, new byte[10024]);
			} finally {
				out.flush();
				out.close();
			}
		} finally {
			in.close();
		}
	}

	private InputStream getSourceStream() throws IOException {
		URL url = getSourceURL();
		if (url==null) {
			throw new FileNotFoundException();
		}
		URLConnection connection = url.openConnection(proxy);
		if (connection instanceof HttpURLConnection) {
			HttpURLConnection ct = (HttpURLConnection) connection;
			int errorCode = ct.getResponseCode();
			if (errorCode != HttpURLConnection.HTTP_OK) {
				throw new IOException(MessageFormat.format("Http Error {1} when opening {0}", url, errorCode)); //$NON-NLS-1$
			}
		}
		return connection.getInputStream();
	}
	
	/**
	 * Parses cache file and create internal data structures containing exchange rates.
	 * <br>Be aware that cache may be empty. In such a case, parse should return an empty T instance
	 * @param tmp true to parse the tmp cache, false to parse the official cache
	 * @return 
	 * @throws ParseException If XML file cannot be parsed.
	 * @throws IOException if connection to the URL or writing to cache file fails.
	 * @see Cache
	 */
	protected abstract T parse(Cache cache, boolean tmp) throws ParseException, IOException;
	
	protected T getData() {
		return this.data;
	}
}
