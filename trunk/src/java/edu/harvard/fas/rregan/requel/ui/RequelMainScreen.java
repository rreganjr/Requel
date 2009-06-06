/*
 * $Id$
 * Copyright 2008, 2009 Ron Regan Jr. All Rights Reserved.
 * This file is part of Requel - the Collaborative Requirments
 * Elicitation System.
 *
 * Requel is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * Requel is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with Requel. If not, see <http://www.gnu.org/licenses/>.
 *
 */
package edu.harvard.fas.rregan.requel.ui;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URL;

import nextapp.echo2.app.Alignment;
import nextapp.echo2.app.Button;
import nextapp.echo2.app.Component;
import nextapp.echo2.app.ContentPane;
import nextapp.echo2.app.Extent;
import nextapp.echo2.app.Label;
import nextapp.echo2.app.Row;
import nextapp.echo2.app.SplitPane;
import nextapp.echo2.app.event.ActionEvent;
import nextapp.echo2.app.event.ActionListener;
import nextapp.echo2.app.filetransfer.DownloadProvider;
import nextapp.echo2.webcontainer.command.BrowserOpenWindowCommand;

import org.apache.commons.io.IOUtils;
import org.apache.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Scope;

import edu.harvard.fas.rregan.requel.user.User;
import net.sf.echopm.PanelContainer;
import net.sf.echopm.login.LogoutEvent;
import net.sf.echopm.navigation.DownloadButton;
import net.sf.echopm.navigation.NavigatorButton;
import net.sf.echopm.navigation.WorkflowDisposition;
import net.sf.echopm.navigation.event.NavigationEvent;
import net.sf.echopm.navigation.event.OpenPanelEvent;
import net.sf.echopm.panel.Panel;
import net.sf.echopm.panel.PanelActionType;
import net.sf.echopm.screen.AbstractScreen;

/**
 * @author ron
 */
@org.springframework.stereotype.Component(RequelMainScreen.screenName)
@Scope("prototype")
public class RequelMainScreen extends AbstractScreen {
	private static final Logger log = Logger.getLogger(RequelMainScreen.class);
	static final long serialVersionUID = 0;

	public static final String screenName = "mainScreen";

	public static final String STYLE_NAME_OUTER_SPLITTER = "RequelMainScreen.OuterSplitter";
	public static final String STYLE_NAME_INNER_SPLITTER = "RequelMainScreen.InnerSplitter";

	public static final String STYLE_NAME_CONTENT_PANEL_CONTAINER = "RequelMainScreen.ContentPanelContainer";

	public static final String STYLE_NAME_BANNER_SPLIT_PANE = "RequelMainScreen.Banner.SplitPane";
	public static final String STYLE_NAME_BANNER_LOGO_PANE = "RequelMainScreen.Banner.LogoPane";
	public static final String STYLE_NAME_BANNER_LOGO_PANE_LAYOUT_ROW = "RequelMainScreen.Banner.LogoPane.Row";

	/**
	 * The name of the style to use for configuring the logo in the banner panel
	 * on the main screen in the Echo2 stylesheet. <br/> Example style: <br/>
	 * 
	 * <pre>
	 * 	&lt;style name=&quot;RequelMainScreen.Logo&quot; 
	 * 			type=&quot;nextapp.echo2.app.Label&quot;&gt;
	 * 		&lt;properties&gt;
	 * 			&lt;property name=&quot;icon&quot; 
	 * 				type=&quot;nextapp.echo2.app.ResourceImageReference&quot;
	 * 				value=&quot;/resources/images/Logo209x100.png&quot; /&gt;
	 * 		&lt;/properties&gt;
	 * 	&lt;/style&gt;
	 * </pre>
	 */
	public static final String STYLE_NAME_LOGO = "RequelMainScreen.Banner.Logo";

	public static final String STYLE_NAME_BANNER_LOGOUT_PANE = "RequelMainScreen.Banner.LogoutPane";
	public static final String STYLE_NAME_BANNER_LOGOUT_PANE_LAYOUT_ROW = "RequelMainScreen.Banner.LogoutPane.Row";

	/**
	 * The name of the property to use for the logout button label from the
	 * specified resource (property) file.
	 */
	public static final String PROP_LABEL_LOGOUT_BUTTON = "LogoutButton.Label";

	/**
	 * The name of the property to use for the logout button label from the
	 * specified resource (property) file.
	 */
	public static final String PROP_LABEL_EDIT_ACCOUNT_BUTTON = "EditAccountButton.Label";

	/**
	 * The name of the property to use for the user guide button label from the
	 * specified resource (property) file.
	 */
	public static final String PROP_LABEL_USER_GUIDE_BUTTON = "UserGuideButton.Label";

	/**
	 * The name of the property to use for the user guide path from the
	 * specified resource (property) file. The path is relative to the class
	 * path in the war file.
	 */
	public static final String PROP_USER_GUIDE_PATH = "UserGuidePath";
	public static final String PROP_USER_GUIDE_PATH_DEFAULT = "../../doc/UserGuide.pdf";

	private final PanelContainer mainNavigationPanelContainer;
	private final PanelContainer mainContentPanelContainer;

	private SplitPane outerSplitter;
	private SplitPane horizontalSplitter;

	/**
	 * TODO: this takes a Set of factories because Spring can only supply a Map
	 * with keyed strings through the AutoWire annotation.
	 * 
	 * @param mainNavigationPanelContainer
	 * @param mainContentPanelContainer
	 */
	@Autowired
	public RequelMainScreen(@Qualifier("mainNavigationPanelContainer")
	PanelContainer mainNavigationPanelContainer, @Qualifier("mainContentPanelContainer")
	PanelContainer mainContentPanelContainer) {
		super(RequelMainScreen.class.getName());
		this.mainContentPanelContainer = mainContentPanelContainer;
		this.mainNavigationPanelContainer = mainNavigationPanelContainer;
	}

	@Override
	public void setup() {
		super.setup();
		outerSplitter = new SplitPane();
		outerSplitter.setStyleName(STYLE_NAME_OUTER_SPLITTER);
		outerSplitter.add(getBanner());
		horizontalSplitter = new SplitPane();
		horizontalSplitter.setStyleName(STYLE_NAME_INNER_SPLITTER);
		mainNavigationPanelContainer.setup();
		horizontalSplitter.add((Component) mainNavigationPanelContainer);
		mainContentPanelContainer.setup();
		mainContentPanelContainer.setStyleName(STYLE_NAME_CONTENT_PANEL_CONTAINER);
		horizontalSplitter.add((Component) mainContentPanelContainer);
		outerSplitter.add(horizontalSplitter);
		add(outerSplitter);
	}

	@Override
	public void dispose() {
		super.dispose();
		removeAll();
	}

	private Component getBanner() {
		SplitPane bannerPane = new SplitPane();
		bannerPane.setStyleName(STYLE_NAME_BANNER_SPLIT_PANE);

		// logo setup
		ContentPane logoPane = new ContentPane();
		logoPane.setStyleName(STYLE_NAME_BANNER_LOGO_PANE);
		Row logoPaneRow = new Row();
		logoPaneRow.setStyleName(STYLE_NAME_BANNER_LOGO_PANE_LAYOUT_ROW);
		Label logo = new Label();
		logo.setStyleName(STYLE_NAME_LOGO);
		logoPaneRow.add(logo);
		logoPane.add(logoPaneRow);
		bannerPane.add(logoPane);

		// logout button setup
		ContentPane logoutPane = new ContentPane();
		logoutPane.setStyleName(STYLE_NAME_BANNER_LOGOUT_PANE);
		// use a row to align the logout button to the far right
		Row logoutPaneRow = new Row();
		logoutPaneRow.setStyleName(STYLE_NAME_BANNER_LOGOUT_PANE_LAYOUT_ROW);

		NavigationEvent openUserEditor = new OpenPanelEvent(this, PanelActionType.Editor, getApp()
				.getUser(), User.class, null, WorkflowDisposition.NewFlow);
		NavigatorButton editAccountButton = new NavigatorButton(
				getResourceBundleHelper(getLocale()).getString(PROP_LABEL_EDIT_ACCOUNT_BUTTON,
						"Edit Account"), getEventDispatcher(), openUserEditor);
		editAccountButton.setStyleName(Panel.STYLE_NAME_DEFAULT);
		logoutPaneRow.add(editAccountButton);

		logoutPaneRow.add(getUserGuideButton());

		NavigatorButton logoutButton = new NavigatorButton(getResourceBundleHelper(getLocale())
				.getString(PROP_LABEL_LOGOUT_BUTTON, "Logout"), getEventDispatcher(),
				new LogoutEvent(this));
		logoutButton.setStyleName(Panel.STYLE_NAME_DEFAULT);
		logoutPaneRow.add(logoutButton);
		logoutPane.add(logoutPaneRow);
		bannerPane.add(logoutPane);

		return bannerPane;
	}

	private Component getDevDocsButton() {
		Button docsButton = new Button("docs");
		docsButton.setStyleName("Plain");
		docsButton.setWidth(new Extent(80));
		docsButton.setAlignment(Alignment.ALIGN_LEFT);
		docsButton.addActionListener(new ActionListener() {
			static final long serialVersionUID = 0;

			public void actionPerformed(ActionEvent e) {
				try {
					/*
					 * open window features width - Defines the width of the new
					 * window (in pixels). height - Defines the height of the
					 * new window (in pixels). directories - Defines whether the
					 * directories (Links) toolbar is shown (yes | no) location -
					 * Defines whether the location toolbar is shown (yes | no).
					 * menubar - Defines whether the menu toolbar (File, Edit
					 * etc.) is shown (yes | no). resizable - Defines whether
					 * the user can resize the new window (yes | no). scrollbars -
					 * Defines whether the new window has scrollbars (yes | no).
					 * status - Defines whether the new window has a status bar
					 * (yes | no). toolbar - Defines whether the general toolbar
					 * (Back, Forward etc.) is shown (yes | no). left - Defines
					 * how many pixels from the left that the new window
					 * appears. top - Defines how many pixels from the top that
					 * the new window appears. fullscreen - Opens popup in IE
					 * fullscreen mode (fullscreen=yes).
					 */
					String features = "height=440,width=350,resizable=yes,status=yes,location=yes,scrollbars=yes";
					getApp().enqueueCommand(
							new BrowserOpenWindowCommand("doc/html/index.html", "Dev Docs",
									features));
				} catch (Exception ex) {
					// TODO: should this be propogated up?
					log.error("Unexpected exception opening browser window: " + ex, ex);
				}
			}
		});
		return docsButton;
	}

	private Component getUserGuideButton() {
		String userGuidePath = getResourceBundleHelper(getLocale()).getString(PROP_USER_GUIDE_PATH,
				PROP_USER_GUIDE_PATH_DEFAULT);
		Button docsButton = new DownloadButton(getResourceBundleHelper(getLocale()).getString(
				PROP_LABEL_USER_GUIDE_BUTTON, "User Guide"), new UserGuideDownloadProvider(
				userGuidePath));
		docsButton.setStyleName("Default");
		return docsButton;
	}

	private static class UserGuideDownloadProvider implements DownloadProvider {
		private final String userGuidePath;
		private File file;

		private UserGuideDownloadProvider(String userGuidePath) {
			this.userGuidePath = userGuidePath;
			try {
				URL url = UserGuideDownloadProvider.class.getClassLoader().getResource(
						userGuidePath);
				if (url != null) {
					file = new File(url.toURI());
				}
			} catch (Exception e) {
				file = null;
			}
		}

		public String getContentType() {
			return "application/pdf";
		}

		public String getFileName() {
			if (file != null) {
				return file.getName();
			}
			return "";
		}

		public int getSize() {
			if (file != null) {
				return (int) file.length();
			}
			return 0;
		}

		public void writeFile(OutputStream outputStream) throws IOException {
			InputStream inputStream = null;
			if (file != null) {
				try {
					inputStream = new FileInputStream(file);
					IOUtils.copy(inputStream, outputStream);
				} finally {
					if (inputStream != null) {
						IOUtils.closeQuietly(inputStream);
					}
				}
			}
		}
	}

}
