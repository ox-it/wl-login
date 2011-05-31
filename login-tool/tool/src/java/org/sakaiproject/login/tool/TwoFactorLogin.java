/**********************************************************************************
 * $URL$
 * $Id$
 ***********************************************************************************
 *
 * Copyright (c) 2005, 2006 The Sakai Foundation.
 * 
 * Licensed under the Educational Community License, Version 1.0 (the "License"); 
 * you may not use this file except in compliance with the License. 
 * You may obtain a copy of the License at
 * 
 *      http://www.opensource.org/licenses/ecl1.php
 * 
 * Unless required by applicable law or agreed to in writing, software 
 * distributed under the License is distributed on an "AS IS" BASIS, 
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. 
 * See the License for the specific language governing permissions and 
 * limitations under the License.
 *
 **********************************************************************************/

package org.sakaiproject.login.tool;

import java.io.IOException;

import javax.servlet.ServletConfig;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.sakaiproject.authz.api.TwoFactorAuthentication;
import org.sakaiproject.component.cover.ComponentManager;
import org.sakaiproject.component.cover.ServerConfigurationService;
import org.sakaiproject.tool.api.Session;
import org.sakaiproject.tool.api.Tool;
import org.sakaiproject.tool.cover.SessionManager;
import org.sakaiproject.util.Web;

/**
 * <p>
 * ContainerLogin ...
 * </p>
 */
public class TwoFactorLogin extends HttpServlet
{
	/**
	 * Attribute set when container login went well.
	 */
	public static final String ATTR_TWOFACTOR_SUCCESS = TwoFactorLogin.class.getName()+"#container.success";

	/** Session attribute set and shared with ContainerLoginTool: if set we have failed container and need to check internal. */
	public static final String ATTR_TWOFACTOR_CHECKED = "sakai.login.container.checked";
	
	/** Our log (commons). */
	private static Log M_log = LogFactory.getLog(TwoFactorLogin.class);
	
	private String defaultReturnUrl;

	private String redirectParameter = "redirect";

	/**
	 * Access the Servlet's information display.
	 * 
	 * @return servlet information.
	 */
	public String getServletInfo()
	{
		return "Sakai Two Factor Login";
	}

	/**
	 * Initialize the servlet.
	 * 
	 * @param config
	 *        The servlet config.
	 * @throws ServletException
	 */
	public void init(ServletConfig config) throws ServletException
	{
		super.init(config);

		M_log.info("init()");
		defaultReturnUrl = ServerConfigurationService.getString("portalPath", "/portal"); 
	}

	/**
	 * Shutdown the servlet.
	 */
	public void destroy()
	{
		M_log.info("destroy()");

		super.destroy();
	}

	/**
	 * Respond to requests.
	 * 
	 * @param req
	 *        The servlet request.
	 * @param res
	 *        The servlet response.
	 * @throws ServletException.
	 * @throws IOException.
	 */
	protected void doGet(HttpServletRequest req, HttpServletResponse res) throws ServletException, IOException
	{
		//System.out.println("TwoFactorLogin.doGet");
		// get the session
		Session session = SessionManager.getCurrentSession();
		
		// This url may not have been processed by apache
		if (session.getAttribute(ATTR_TWOFACTOR_CHECKED) == null) {
			String twoFactorCheckPath = this.getServletConfig().getInitParameter("twofactor");
			String twoFactorCheckUrl = Web.serverUrl(req) + twoFactorCheckPath;

			String queryString = req.getQueryString();
			if (queryString != null) twoFactorCheckUrl = twoFactorCheckUrl + "?" + queryString;

			session.setAttribute(ATTR_TWOFACTOR_CHECKED, "true");
			//System.out.println("TwoFactorLogin.doGet ATTR_TWOFACTOR_CHECKED ["+twoFactorCheckUrl+"]");
			res.sendRedirect(res.encodeRedirectURL(twoFactorCheckUrl));
			return;
		}
		

		// check the remote user for authentication
		String remoteUser = req.getRemoteUser();
		String url = getUrl(req, session, Tool.HELPER_DONE_URL);
		
		//TODO sometime
		//try	{
			//Evidence e = new ExternalTrustedEvidence(remoteUser);
			//Authentication a = AuthenticationManager.authenticate(e);

			// cleanup session
			//session.removeAttribute(Tool.HELPER_MESSAGE);
			//session.removeAttribute(Tool.HELPER_DONE_URL);
				
			TwoFactorAuthentication twoFactorAuthentication = 
				(TwoFactorAuthentication)ComponentManager.get(TwoFactorAuthentication.class);
			twoFactorAuthentication.markTwoFactor();
			
			res.setStatus(HttpServletResponse.SC_TEMPORARY_REDIRECT);
		/*	
		} catch (AuthenticationMissingException ame) {
			// Don't need to log these normally...
			M_log.debug("User not found: "+ remoteUser);
			throw LoginMissingException.wrap(ame);
			
		} catch (AuthenticationException ex) {
			M_log.warn("Authentication Failed for: "+ remoteUser+ ". "+ ex.getMessage());
			throw LoginException.wrap(ex);
			
		} 
		*/
			
		// mark the session and redirect (for login failure or authentication exception)
		session.removeAttribute(ATTR_TWOFACTOR_CHECKED);
		//System.out.println("TwoFactorLogin.doGet ["+url+"]");
		res.sendRedirect(res.encodeRedirectURL(url));
	}

	/**
	 * Gets a URL from the session, if not found returns the portal URL.
	 * @param session The users HTTP session.
	 * @param sessionAttribute The attribute the URL is stored under.
	 * @return The URL.
	 */
	private String getUrl(HttpServletRequest request, Session session, String sessionAttribute) {
		String url = request.getParameter(redirectParameter);
		if (url == null || url.length() == 0) 
		{
			url = (String) session.getAttribute(sessionAttribute);
			if (url == null || url.length() == 0)
			{
				M_log.debug("No "+ sessionAttribute + " URL, redirecting to portal URL.");
				url = defaultReturnUrl;
			}
		}
		return url;
	}
}
