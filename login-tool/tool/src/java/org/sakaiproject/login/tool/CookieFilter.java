package org.sakaiproject.login.tool;

import java.io.IOException;

import javax.servlet.Filter;
import javax.servlet.FilterChain;
import javax.servlet.FilterConfig;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

/**
 * Simple filter to set a cookie.
 * This is so that we can filter all WebAuth logins and set a non-secure cookie
 * which we can use to optimistically login to the other system. 
 * @author buckett
 *
 */
public class CookieFilter implements Filter {

	private static final Log log = LogFactory.getLog(CookieFilter.class);

	private String cookieName;
	private String cookieValue;
	private String cookiePath;
	private boolean cookieSecure;
	private int cookieAge;
	
	private boolean enabled;
	private boolean remove;
	private boolean always;

	public void destroy() {

	}

	public void doFilter(ServletRequest request, ServletResponse response,
			FilterChain chain) throws IOException, ServletException {
		chain.doFilter(request, response);
		if (enabled) {
			if (response.isCommitted()) {
				log.warn("The response has already been committed, we can't set a cookie.");
			}
			if (request instanceof HttpServletRequest && response instanceof HttpServletResponse) {
				HttpServletRequest httpRequest = (HttpServletRequest)request;
				HttpServletResponse httpResponse = (HttpServletResponse)response;

				if (always || hasCookie(httpRequest) == remove) {
					Cookie cookie = new Cookie(cookieName, getCookieValue(httpRequest));
					cookie.setSecure(cookieSecure);
					cookie.setMaxAge(cookieAge);
					if (cookiePath != null) {
						cookie.setPath(cookiePath);
					}
					httpResponse.addCookie(cookie);
				}
			}
		}
	}

	private boolean hasCookie(HttpServletRequest httpRequest) {
		Cookie[] cookies = httpRequest.getCookies();
		boolean hasCookie = false;
		if (cookies != null) {
			for (Cookie cookie: cookies) {
				if (cookieName.equals(cookie.getName())) {
					hasCookie = true;
					break;
				}
			}
		}
		return hasCookie;
	}

	public void init(FilterConfig filterConfig) throws ServletException {
		cookieName = filterConfig.getInitParameter("name");
		cookieValue = filterConfig.getInitParameter("value");
		cookiePath = filterConfig.getInitParameter("path");
		cookieSecure = Boolean.valueOf(filterConfig.getInitParameter("secure")).booleanValue();
		remove = Boolean.valueOf(filterConfig.getInitParameter("remove")).booleanValue();
		always = Boolean.valueOf(filterConfig.getInitParameter("always")).booleanValue();
		cookieAge = (remove)?0:-1;
		
		if (cookieName != null && cookieName.length() > 0 &&
				cookieValue != null && cookieValue.length() > 0) {
			enabled = true;
			log.info("Cookie of "+ cookieName+ " will be "+ ((remove)?"removed":"created"));
		} else {
			log.error("You need to set name/value init parameters for this filter to work.");
		}
	}
	
	public String getCookieValue(HttpServletRequest request) {
		return cookieValue;
	}

}
