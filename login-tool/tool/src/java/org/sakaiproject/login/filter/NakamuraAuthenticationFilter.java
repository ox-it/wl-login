/**********************************************************************************
 * $URL$
 * $Id$
 ***********************************************************************************
 *
 * Copyright (c) 2005, 2006, 2007, 2008 Sakai Foundation
 *
 * Licensed under the Educational Community License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *       http://www.osedu.org/licenses/ECL-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 **********************************************************************************/

package org.sakaiproject.login.filter;

import java.io.IOException;
import java.security.Principal;

import javax.servlet.Filter;
import javax.servlet.FilterChain;
import javax.servlet.FilterConfig;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletRequestWrapper;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.sakaiproject.authz.api.AuthzGroupService;
import org.sakaiproject.component.api.ComponentManager;
import org.sakaiproject.component.cover.ServerConfigurationService;
import org.sakaiproject.event.api.EventTrackingService;
import org.sakaiproject.event.api.UsageSessionService;
import org.sakaiproject.tool.api.SessionManager;
import org.sakaiproject.user.api.UserDirectoryService;
import org.sakaiproject.util.NakamuraAuthenticationHelper;
import org.sakaiproject.util.NakamuraAuthenticationHelper.AuthInfo;
import org.sakaiproject.util.XSakaiToken;

/**
 * A simple {@link Filter} which can be used for container authentication (e.g.
 * like CAS) against a Nakamura instance. Because Nakamura does not currently
 * have a redirecting authentication URL, the user must already be authenticated
 * to Nakamura.
 */
public class NakamuraAuthenticationFilter implements Filter {
	private static final Log LOG = LogFactory
			.getLog(NakamuraAuthenticationFilter.class);

	// dependencies
	private SessionManager sessionManager;
	private UserDirectoryService userDirectoryService;
	private UsageSessionService usageSessionService;
	private EventTrackingService eventTrackingService;
	private AuthzGroupService authzGroupService;
	private NakamuraAuthenticationHelper nakamuraAuthenticationHelper;
	private ComponentManager componentManager;

	/**
	 * All sakai.properties settings will be prefixed with this string.
	 */
	public static final String CONFIG_PREFIX = "org.sakaiproject.login.filter.NakamuraAuthenticationFilter";
	/**
	 * Is the filtered enabled? true == enabled; default = false;
	 */
	public static final String CONFIG_ENABLED = CONFIG_PREFIX + ".enabled";
	/**
	 * The principal that will be used when connecting to Nakamura REST
	 * end-point. Must have permissions to read /var/cluster/user.cookie.json.
	 * 
	 * @see XSakaiToken#createToken(String, String)
	 */
	public static final String CONFIG_PRINCIPAL = CONFIG_PREFIX + ".principal";
	/**
	 * The hostname we will use to lookup the sharedSecret for access to
	 * validateUrl.
	 * 
	 * @see XSakaiToken#createToken(String, String)
	 */
	public static final String CONFIG_HOST_NAME = CONFIG_PREFIX + ".hostname";
	/**
	 * The Nakamura REST end-point we will use to validate the cookie
	 */
	public static final String CONFIG_VALIDATE_URL = CONFIG_PREFIX
			+ ".validateUrl";

	/**
	 * Filter will be disabled by default.
	 * 
	 * @see #CONFIG_ENABLED
	 */
	protected boolean filterEnabled = false;

	/**
	 * The Nakamura RESTful service to validate authenticated users. A good
	 * default for common hybrid implementations is supplied.
	 * 
	 * @see #CONFIG_VALIDATE_URL
	 */
	protected String validateUrl = "http://localhost/var/cluster/user.cookie.json?c=";

	/**
	 * The nakamura user that has permissions to GET
	 * /var/cluster/user.cookie.json. A good default for common hybrid
	 * implementations is supplied.
	 * 
	 * @see XSakaiToken#createToken(String, String)
	 * @see #CONFIG_PRINCIPAL
	 */
	protected String principal = "admin";

	/**
	 * The hostname we will use to lookup the sharedSecret for access to
	 * validateUrl. A good default for common hybrid implementations is
	 * supplied.
	 * 
	 * @see XSakaiToken#createToken(String, String)
	 * @see #CONFIG_HOST_NAME
	 */
	protected String hostname = "localhost";

	/**
	 * @see javax.servlet.Filter#doFilter(javax.servlet.ServletRequest,
	 *      javax.servlet.ServletResponse, javax.servlet.FilterChain)
	 */
	public void doFilter(ServletRequest servletRequest,
			ServletResponse servletResponse, FilterChain chain)
			throws IOException, ServletException {
		if (LOG.isDebugEnabled()) {
			LOG.debug("doFilter(ServletRequest " + servletRequest
					+ ", ServletResponse " + servletResponse + ", FilterChain "
					+ chain + ")");
		}
		if (filterEnabled && servletRequest instanceof HttpServletRequest) {
			final HttpServletRequest request = (HttpServletRequest) servletRequest;

			final AuthInfo authInfo = nakamuraAuthenticationHelper
					.getPrincipalLoggedIntoNakamura(request);
			if (authInfo != null && authInfo.getPrincipal() != null) {
				if (LOG.isDebugEnabled()) {
					LOG.debug("Authenticated to Nakamura proceeding with chain: "
							+ authInfo.getPrincipal());
				}
				chain.doFilter(new NakamuraHttpServletRequestWrapper(request,
						authInfo.getPrincipal()), servletResponse);
				return;
			}
		}
		/*
		 * not enabled or not authenticated to nakamura - just proceed with
		 * normal chain
		 */
		chain.doFilter(servletRequest, servletResponse);
		return;
	}

	/**
	 * @see javax.servlet.Filter#init(javax.servlet.FilterConfig)
	 */
	public void init(FilterConfig filterConfig) throws ServletException {
		LOG.debug("init(FilterConfig filterConfig)");
		filterEnabled = ServerConfigurationService.getBoolean(CONFIG_ENABLED,
				filterEnabled);
		if (filterEnabled) {
			LOG.info("NakamuraAuthenticationFilter ENABLED.");
			validateUrl = ServerConfigurationService.getString(
					CONFIG_VALIDATE_URL, validateUrl);
			LOG.info("vaildateUrl=" + validateUrl);
			principal = ServerConfigurationService.getString(CONFIG_PRINCIPAL,
					principal);
			LOG.info("principal=" + principal);
			hostname = ServerConfigurationService.getString(CONFIG_HOST_NAME,
					hostname);
			LOG.info("hostname=" + hostname);

			// make sure container.login is turned on as well
			final boolean containerLogin = ServerConfigurationService
					.getBoolean("container.login", false);
			if (!containerLogin) {
				LOG.error("container.login must be enabled in sakai.properties for hybrid authentication!");
				throw new IllegalStateException(
						"container.login must be enabled in sakai.properties for hybrid authentication!");
			}
			// what about top.login = false ?
			/**
			 * Whether or not to provide a login form on the portal itself
			 * (typically on the top).
			 */
			final boolean topLogin = ServerConfigurationService.getBoolean(
					"top.login", false);
			if (topLogin) {
				LOG.warn("top.login is usually disabled in sakai.properties for container authentication scenarios");
			}

			componentManager = org.sakaiproject.component.cover.ComponentManager
					.getInstance();
			if (componentManager == null) {
				throw new IllegalStateException("componentManager == null");
			}
			sessionManager = (SessionManager) componentManager
					.get(SessionManager.class);
			if (sessionManager == null) {
				throw new IllegalStateException("SessionManager == null");
			}
			userDirectoryService = (UserDirectoryService) componentManager
					.get(UserDirectoryService.class);
			if (userDirectoryService == null) {
				throw new IllegalStateException("UserDirectoryService == null");
			}
			usageSessionService = (UsageSessionService) componentManager
					.get(UsageSessionService.class);
			if (usageSessionService == null) {
				throw new IllegalStateException("UsageSessionService == null");
			}
			eventTrackingService = (EventTrackingService) componentManager
					.get(EventTrackingService.class);
			if (eventTrackingService == null) {
				throw new IllegalStateException("EventTrackingService == null");
			}
			authzGroupService = (AuthzGroupService) componentManager
					.get(AuthzGroupService.class);
			if (authzGroupService == null) {
				throw new IllegalStateException("AuthzGroupService == null");
			}
			nakamuraAuthenticationHelper = new NakamuraAuthenticationHelper(
					componentManager, validateUrl, principal, hostname);
		}
	}

	/**
	 * @see javax.servlet.Filter#destroy()
	 */
	public void destroy() {
		// nothing to do here
	}

	/**
	 * A simple implementation of {@link Principal} which will be used by
	 * {@link NakamuraHttpServletRequestWrapper}.
	 */
	public static final class NakamuraPrincipal implements Principal {
		private String name = null;

		/**
		 * Create a {@link NakamuraPrincipal} with given name (eid).
		 * 
		 * @param name
		 *            i.e. eid or username.
		 * @throws IllegalArgumentException
		 */
		public NakamuraPrincipal(String name) {
			if (name == null || "".equals(name)) {
				throw new IllegalArgumentException("name == null OR empty");
			}
			this.name = name;
		}

		/**
		 * @see Principal#getName()
		 */
		public String getName() {
			return name;
		}

		/**
		 * @see java.lang.Object#equals(java.lang.Object)
		 */
		@Override
		public boolean equals(Object obj) {
			if (obj == this) {
				return true;
			}
			if (obj instanceof NakamuraPrincipal) {
				return name.equals(((NakamuraPrincipal) obj).getName());
			}
			return false;
		}

		/**
		 * @see java.lang.Object#hashCode()
		 */
		@Override
		public int hashCode() {
			return name.hashCode();
		}

		/**
		 * @see java.lang.Object#toString()
		 */
		@Override
		public String toString() {
			return name;
		}

	}

	/**
	 * A {@link HttpServletRequestWrapper} which uses the passed
	 * {@link NakamuraPrincipal} as the current user when completing the servlet
	 * request.
	 */
	public static class NakamuraHttpServletRequestWrapper extends
			HttpServletRequestWrapper {

		private final Principal principal;

		/**
		 * @param request
		 * @param principal
		 * @throws IllegalArgumentException
		 */
		public NakamuraHttpServletRequestWrapper(
				final HttpServletRequest request, final String principal) {
			super(request);
			if (request == null) {
				throw new IllegalArgumentException("HttpServletRequest == null");
			}
			if (principal == null || "".equals(principal)) {
				throw new IllegalArgumentException("principal == null OR empty");
			}
			this.principal = new NakamuraPrincipal(principal);
		}

		/**
		 * @see javax.servlet.http.HttpServletRequestWrapper#getRemoteUser()
		 */
		@Override
		public String getRemoteUser() {
			return principal != null ? this.principal.getName() : null;
		}

		/**
		 * @see javax.servlet.http.HttpServletRequestWrapper#getUserPrincipal()
		 */
		@Override
		public Principal getUserPrincipal() {
			return this.principal;
		}

		/**
		 * @see javax.servlet.http.HttpServletRequestWrapper#isUserInRole(java.lang.String)
		 */
		@Override
		public boolean isUserInRole(String role) {
			// not needed for this filter
			return false;
		}

	}

}
