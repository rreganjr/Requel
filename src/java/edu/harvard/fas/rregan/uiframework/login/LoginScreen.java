/*
 * $Id: LoginScreen.java,v 1.12 2008/09/12 00:15:12 rregan Exp $
 * Copyright (c) 2008 Ron Regan Jr. All Rights Reserved.
 */
package edu.harvard.fas.rregan.uiframework.login;

import nextapp.echo2.app.Button;
import nextapp.echo2.app.Column;
import nextapp.echo2.app.Component;
import nextapp.echo2.app.ContentPane;
import nextapp.echo2.app.Grid;
import nextapp.echo2.app.Label;
import nextapp.echo2.app.PasswordField;
import nextapp.echo2.app.SplitPane;
import nextapp.echo2.app.TextField;
import nextapp.echo2.app.event.ActionEvent;
import nextapp.echo2.app.event.ActionListener;

import org.apache.log4j.Logger;
import org.springframework.context.annotation.Scope;

import edu.harvard.fas.rregan.uiframework.AbstractAppAwareActionListener;
import edu.harvard.fas.rregan.uiframework.screen.AbstractScreen;

/**
 * The login screen collects user credentials and when the user clicks the
 * submit button it fires a LoginEvent through the EventDispatcher to the app
 * specific login controller. The login screen has an embedded login failed
 * controller (loginFailedListener) that listens for login failed events and
 * displays the supplied message to the user. The look-and-feel of the screen
 * can be customized through the Echo2 stylesheet and the field and button
 * labels can be customized through resource bundle property files.
 */
@org.springframework.stereotype.Component(LoginScreen.screenName)
@Scope("prototype")
public class LoginScreen extends AbstractScreen {
	private static final Logger log = Logger.getLogger(LoginScreen.class);
	static final long serialVersionUID = 0;

	/**
	 * The name of this screen, used to open this screen via a SetScreenEvent or
	 * sub-type.
	 */
	public static final String screenName = "loginScreen";

	/**
	 * The name of the style to use for configuring the split pane of the login
	 * screen in the Echo2 stylesheet. <br>
	 * Example style: <br>
	 * 
	 * <pre>
	 * &lt;style name=&quot;LoginScreen.SplitPane&quot; 
	 * 			type=&quot;nextapp.echo2.app.SplitPane&quot;&gt;
	 *  &lt;properties&gt;
	 * 	&lt;property name=&quot;layoutData&quot;&gt;
	 * 		&lt;layout-data type=&quot;nextapp.echo2.app.layout.SplitPaneLayoutData&quot;&gt;
	 * 			&lt;properties&gt;
	 * 				&lt;property name=&quot;overflow&quot; 
	 * 					value=&quot;nextapp.echo2.app.layout.SplitPaneLayoutData.OVERFLOW_HIDDEN&quot; /&gt;
	 * 			&lt;/properties&gt;
	 * 		&lt;/layout-data&gt;
	 * 	&lt;/property&gt;
	 * 	&lt;property name=&quot;orientation&quot; 
	 * 		value=&quot;nextapp.echo2.app.SplitPane.ORIENTATION_HORIZONTAL_LEADING_TRAILING&quot; /&gt;
	 * 	&lt;property name=&quot;separatorPosition&quot; value=&quot;235px&quot; /&gt;
	 * &lt;/properties&gt;
	 * &lt;/style&gt;
	 * </pre>
	 */
	public static final String STYLE_NAME_SPLIT_SCREEN = "LoginScreen.SplitPane";

	/**
	 * The name of the style to use for configuring the panel in the left half
	 * of the split pane of the login screen in the Echo2 stylesheet. <br>
	 * Example style: <br>
	 * 
	 * <pre>
	 * 	&lt;style name=&quot;LoginScreen.LeftPane&quot; 
	 * 			type=&quot;nextapp.echo2.app.ContentPane&quot;&gt;
	 * 		 &lt;properties&gt;
	 * 			&lt;property name=&quot;backgroundImage&quot;&gt;
	 * 				&lt;fill-image repeat=&quot;vertical&quot;&gt;
	 * 					&lt;image type=&quot;nextapp.echo2.app.ResourceImageReference&quot;&gt;
	 * 						&lt;resource-image-reference 
	 * 							resource=&quot;/resources/images/backgrounds/LoginPaneBackground2.png&quot; /&gt;
	 * 					&lt;/image&gt;
	 * 				&lt;/fill-image&gt;
	 * 			&lt;/property&gt;
	 * 		 &lt;/properties&gt;
	 * 	   &lt;/style&gt;
	 * </pre>
	 */
	public static final String STYLE_NAME_LEFT_PANE = "LoginScreen.LeftPane";

	/**
	 * The name of the style to use for configuring the column that lays out the
	 * left panel logo of the screen in the Echo2 stylesheet. <br>
	 * Example style: <br>
	 * 
	 * <pre>
	 * 	&lt;style name=&quot;LoginScreen.LeftPane.Column&quot;
	 * 			type=&quot;nextapp.echo2.app.Column&quot;&gt;
	 * 		&lt;properties&gt;
	 * 			&lt;property name=&quot;cellSpacing&quot; value=&quot;10px&quot; /&gt;
	 * 			&lt;property name=&quot;insets&quot; value=&quot;20px 50px 20px 50px&quot; /&gt;
	 * 		&lt;/properties&gt;
	 * 	&lt;/style&gt;
	 * </pre>
	 */
	public static final String STYLE_NAME_LEFT_PANE_COLUMN = "LoginScreen.LeftPane.Column";

	/**
	 * The name of the style to use for configuring the logo in the left pane of
	 * the login screen in the Echo2 stylesheet. <br>
	 * Example style: <br>
	 * 
	 * <pre>
	 * 	&lt;style name=&quot;LoginScreen.Logo&quot; 
	 * 			type=&quot;nextapp.echo2.app.Label&quot;&gt;
	 * 		&lt;properties&gt;
	 * 			&lt;property name=&quot;icon&quot; 
	 * 				type=&quot;nextapp.echo2.app.ResourceImageReference&quot;
	 * 				value=&quot;/resources/images/logo_fembot_torso_blue_cutout.png&quot; /&gt;
	 * 		&lt;/properties&gt;
	 * 	&lt;/style&gt;
	 * </pre>
	 */
	public static final String STYLE_NAME_LEFT_LOGO = "LoginScreen.Left.Logo";

	/**
	 * The name of the style to use for configuring the panel in the left half
	 * of the split pane of the login screen in the Echo2 stylesheet. <br>
	 * Example style: <br>
	 * 
	 * <pre>
	 * 	&lt;style name=&quot;LoginScreen.RightPane&quot; 
	 * 			type=&quot;nextapp.echo2.app.ContentPane&quot;&gt;
	 * 		&lt;properties&gt;
	 * 			&lt;property name=&quot;background&quot; value=&quot;#aaaaff&quot; /&gt;
	 * 		&lt;/properties&gt;
	 * 	&lt;/style&gt;
	 * </pre>
	 */
	public static final String STYLE_NAME_RIGHT_PANE = "LoginScreen.RightPane";

	/**
	 * The name of the style to use for configuring the column that lays out the
	 * logo, title, subtitle, input control grid, and login button in the right
	 * half of the screen in the Echo2 stylesheet. <br>
	 * Example style: <br>
	 * 
	 * <pre>
	 * 	&lt;style name=&quot;LoginScreen.RightPane.Column&quot;
	 * 			type=&quot;nextapp.echo2.app.Column&quot;&gt;
	 * 		&lt;properties&gt;
	 * 			&lt;property name=&quot;cellSpacing&quot; value=&quot;10px&quot; /&gt;
	 * 			&lt;property name=&quot;insets&quot; value=&quot;20px 50px 20px 50px&quot; /&gt;
	 * 		&lt;/properties&gt;
	 * 	&lt;/style&gt;
	 * </pre>
	 */
	public static final String STYLE_NAME_RIGHT_PANE_COLUMN = "LoginScreen.RightPane.Column";

	/**
	 * The name of the style to use for configuring the logo on the login screen
	 * above the title in the Echo2 stylesheet. <br>
	 * Example style: <br>
	 * 
	 * <pre>
	 * 	&lt;style name=&quot;LoginScreen.Logo&quot; 
	 * 			type=&quot;nextapp.echo2.app.Label&quot;&gt;
	 * 		&lt;properties&gt;
	 * 			&lt;property name=&quot;icon&quot; 
	 * 				type=&quot;nextapp.echo2.app.ResourceImageReference&quot;
	 * 				value=&quot;/resources/images/Logo209x100.png&quot; /&gt;
	 * 		&lt;/properties&gt;
	 * 	&lt;/style&gt;
	 * </pre>
	 */
	public static final String STYLE_NAME_RIGHT_LOGO = "LoginScreen.Right.Logo";

	/**
	 * The name of the style to use for configuring the title on the login
	 * screen below the logo in the Echo2 stylesheet. <br>
	 * Example style: <br>
	 * 
	 * <pre>
	 * 	&lt;style name=&quot;LoginScreen.Title&quot;
	 * 			type=&quot;nextapp.echo2.app.Label&quot;&gt;
	 * 		&lt;properties&gt;
	 * 			&lt;property name=&quot;font&quot;&gt;
	 * 				&lt;font bold=&quot;true&quot;
	 * 				size=&quot;22pt&quot; typeface=&quot;Arial&quot; /&gt;
	 * 			&lt;/property&gt;
	 * 		&lt;/properties&gt;
	 * 	&lt;/style&gt;
	 * </pre>
	 */
	public static final String STYLE_NAME_TITLE = "LoginScreen.Title";

	/**
	 * The name of the style to use for configuring the subtitle on the login
	 * screen below the title in the Echo2 stylesheet. <br>
	 * Example style: <br>
	 * 
	 * <pre>
	 * 	&lt;style name=&quot;LoginScreen.Subtitle&quot;
	 *  		type=&quot;nextapp.echo2.app.Label&quot;&gt;
	 * 		&lt;properties&gt;
	 * 			&lt;property name=&quot;font&quot;&gt;
	 * 				&lt;font bold=&quot;true&quot;
	 * 					size=&quot;12pt&quot; typeface=&quot;Arial&quot; /&gt;
	 * 			&lt;/property&gt;
	 * 		&lt;/properties&gt;
	 * 	&lt;/style&gt;
	 * </pre>
	 */
	public static final String STYLE_NAME_SUB_TITLE = "LoginScreen.Subtitle";

	/**
	 * The name of the style to use for configuring the layout grid on the login
	 * screen containing the input fields and labels in the Echo2 stylesheet.
	 * <br>
	 * Example style: <br>
	 * 
	 * <pre>
	 * 	&lt;style name=&quot;LoginScreen.LayoutGrid&quot;
	 * 			type=&quot;nextapp.echo2.app.Grid&quot;&gt;
	 * 		&lt;properties&gt;
	 * 			&lt;property name=&quot;insets&quot; value=&quot;10px 10px&quot; /&gt;
	 * 			&lt;property name=&quot;border&quot;&gt;
	 * 				&lt;border color=&quot;#bbbbff&quot;
	 * 				size=&quot;2px&quot; style=&quot;groove&quot; /&gt;
	 * 			&lt;/property&gt;
	 * 		&lt;/properties&gt;
	 * 	&lt;/style&gt;
	 * </pre>
	 */
	public static final String STYLE_NAME_LAYOUT_GRID = "LoginScreen.LayoutGrid";

	/**
	 * The name of the style to use for configuring the username and password
	 * input field labels in the Echo2 stylesheet. <br>
	 * Example style: <br>
	 * 
	 * <pre>
	 * 	&lt;style name=&quot;LoginScreen.Label&quot; 
	 * 			base-name=&quot;Default&quot; 
	 * 			type=&quot;nextapp.echo2.app.Label&quot;&gt;
	 * 		&lt;properties&gt;
	 * 			&lt;property name=&quot;font&quot;&gt;
	 * 				&lt;font bold=&quot;false&quot; 
	 * 					size=&quot;10pt&quot; typeface=&quot;Arial&quot; /&gt;
	 * 			&lt;/property&gt;
	 * 		&lt;/properties&gt;
	 * 	&lt;/style&gt;
	 * </pre>
	 */
	public static final String STYLE_NAME_LABEL = "LoginScreen.Label";

	/**
	 * The name of the style to use for configuring the username input field in
	 * the Echo2 stylesheet. <br>
	 * Example style: <br>
	 * 
	 * <pre>
	 * 	&lt;style name=&quot;LoginScreen.Username.InputField&quot;
	 * 			base-name=&quot;Default&quot;
	 * 			type=&quot;nextapp.echo2.app.text.TextComponent&quot;&gt;
	 * 		&lt;properties&gt;
	 * 			&lt;property name=&quot;width&quot; value=&quot;300px&quot; /&gt;
	 * 			&lt;property name=&quot;font&quot;&gt;
	 * 				&lt;font bold=&quot;false&quot;
	 * 					size=&quot;10pt&quot; typeface=&quot;Arial&quot; /&gt;
	 * 			&lt;/property&gt;
	 * 		&lt;/properties&gt;
	 * 	&lt;/style&gt;
	 * </pre>
	 */
	public static final String STYLE_NAME_USERNAME_INPUT = "LoginScreen.Username.InputField";

	/**
	 * The name of the style to use for configuring the username input field in
	 * the Echo2 stylesheet. <br>
	 * See the username input for an example. <br>
	 */
	public static final String STYLE_NAME_PASSWORD_INPUT = "LoginScreen.Password.InputField";

	/**
	 * The name of the style to use for configuring the login button in the
	 * Echo2 stylesheet. <br>
	 * See the Default style for AbstractButton as an example. <br>
	 */
	public static final String STYLE_NAME_LOGIN_BUTTON = "LoginScreen.LoginButton";

	/**
	 * The name of the style to use for configuring the login button in the
	 * Echo2 stylesheet. <br>
	 * See the ValidationLabel style for an example. <br>
	 */
	public static final String STYLE_NAME_VALIDATION = "ValidationLabel";

	/**
	 * The name of the property to use for the title from the specified resource
	 * (property) file.
	 */
	public static final String PROP_NAME_TITLE = "Screen.Title";

	/**
	 * The name of the property to use for the subtitle from the specified
	 * resource (property) file.
	 */
	public static final String PROP_NAME_SUB_TITLE = "Screen.Subtitle";

	/**
	 * The name of the property to use for the username field label from the
	 * specified resource (property) file.
	 */
	public static final String PROP_NAME_USERNAME_LABEL = "Username.Label";

	/**
	 * The name of the property to use for the password field label from the
	 * specified resource (property) file.
	 */
	public static final String PROP_NAME_PASSWORD_LABEL = "Password.Label";

	/**
	 * The name of the property to use for the login button label from the
	 * specified resource (property) file.
	 */
	public static final String PROP_NAME_LOGIN_BUTTON_LABEL = "LoginButton.Label";

	// the ActionEvent command string for the local event fired from the
	// password field when return is hit, or from the login button. This
	// event is handle locally by the SubmitLoginListener
	private static final String ACTION_SUBMIT_LOGIN = LoginScreen.class.getName() + ".submitLogin";

	private TextField usernameField;
	private PasswordField passwordField;
	private Label messages;

	private final SubmitLoginListener submitLoginListener;
	private final LoginFailedListener loginFailedListener;

	/**
	 * Create a LoginScreen using LoginScreen.properties in the same classpath
	 * directory for the resource bundle.
	 */
	public LoginScreen() {
		this(LoginScreen.class.getName());
	}

	/**
	 * Create a LoginScreen using the supplied name for the resource bundle.
	 * 
	 * @param resourceBundleName -
	 *            The name of the resource bundle including the path.
	 */
	public LoginScreen(String resourceBundleName) {
		super(resourceBundleName);
		submitLoginListener = new SubmitLoginListener(this);
		loginFailedListener = new LoginFailedListener(this);

		log.debug("new login screen created, using resources from: " + resourceBundleName);
	}

	@Override
	public void setup() {
		super.setup();
		SplitPane splitPane1 = new SplitPane();
		splitPane1.setStyleName(STYLE_NAME_SPLIT_SCREEN);
		splitPane1.add(createLeftPanel());
		splitPane1.add(createRightPanel());
		add(splitPane1);
		getEventDispatcher().addEventTypeActionListener(LoginFailedEvent.class, loginFailedListener);
		resetState();
	}

	@Override
	public void dispose() {
		getEventDispatcher().removeEventTypeActionListener(LoginFailedEvent.class,
				loginFailedListener);
		super.dispose();
	}

	private Component createLeftPanel() {
		ContentPane leftPanel = new ContentPane();
		leftPanel.setStyleName(STYLE_NAME_LEFT_PANE);

		Column column = new Column();
		column.setStyleName(STYLE_NAME_LEFT_PANE_COLUMN);
		leftPanel.add(column);

		Label logo = new Label();
		logo.setStyleName(STYLE_NAME_LEFT_LOGO);
		column.add(logo);

		return leftPanel;
	}

	private Component createRightPanel() {
		ContentPane loginPanel = new ContentPane();
		loginPanel.setStyleName(STYLE_NAME_RIGHT_PANE);

		Column column = new Column();
		column.setStyleName(STYLE_NAME_RIGHT_PANE_COLUMN);
		loginPanel.add(column);

		Label logo = new Label();
		logo.setStyleName(STYLE_NAME_RIGHT_LOGO);
		column.add(logo);

		Label title = new Label(getResourceBundleHelper(getLocale()).getString(PROP_NAME_TITLE,
				"Login"));
		title.setStyleName(STYLE_NAME_TITLE);
		column.add(title);

		Label subTitle = new Label(getResourceBundleHelper(getLocale()).getString(
				PROP_NAME_SUB_TITLE, "Login"));
		subTitle.setStyleName(STYLE_NAME_SUB_TITLE);
		column.add(subTitle);

		Grid grid1 = new Grid(2);
		grid1.setStyleName(STYLE_NAME_LAYOUT_GRID);

		Label usernameLabel = new Label(getResourceBundleHelper(getLocale()).getString(
				PROP_NAME_USERNAME_LABEL, "Username:"));
		usernameLabel.setStyleName(STYLE_NAME_LABEL);
		grid1.add(usernameLabel);

		usernameField = new TextField();
		usernameField.setStyleName(STYLE_NAME_USERNAME_INPUT);
		usernameField.addActionListener(new ActionListener() {
			private static final long serialVersionUID = 0;

			public void actionPerformed(ActionEvent e) {
				getApp().setFocusedComponent(passwordField);
			}
		});
		grid1.add(usernameField);

		Label passwordLabel = new Label(getResourceBundleHelper(getLocale()).getString(
				PROP_NAME_PASSWORD_LABEL, "Password:"));
		passwordLabel.setStyleName(STYLE_NAME_LABEL);
		grid1.add(passwordLabel);

		passwordField = new PasswordField();
		passwordField.setStyleName(STYLE_NAME_PASSWORD_INPUT);
		passwordField.addActionListener(submitLoginListener);
		passwordField.setActionCommand(ACTION_SUBMIT_LOGIN);
		grid1.add(passwordField);

		column.add(grid1);

		Button loginButton = new Button(getResourceBundleHelper(getLocale()).getString(
				PROP_NAME_LOGIN_BUTTON_LABEL, "Login"));
		loginButton.setStyleName(STYLE_NAME_LOGIN_BUTTON);
		loginButton.setActionCommand(ACTION_SUBMIT_LOGIN);
		loginButton.addActionListener(submitLoginListener);
		column.add(loginButton);

		messages = new Label();
		messages.setStyleName(STYLE_NAME_VALIDATION);
		column.add(messages);
		return loginPanel;
	}

	/**
	 * 
	 */
	public void resetState() {
		usernameField.setText("");
		passwordField.setText("");
		messages.setText("");
		getApp().setFocusedComponent(usernameField);
	}

	private static class SubmitLoginListener extends AbstractAppAwareActionListener {
		static final long serialVersionUID = 0;

		private final LoginScreen screen;

		protected SubmitLoginListener(LoginScreen screen) {
			super(screen.getApp());
			this.screen = screen;
		}

		public void actionPerformed(ActionEvent event) {
			log.debug("event: " + event);
			if (ACTION_SUBMIT_LOGIN.equals(event.getActionCommand())) {
				String username = screen.usernameField.getText();
				String password = screen.passwordField.getText();
				try {
					screen.getApp().getEventDispatcher().dispatchEvent(
							new LoginEvent(screen, username, password));
					screen.passwordField.setText("");
				} catch (Exception e) {
					log.error("exception executing LoginEvent: " + e, e);
					screen.messages
							.setText("A system problem caused the login to fail. If this continues please contact the administrator.");
				}
			}
		}
	}

	private static class LoginFailedListener extends AbstractAppAwareActionListener {
		static final long serialVersionUID = 0;

		private final LoginScreen screen;

		protected LoginFailedListener(LoginScreen screen) {
			super(screen.getApp());
			this.screen = screen;
		}

		public void actionPerformed(ActionEvent e) {
			log.debug("event: " + e);
			if (e instanceof LoginFailedEvent) {
				screen.messages.setText(((LoginFailedEvent) e).getMessage());
			}
		}
	}

}
