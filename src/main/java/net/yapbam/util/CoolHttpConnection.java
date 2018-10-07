package net.yapbam.util;

import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.Proxy;
import java.net.URL;
import java.security.SecureRandom;
import java.security.cert.X509Certificate;

import javax.net.ssl.HttpsURLConnection;
import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/** A Http connection that follows redirects from http to https and patches bugs in java6 and 7.
 */
public class CoolHttpConnection {
	private static final String HTTPS_PROTOCOL = "https";
	private static final Logger LOGGER = LoggerFactory.getLogger(CoolHttpConnection.class);
	public static final boolean IS_SSL_PATCHED;
	private static ProxiedFailover proxiedFailOver = null; 

	private HttpURLConnection ct;
	
	static {
		// Deactivate SSL certificate checking for old versions of java (java has its local approved certificate repository).
		// This results in old java version to not trust recent valid certificates (here we patch versions that does not trust LetsEncrypt).
		IS_SSL_PATCHED = patchSSL();
	}
	
	public interface ProxiedFailover {
		URL getProxied(URL url) throws IOException;
	}
	
	public static void setProxiedFailOver(ProxiedFailover failOver) {
		proxiedFailOver = failOver;
	}
	
	private static boolean patchSSL() {
		String javaVersion = System.getProperty("java.version");
		boolean deactivate = false;
		if (javaVersion.startsWith("1.8_")) {
			try {
				int release = Integer.parseInt(javaVersion.substring("1.8_".length()));
				if (release < 101) {
					deactivate=true;
				}
			} catch (NumberFormatException e) {
				LOGGER.warn("Unable to find java 8 release number", e);
			}
		} else if (javaVersion.startsWith("1.7") || javaVersion.startsWith("1.6")) {
			deactivate = true;
		}
		if (deactivate) {
			deactivateSSLCertificate();
			LOGGER.warn("{} java version is too old, SSL certificate checking is deactivated",javaVersion);
		}
		return deactivate;
	}

	protected static void deactivateSSLCertificate() {
		// Create a trust manager that does not validate certificate chains
		TrustManager[] trustAllCerts = new TrustManager[] {
			new X509TrustManager() {
				public X509Certificate[] getAcceptedIssuers() {
					return new X509Certificate[0];
				}

				public void checkClientTrusted(X509Certificate[] certs, String authType) {
				}

				public void checkServerTrusted(X509Certificate[] certs, String authType) {
				}
			}
		};

		// Install the all-trusting trust manager
		try {
			SSLContext sc = SSLContext.getInstance("TLS");
			sc.init(null, trustAllCerts, new SecureRandom());
			HttpsURLConnection.setDefaultSSLSocketFactory(sc.getSocketFactory());
		} catch (Exception e) {
			throw new RuntimeException(e);
		}
	}

	public CoolHttpConnection(URL url, Proxy proxy, URL... failOvers) throws IOException {
		try {
			this.ct = buildConnection(url, proxy);
		} catch (IOException e) {
			if (!tryFailOvers(proxy, url, e, failOvers)) {
				throw e;
			}
		} catch (RuntimeException e) {
			if (!tryFailOvers(proxy, url, e, failOvers)) {
				throw e;
			}
		}
	}
	
	private boolean tryFailOvers(Proxy proxy, URL original, Exception e, URL... failOvers) throws IOException {
		LOGGER.warn("Error while trying target URL "+original, e);
		for (URL failOver : failOvers) {
			if (tryIt(proxy, failOver)) {
				return true;
			}
		}
		if (proxiedFailOver!=null && IS_SSL_PATCHED) {
			if (HTTPS_PROTOCOL.equals(original.getProtocol()) && tryIt(proxy, proxiedFailOver.getProxied(original))) {
				return true;
			}
			for (URL failOver : failOvers) {
				if (HTTPS_PROTOCOL.equals(failOver.getProtocol()) && tryIt(proxy, proxiedFailOver.getProxied(failOver))) {
					return true;
				}
			}
		}
		return false;
	}

	private boolean tryIt(Proxy proxy, URL failOver) {
		try {
			this.ct = buildConnection(failOver, proxy);
			return true;
		} catch (Exception ee) {
			LOGGER.warn("Error while trying failover URL "+failOver, ee);
			return false;
		}
	}

	private HttpURLConnection buildConnection(URL url, Proxy proxy) throws IOException {
		HttpURLConnection connect = (HttpURLConnection) url.openConnection(proxy);
		while (connect.getResponseCode()==HttpURLConnection.HTTP_MOVED_PERM || connect.getResponseCode()==HttpURLConnection.HTTP_MOVED_TEMP) {
			String redirect = connect.getHeaderField("Location");
			connect.disconnect();
			url = new URL(redirect);
			return (HttpURLConnection) url.openConnection(proxy);
		}
		return connect;
	}

	public int getResponseCode() throws IOException {
		return this.ct.getResponseCode();
	}

	public InputStream getInputStream() throws IOException {
		return this.ct.getInputStream();
	}

	public String getContentEncoding() {
		return ct.getContentEncoding();
	}

	public String getHeaderField(String field) {
		return ct.getHeaderField(field);
	}
}
