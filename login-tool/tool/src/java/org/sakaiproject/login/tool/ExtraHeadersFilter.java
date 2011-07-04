package org.sakaiproject.login.tool;

import java.io.IOException;
import java.util.Map;

import javax.servlet.Filter;
import javax.servlet.FilterChain;
import javax.servlet.FilterConfig;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletRequestWrapper;
import javax.servlet.http.HttpSession;

import org.sakaiproject.tool.api.Session;
import org.sakaiproject.tool.cover.SessionManager;

/**
 * Small filter to add some extra headers to the request, this is used for testing the 
 * two factor authentication code without Shibboleth setup/installed.
 * This filter should go after the Sakai
 * @author buckett
 *
 */
public class ExtraHeadersFilter implements Filter {

	@Override
	public void init(FilterConfig filterConfig) throws ServletException {

	}

	@Override
	public void doFilter(ServletRequest request, ServletResponse response,
			FilterChain chain) throws IOException, ServletException {
		if (request instanceof HttpServletRequest) {
			HttpServletRequest httpRequest = (HttpServletRequest)request;
			Session session = SessionManager.getCurrentSession();
			Map<String,String> extraHeaders  = (Map<String, String>) session.getAttribute("extra-headers");
			if (extraHeaders != null) {
				for(Map.Entry<String, String>header: extraHeaders.entrySet()) {
					// Special case
					if ("REMOTE_USER".equals(header.getKey())) {
						final String remoteUser = header.getValue();
						httpRequest = new HttpServletRequestWrapper(httpRequest) {
							@Override
							public String getRemoteUser() {
								return remoteUser;
							}
						};
					} else {
						httpRequest.setAttribute(header.getKey(), header.getValue());
					}
					
				}
			}
			chain.doFilter(httpRequest, response);
		}

	}

	@Override
	public void destroy() {

	}

}
