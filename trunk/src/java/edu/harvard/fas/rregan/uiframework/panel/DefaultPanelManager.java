/*
 * $Id: DefaultPanelManager.java,v 1.14 2008/12/17 02:00:42 rregan Exp $
 * Copyright (c) 2008 Ron Regan Jr. All Rights Reserved.
 */
package edu.harvard.fas.rregan.uiframework.panel;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import nextapp.echo2.app.event.ActionEvent;

import org.apache.log4j.Logger;

import edu.harvard.fas.rregan.uiframework.PanelContainer;
import edu.harvard.fas.rregan.uiframework.navigation.event.ClosePanelEvent;
import edu.harvard.fas.rregan.uiframework.navigation.event.EventDispatcher;
import edu.harvard.fas.rregan.uiframework.navigation.event.NavigationEvent;
import edu.harvard.fas.rregan.uiframework.navigation.event.OpenPanelEvent;

/**
 * The DefaultPanelManager
 * 
 * @author ron
 */
public class DefaultPanelManager implements PanelManager {
	static final long serialVersionUID = 0L;
	private static final Logger log = Logger.getLogger(DefaultPanelManager.class);

	private final Map<PanelRegistryKey, PanelDescriptor> panelDescriptors = new HashMap<PanelRegistryKey, PanelDescriptor>();
	private final EventDispatcher eventDispatcher;

	private PanelContainer panelContainer;

	/**
	 * Create a PanelManager without the managed PanelContainer. The
	 * PanelContainer must be set before the PanelManager can handle events
	 * through the actionPerformed method.<br/> The manager accepts a Set of
	 * PanelDescriptors that may be actual Panel instances or PanelFactory
	 * instances. Supplied Panels will be reused each time an Event causes the
	 * Panel to be displayed. A PanelFactory may create a new Panel for each
	 * request or use a pool of panels.<br/>
	 * 
	 * @param eventDispatcher -
	 *            the dispatcher that will send open panel events to the
	 *            PanelContainer.
	 * @param panelDescriptors -
	 *            a set of Panels and/or PanelFactorys that this manager will
	 *            use to populate its assigned panel container.
	 */
	public DefaultPanelManager(EventDispatcher eventDispatcher,
			Set<PanelDescriptor> panelDescriptors) {
		this.eventDispatcher = eventDispatcher;
		register(panelDescriptors, eventDispatcher);
	}

	protected EventDispatcher getEventDispatcher() {
		return eventDispatcher;
	}

	/**
	 * Assign the Panel container that this manager will apply events to.
	 * 
	 * @param panelContainer
	 */
	public void setPanelContainer(PanelContainer panelContainer) {
		if ((panelContainer == null) && (this.panelContainer != null)) {
			this.panelContainer.dispose();
		}
		this.panelContainer = panelContainer;
	}

	protected PanelContainer getPanelContainer() {
		return panelContainer;
	}

	/**
	 * This is the method that gets called by the EventDispatcher when the
	 * dispatcher gets an OpenPanelEvent or ClosePanelEvent for one of the
	 * PanelDescriptors registered with this manager.
	 * 
	 * @see nextapp.echo2.app.event.ActionListener#actionPerformed(nextapp.echo2.app.event.ActionEvent)
	 */
	public void actionPerformed(ActionEvent e) {
		if (e instanceof NavigationEvent) {
			if (e instanceof OpenPanelEvent) {
				OpenPanelEvent event = (OpenPanelEvent) e;
				Panel panel = getPanelForEvent(event);
				getEventDispatcher().addPanelInstanceEventActionListener(panel, this);
				getPanelContainer().displayPanel(panel, event.getWorkflowDisposition());
			} else if (e instanceof ClosePanelEvent) {
				ClosePanelEvent event = (ClosePanelEvent) e;
				Panel panelToClose = event.getPanelToClose();
				if (panelToClose != null) {
					getPanelContainer().undisplayPanel(panelToClose);
					unregister(panelToClose, getEventDispatcher(),
							panelToClose.getTargetObject());
				}
			}
		}
	}

	public void register(PanelDescriptor panelDescriptor, EventDispatcher eventDispatcher) {
		register(panelDescriptor, eventDispatcher, null);
	}

	private void register(PanelDescriptor panelDescriptor, EventDispatcher eventDispatcher,
			Object targetObject) {
		PanelRegistryKey panelRegistryKey = new PanelRegistryKey(panelDescriptor, targetObject);
		PanelDescriptor existingPanelDescriptor = panelDescriptors.get(panelRegistryKey);
		if (existingPanelDescriptor != null) {
			eventDispatcher.removeOpenPanelEventActionListener(panelDescriptor, this);
			log.warn("The Panel or PanelFactory " + panelDescriptor + " is replacing "
					+ existingPanelDescriptor);
		}
		eventDispatcher.addOpenPanelEventActionListener(panelDescriptor, this);
		panelDescriptors.put(panelRegistryKey, panelDescriptor);
	}

	private void unregister(PanelDescriptor panelDescriptor, EventDispatcher eventDispatcher,
			Object targetObject) {

		// only unregister panels for specific targets, otherwise the specific
		// type of panel may never be opened again.
		if ((targetObject != null) && (panelDescriptor instanceof Panel)) {
			PanelRegistryKey panelRegistryKey = new PanelRegistryKey(panelDescriptor, targetObject);
			if (panelDescriptors.containsKey(panelRegistryKey)) {
				panelDescriptors.remove(panelRegistryKey);
				eventDispatcher.removePanelInstanceEventActionListener((Panel) panelDescriptor,
						this);
			}
		}
	}

	public void register(Set<PanelDescriptor> panelDescriptors, EventDispatcher eventDispatcher) {
		for (PanelDescriptor panelDescriptor : panelDescriptors) {
			register(panelDescriptor, eventDispatcher);
		}
	}

	/**
	 * @return all the PanelDescriptors registered with this manager
	 */
	protected Collection<PanelDescriptor> getRegisteredPanelDescriptors() {
		return panelDescriptors.values();
	}

	protected Panel getPanelForEvent(NavigationEvent e) {
		Panel panel = null;
		if (e instanceof OpenPanelEvent) {
			OpenPanelEvent openEvent = (OpenPanelEvent) e;
			if (openEvent.getPanel() != null) {
				panel = openEvent.getPanel();
			} else {
				Class<?> targetType = openEvent.getTargetType();
				if (targetType != null) {
					while (targetType != null) {
						panel = getPanel(openEvent.getPanelActionType(), targetType, openEvent
								.getPanelName(), openEvent.getTargetObject(), openEvent.getSource());
						if (panel != null) {
							break;
						}
						if (targetType.getInterfaces() != null) {
							for (Class<?> face : targetType.getInterfaces()) {
								panel = getPanel(openEvent.getPanelActionType(), face, openEvent
										.getPanelName(), openEvent.getTargetObject(), openEvent.getSource());
								if (panel != null) {
									break;
								}
							}
						}
						if (panel != null) {
							break;
						}
						targetType = targetType.getSuperclass();
					}
				} else {
					// panel with no content type
					panel = getPanel(openEvent.getPanelActionType(), null, openEvent
							.getPanelName(), openEvent.getTargetObject(), openEvent.getSource());					
				}
			}
		} else if (e instanceof ClosePanelEvent) {
			ClosePanelEvent event = (ClosePanelEvent) e;
			panel = event.getPanelToClose();
		}
		return panel;
	}

	/**
	 * Get a panel responsible for handling events based on the supplied
	 * criteria.<br>
	 * The returned panel will be initialized via its setup() method, and have
	 * its target object set.<br>
	 * If there is an exact match based on the supplied parameters then that
	 * panel will be returned. If there isn't an exact match the following order
	 * for matching will be used unless one of the parameters is null:<br>
	 * <ul>
	 * <li>actionType, contentType, panelName, targetObject</li>
	 * <li>actionType, contentType, panelName</li>
	 * <li>contentType, panelName</li>
	 * <li>panelName</li>
	 * <li>actionType, contentType</li>
	 * </ul>
	 * <br>
	 * In each case where the contentType is not null, the supertype and
	 * interfaces of the type will be substituted until a match is found or the
	 * Object class is reached.
	 * 
	 * @param actionType -
	 *            The type of action from the PanelActionType enum. if this is
	 *            null PanelActionType.Unspecified will be substituted.
	 * @param contentType -
	 *            The type of content that will be supplied to the panel. If
	 *            there isn't a panel for the exact type, a panel for the
	 *            closest supertype or interfaces will be substituted.
	 * @param panelName -
	 *            the name of the panel
	 * @param targetObject -
	 *            the object that the supplied panel will use to initialize its
	 *            state
	 * @param destinationObject -
	 *            The destination to set on events fired from the panel
	 * @return
	 */
	protected Panel getPanel(PanelActionType actionType, Class<?> contentType, String panelName,
			Object targetObject, Object destinationObject) {
		if (actionType == null) {
			actionType = PanelActionType.Unspecified;
		}
		if (panelName != null) {
			panelName = panelName.trim();
			if ("".equals(panelName)) {
				panelName = null;
			}
		}

		// first try to find an existing panel with the targetObject
		PanelDescriptor panelDescriptor = getPanelDescriptor(actionType, contentType, panelName,
				targetObject);
		// then try for a panel or panel factory without the targetObject
		if (panelDescriptor == null) {
			panelDescriptor = getPanelDescriptor(actionType, contentType, panelName, null);
		}

		Panel panel = null;
		if (panelDescriptor != null) {
			if (panelDescriptor instanceof Panel) {
				panel = (Panel) panelDescriptor;
			} else {
				PanelFactory factory = (PanelFactory) panelDescriptor;
				try {
					panel = factory.newInstance();
					// register the new panel for handling events for the
					// target object instead of creating a new panel.
					if (targetObject != null) {
						register(panel, eventDispatcher, targetObject);
					}
				} catch (Exception e) {
					log.error("failed to create new panel with noargs constructor and factory '"
							+ factory + "': " + e, e);
				}
			}
		}
		if (panel != null) {
			if (destinationObject != null) {
				if (panel.getDestinationObject() != null && !destinationObject.equals(panel.getDestinationObject())) {
					// the panel is already configured 
					log.warn("the panel " + panel + " is already configured to send events to destination " + panel.getDestinationObject() + ", not changing destination to " + destinationObject);
				} else {
					panel.setDestinationObject(destinationObject);
				}
			}
			panel.setTargetObject(targetObject);
			if (!panel.isInitialized()) {
				panel.setup();
			}
		}
		return panel;
	}

	/**
	 * NOTE: this is protected for testing purposes only, it should not be
	 * called directly.<br>
	 * get the PanelDescriptor based on the precedence rules
	 */
	protected PanelDescriptor getPanelDescriptor(PanelActionType actionType, Class<?> contentType,
			String panelName, Object targetObject) {
		PanelDescriptor panelDescriptor = null;

		if (panelName != null) {
			// actionType, contentType, panelName
			panelDescriptor = getPanelDescriptorForContentType(actionType, contentType, panelName,
					targetObject);
			// contentType, panelName
			if (panelDescriptor == null) {
				panelDescriptor = getPanelDescriptorForContentType(PanelActionType.Unspecified,
						contentType, panelName, targetObject);
			}
			// actionType, panelName
			if (panelDescriptor == null) {
				panelDescriptor = panelDescriptors.get(new PanelRegistryKey(
						actionType, null, panelName, targetObject));
			}

			// panelName
			if (panelDescriptor == null) {
				panelDescriptor = panelDescriptors.get(new PanelRegistryKey(
						PanelActionType.Unspecified, null, panelName, targetObject));
			}
		}
		if ((panelDescriptor == null) && !PanelActionType.Unspecified.equals(actionType)) {
			// actionType, contentType
			panelDescriptor = getPanelDescriptorForContentType(actionType, contentType, panelName,
					targetObject);
		}
		return panelDescriptor;
	}

	/**
	 * NOTE: this is protected for testing purposes only, it should not be
	 * called directly.<br>
	 * if contentType isn't null search for a PanelDescriptor by the type,
	 * supertypes and interfaces.
	 */
	protected PanelDescriptor getPanelDescriptorForContentType(PanelActionType actionType,
			Class<?> contentType, String panelName, Object targetObject) {
		PanelDescriptor panelDescriptor = null;
		if (contentType != null) {
			while (contentType != null) {
				log.debug(panelDescriptors);
				panelDescriptor = panelDescriptors.get(new PanelRegistryKey(actionType,
						contentType, panelName, targetObject));
				if (panelDescriptor != null) {
					break;
				}
				contentType = contentType.getSuperclass();
			}
		}
		return panelDescriptor;
	}

	public void dispose() {
		for (PanelDescriptor panelDescriptor : getRegisteredPanelDescriptors()) {
			panelDescriptor.dispose();
		}
	}

	@Override
	public String toString() {
		return getClass().getSimpleName();
	}

	private static class PanelRegistryKey {
		private final PanelActionType actionType;
		private final Class<?> contentType;
		private final String panelName;
		private final Object targetObject;

		/**
		 * Create a key for a given panel
		 * 
		 * @param panelDescriptor
		 */
		protected PanelRegistryKey(PanelDescriptor panelDescriptor, Object targetObject) {
			this(panelDescriptor.getSupportedActionType(), panelDescriptor
					.getSupportedContentType(), panelDescriptor.getPanelName(), targetObject);
		}

		/**
		 * Create a key for the given constraints
		 * 
		 * @param actionType
		 * @param contentType
		 * @param name
		 */
		protected PanelRegistryKey(PanelActionType actionType, Class<?> contentType,
				String panelName, Object targetObject) {
			this.actionType = actionType;
			this.contentType = contentType;
			this.panelName = panelName;
			this.targetObject = targetObject;
		}

		@Override
		public int hashCode() {
			final int prime = 31;
			int result = 1;
			result = prime * result + ((actionType == null) ? 0 : actionType.hashCode());
			result = prime * result + ((contentType == null) ? 0 : contentType.hashCode());
			result = prime * result + ((panelName == null) ? 0 : panelName.hashCode());
			result = prime * result + ((targetObject == null) ? 0 : targetObject.hashCode());
			return result;
		}

		@Override
		public boolean equals(Object obj) {
			if (this == obj) {
				return true;
			}
			if (obj == null) {
				return false;
			}
			if (getClass() != obj.getClass()) {
				return false;
			}
			final PanelRegistryKey other = (PanelRegistryKey) obj;
			if (actionType == null) {
				if (other.actionType != null) {
					return false;
				}
			} else if (!actionType.equals(other.actionType)) {
				return false;
			}
			if (contentType == null) {
				if (other.contentType != null) {
					return false;
				}
			} else if (!contentType.equals(other.contentType)) {
				return false;
			}
			if (panelName == null) {
				if (other.panelName != null) {
					return false;
				}
			} else if (!panelName.equals(other.panelName)) {
				return false;
			}
			if (targetObject == null) {
				if (other.targetObject != null) {
					return false;
				}
			} else if (!targetObject.equals(other.targetObject)) {
				return false;
			}
			return true;
		}

		@Override
		public String toString() {
			return getClass().getSimpleName() + "[actionType = " + actionType + ", contentType = "
					+ contentType + ", panelName = " + panelName + ", targetObject = "
					+ targetObject + "]";
		}
	}
}