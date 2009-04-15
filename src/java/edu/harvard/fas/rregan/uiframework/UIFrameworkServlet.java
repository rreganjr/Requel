/*
 * $Id: UIFrameworkServlet.java,v 1.4 2008/02/26 01:30:26 rregan Exp $
 * Copyright (c) 2008 Ron Regan Jr. All Rights Reserved.
 */
package edu.harvard.fas.rregan.uiframework;

import nextapp.echo2.app.ApplicationInstance;
import nextapp.echo2.webcontainer.WebContainerServlet;

import org.springframework.web.context.WebApplicationContext;
import org.springframework.web.context.support.WebApplicationContextUtils;

/**
 * @author ron
 */
public class UIFrameworkServlet extends WebContainerServlet {
	static final long serialVersionUID = 0;

	/**
	 * 
	 */
	public UIFrameworkServlet() {
		super();
	}

	@Override
	public ApplicationInstance newApplicationInstance() {
		WebApplicationContext ctx = WebApplicationContextUtils.getWebApplicationContext(getServletContext());
		return (ApplicationInstance) ctx.getAutowireCapableBeanFactory().createBean(UIFrameworkApp.class);
	}

}
