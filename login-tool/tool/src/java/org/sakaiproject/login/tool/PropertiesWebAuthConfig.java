package org.sakaiproject.login.tool;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.Collections;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Properties;

import javax.servlet.FilterConfig;
import javax.servlet.ServletContext;
import javax.servlet.ServletException;

import org.sakaiproject.component.cover.ServerConfigurationService;

/**
 * This class allows the location of the webauth filter properties to be loaded from a file defined in the sakai.properties file.
 * Set <code>webauth.properties</code> to the location of the of the config file for WebAuth, if the file isn't absolute it will be 
 * Interpreted relative to the sakai home folder.
 * @author buckett
 *
 */
public class PropertiesWebAuthConfig implements FilterConfig {

	private FilterConfig orginal;
	private HashMap values = new HashMap();
	public final static String CONFIG_LOCATION = "webauth.properties";

	public PropertiesWebAuthConfig(FilterConfig orginal)
			throws ServletException {
		this.orginal = orginal;

		String property = ServerConfigurationService.getString(CONFIG_LOCATION);
		if (property != null) {
			Properties props = new Properties();
			String location = ((new File(property).isAbsolute()) ? property
					: ServerConfigurationService.getSakaiHomePath()
							+ property);
			try {
				props.load(new BufferedInputStream(
						new FileInputStream(location)));
			} catch (IOException ioe) {
				throw new ServletException(
						"There was a problem reading the Webauth configuration file '"
								+ location
								+ "' this is set by the sakai property '"
								+ CONFIG_LOCATION + "'.", ioe);
			}
			values.putAll(props);
		}

	}

	public String getFilterName() {
		return orginal.getFilterName();
	}

	public String getInitParameter(String arg0) {
		return (String) values.get(arg0);
	}

	public Enumeration getInitParameterNames() {
		return Collections.enumeration(values.keySet());
	}

	public ServletContext getServletContext() {
		return orginal.getServletContext();
	}

}
