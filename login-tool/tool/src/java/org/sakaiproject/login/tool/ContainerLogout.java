package org.sakaiproject.login.tool;

import java.io.IOException;

import javax.servlet.ServletConfig;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.sakaiproject.component.cover.ServerConfigurationService;
import org.sakaiproject.event.cover.UsageSessionService;
import org.sakaiproject.tool.api.Session;
import org.sakaiproject.tool.api.Tool;
import org.sakaiproject.tool.cover.SessionManager;

public class ContainerLogout extends HttpServlet {

	/** Our log (commons). */
	private static Log M_log = LogFactory.getLog(ContainerLogin.class);

	/**
	 * Access the Servlet's information display.
	 * 
	 * @return servlet information.
	 */
	public String getServletInfo()
	{
		return "Sakai Container Logout";
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

		M_log.debug("init()");
	}

	/**
	 * Shutdown the servlet.
	 */
	public void destroy()
	{
		M_log.debug("destroy()");

		super.destroy();
	}
	
	protected void doGet(HttpServletRequest req, HttpServletResponse res) throws ServletException, IOException
	{
		// get the session
		Session session = SessionManager.getCurrentSession();
		String returnUrl = ServerConfigurationService.getString("container.logout.url");
		
		// if we end up with nowhere to go, go to the portal
		if (returnUrl == null)
		{
			returnUrl = (String)session.getAttribute(Tool.HELPER_DONE_URL);
			if ("".equals(returnUrl))
			{
				M_log.debug("complete: nowhere set to go, going to portal");
				returnUrl = ServerConfigurationService.getPortalUrl();
			}
		}

		UsageSessionService.logout();
		
		// redirect to the done URL
		// Don't use sendRedirect as it commit's the response.
		res.setHeader("Location", res.encodeRedirectURL(returnUrl));
		res.setStatus(HttpServletResponse.SC_TEMPORARY_REDIRECT);
	}
	
	
}
