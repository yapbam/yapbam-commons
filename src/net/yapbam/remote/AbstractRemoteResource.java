package net.yapbam.remote;

import java.net.*;
import java.io.*;

import net.yapbam.remote.Cache;

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
	 * @param proxy The proxy to use to get the data (Proxy.NoProxy to not use any proxy)
	 * @param cache A cache instance, or null to use no cache
	 * @throws IOException if an IOException occurs during the initialization.
	 * @throws ParseException if data is corrupted
	 */
	protected AbstractRemoteResource(Proxy proxy, Cache cache) throws IOException, ParseException {
		this.proxy = proxy;
		this.data = null;
		this.cache = cache==null?new MemoryCache():cache;
		boolean cacheUnavailable = this.cache.isEmpty();
		if (!cacheUnavailable) {
			getLogger().trace("cache is available");
			try {
				// Try to read the cache file
				this.data = parse(cache, false);
			} catch (Exception e) {
				// Cache parsing failed, maybe cache file is not present or is corrupted. 
				// We will call update without try/catch clause to throw exceptions if data can't be red.
				getLogger().warn("Parse failed", e);
				cacheUnavailable = true;
			}
		} else {
			getLogger().trace("cache is unavailable");
		}
		try {
			// If cache was not read update it.
			this.update();
		} catch (IOException e) {
			processException(cacheUnavailable, e);
		} catch (ParseException e) {
			processException(cacheUnavailable, e);
		}
	}

	private  <V extends Exception> void processException(boolean cacheUnavailable, V e) throws V {
		if (cacheUnavailable) {
			throw e;
		} else {
			// Don't throw any exception if update fails as the instance is already initialized with the cache
			// isSynchronized method will return false, indicating that this instance is not synchronized with Internet
			getLogger().warn("Update failed", e);
		}
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
	 */
	public long getTimeStamp() {
		return data==null ? -1 : this.data.getTimeStamp();
	}

	/** Tests whether this converter is synchronized with web server.
	 * @return true if the rates are up to date
	 * @see #update()
	 */
	public boolean isSynchronized() {
		return this.isSynchronized;
	}

	/**
	 * Makes the cache uptodate.
	 * <br>If it is not, downloads again cache file and parse data into internal data structure.
	 * @return true if the web server was called. In order to preserve server resources, it is not called if cache is not so old (ECB refresh its rates never more
	 * than 1 time per day, we don't call ECB again if data is younger than 24 hours. There's also special handling of week-ends). In such a case, this method returns false.
	 * @throws IOException If cache file cannot be read/written or if URL cannot be opened.
	 * @throws ParseException If an error occurs while parsing the XML cache file.
	 */
	public boolean update() throws IOException, ParseException {
		//TODO Review method comment (remove references to ECB). Probably cacheIsExpired should be overridable.
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
	 * <br>The default implementation suppose that 
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
		// hypothetical: rates are never published on Saturdays and Sunday
		int hoursValid = 24 + tolerance;
		if (cal.get(Calendar.DAY_OF_WEEK) == Calendar.FRIDAY) {
			hoursValid = 72;
		} else if (cal.get(Calendar.DAY_OF_WEEK) == Calendar.SATURDAY) {
			hoursValid = 48; 
		}

		return hoursOld > hoursValid;
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
			Writer out = cache.getWriter();
			try {
				for (int c=in.read() ; c!=-1; c=in.read()) {
					out.write(c);
				}
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