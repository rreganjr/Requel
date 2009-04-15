package edu.harvard.fas.rregan.uiframework;

import java.io.IOException;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

/**
 * To properly clear the session of an Echo2 application a separate servlet is
 * required. This invalidates the session and redirects to the
 * UIFrameworkServlet to display the login screen.
 * 
 * @author ron
 */
public class UIFrameworkLogoutServlet extends HttpServlet {
	static final long serialVersionUID = 0;

	@Override
	protected void service(HttpServletRequest request, HttpServletResponse response)
			throws ServletException, IOException {
		request.getSession().invalidate();
		// TODO: this presumes that the UIFrameworkServlet is registered at the
		// root of the context
		response.sendRedirect(request.getContextPath());
	}
}
