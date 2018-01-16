When adding a new spring config file the com.rreganjr.AbstractIntegrationTestCase class
getConfigLocations() method to include the new file and the conf/web.xml contextConfigLocation context
param must also be updated.

When adding a new panel factory in uiProjectConfig, uiUserConfig, uiAnnotationConfig, uiNLPConfig, etc.
also add a reference to the bean in the mainContentPanelManager if the panel should be opened in the
main content container, or in the mainNavigationPanelManager if it is a left hand navigation panel.

