package org.sakaiproject.login.tool;

import javax.servlet.FilterConfig;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;


/**
 * Attempt to work out when the Webauth session expires.
 * @author buckett
 *
 */
public class WACookieFilter extends CookieFilter {

	private static final Log log = LogFactory.getLog(WACookieFilter.class);
	
	// Default of 8 hours.
	private int defaultDuration = 1000 * 60 * 60 * 8;
	
	// Minimum duration of 30 minutes.
	private int minimumDuration = 1000 * 60 * 30;

	// Request parameter to pull the expiry time from.
	private static String requestParam = "WEBAUTH_TOKEN_EXPIRATION";

	
	public void init(FilterConfig config) throws ServletException {
		super.init(config);
		String param = null;
		
		param = config.getInitParameter("requestParam");
		if (param != null && param.length() > 0) {
			requestParam = (String)param;
		}
		
		try {
			param = config.getInitParameter("defaultDuration");
			if (param != null && param.length() > 0) {
				defaultDuration = Integer.parseInt(param);
			}
		} catch (NumberFormatException nfe) {
			log.warn("defaultDuration is not a number: "+ param);
		}
		try {
			param = config.getInitParameter("minimumDuration");
			if (param != null && param.length() > 0) {
				minimumDuration = Integer.parseInt(param);
			}
		} catch (NumberFormatException nfe) {
			log.warn("minimumDuration is not a number: "+ param);
		}
	}
	
	public String getCookieValue(HttpServletRequest request) {
		Object attr = request.getAttribute(requestParam);
		long now = System.currentTimeMillis();
		long expiryTime = now + minimumDuration;
		if (attr != null && attr instanceof String) {
			String attrString = (String)attr;
			try {
				// Webauth time is just in sec since epoch.
				long expiryTimeWA = Long.parseLong(attrString)*1000;
				if (expiryTimeWA < now) {
					log.warn("WebAuth session has apparently already expired. Time: "+ now+ " Expired: "+ expiryTimeWA);
				} else if (expiryTimeWA < now + minimumDuration) {
					log.debug("Webauth session expires too quickly, giving user a little grace.");
				} else {
					expiryTime = expiryTimeWA;
				}
			} catch (NumberFormatException nfe) {
				log.info("WebAuth expiry isn't a number: "+ attrString);
			}
		} else {
			log.warn("Request attribute isn't good: "+ attr);
			expiryTime = now + defaultDuration;
		}
		return Long.toString(expiryTime);
	}
}
