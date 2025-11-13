package edu.sb.tool;

import java.security.KeyManagementException;
import java.security.NoSuchAlgorithmException;
import java.security.cert.X509Certificate;
import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;


/**
 * X509 trust manager that accepts any client and server,
 * regardless of circumstances.
 */
@Copyright(year=2023, holders="Sascha Baumeister")
public class TrustAllManager implements X509TrustManager {

	/**
	 * Checks if a client can be trusted based on the given arguments;
	 * never complains.
	 * @param certificateChain the certificate chain
	 * @param authType the authentication type
	 */
	@Override
	public void checkClientTrusted (final X509Certificate[] certificateChain, final String authType) {}


	/**
	 * Checks if a server can be trusted based on the given arguments;
	 * never complains.
	 * @param certificateChain the certificate chain
	 * @param authType the authentication type
	 */
	@Override
	public void checkServerTrusted (final X509Certificate[] certificateChain, final String authType) {}


	/**
	 * Returns an empty X509 certificate array.
	 * @return the empty X509 certificate array
	 */
	@Override
	public X509Certificate[] getAcceptedIssuers () {
		return new X509Certificate[0];
	}


	/**
	 * Creates and returns a new transport layer security context based
	 * on an all-trusting trust manager.
	 * @return the trust manager created
	 */
	static public SSLContext newTLSContext () {
		try {
			final SSLContext context = SSLContext.getInstance("TLSv1.3");
			context.init(null, new TrustManager[] { new TrustAllManager() }, null);
			return context;
		} catch (final NoSuchAlgorithmException | KeyManagementException e) {
			throw new AssertionError(e);
		}
	}
}
