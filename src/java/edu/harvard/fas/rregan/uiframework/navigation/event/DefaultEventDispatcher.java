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
package edu.harvard.fas.rregan.uiframework.navigation.event;

import java.util.Collection;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;

import nextapp.echo2.app.event.ActionEvent;
import nextapp.echo2.app.event.ActionListener;

import org.apache.log4j.Logger;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

import edu.harvard.fas.rregan.uiframework.panel.Panel;
import edu.harvard.fas.rregan.uiframework.panel.PanelActionType;
import edu.harvard.fas.rregan.uiframework.panel.PanelDescriptor;
import edu.harvard.fas.rregan.uiframework.screen.Screen;

/**
 * The default implementation of the EventDispatcher. It supports a single
 * instance of a UIFrameworkApp for a single user, no cross user events.
 * 
 * @author ron
 */
@Component("eventDispatcher")
@Scope("session")
public class DefaultEventDispatcher implements EventDispatcher {
	private static final Logger log = Logger.getLogger(DefaultEventDispatcher.class);
	static final long serialVersionUID = 0L;

	// TODO: should the entries use a Weak or Soft reference so that if a
	// component is deleted from the UI it won't stick around because it
	// is regestered for events?
	private final Map<PanelListenerKey, Set<ActionListener>> eventTypeActionListenerSets = new HashMap<PanelListenerKey, Set<ActionListener>>();

	/**
	 * 
	 */
	public DefaultEventDispatcher() {
		log.debug("new DefaultEventDispatcher");
	}

	/**
	 * dispatches an event to all appropriate listeners based on the event's
	 * source, type and action command. This is a simple wrapper for
	 * actionPerformed()
	 * 
	 * @param event -
	 *            an ActionEvent to dispatch
	 */
	public void dispatchEvent(ActionEvent event) {
		actionPerformed(event);
	}

	/**
	 * The EventDispatcher registers itself as a Listener for all objects
	 * registered as event sources so that it can forward events to listeners
	 * indirectly.
	 * 
	 * @throws EventDispatcherMultiException -
	 *             if any listener actionPerformed() throws an exception. It
	 *             contains a map keyed by listneners to the exception thrown.
	 *             All listeners will be called no matter which listeners throw
	 *             exceptions and this exception is thrown at the end.
	 */
	public void actionPerformed(ActionEvent event) {
		log.debug("event = " + event);

		if (event != null) {
			Set<ActionListener> distinctListeners = getDistinctListenersForEvent(event);

			if (distinctListeners.size() > 0) {
				// all listeners are notified even if some throw exceptions, the
				// thrown exceptions are recorded with the listener that threw
				// it and reported at the end.
				Map<ActionListener, Exception> listenerExceptions = new HashMap<ActionListener, Exception>();

				for (ActionListener listener : distinctListeners) {
					try {
						log.debug("sending event " + event + " to listener " + listener);
						listener.actionPerformed(event);
					} catch (Exception e) {
						listenerExceptions.put(listener, e);
						log.warn("listener " + listener
								+ " threw an exception from actionPerformed() for event " + event,
								e);
					}
				}
				if (!listenerExceptions.isEmpty()) {
					// TODO: maybe this shouldn't throw an exception, but fire
					// an event with a message to be displayed in a window.
					throw new EventDispatcherMultiException(listenerExceptions);
				}
			} else {
				log.warn("no listeners match for event: " + event);
			}
		} else {
			log.warn("null event supplied.");
		}
	}

	/**
	 * NOTE: this is protected for testing purposes, it should not be called
	 * directly.<br>
	 * 
	 * @param event
	 */
	protected Set<ActionListener> getDistinctListenersForEvent(ActionEvent event) {
		// Use a set to collect all the distinct listeners so that if a
		// listener is listening based on different criteria, it only
		// recieves the event once. Screen listeners are notified first
		// followed by Panels and then all other listeners
		Set<ActionListener> distinctListeners = new TreeSet<ActionListener>(
				new ActionListenerComparator());

		if (event != null) {
			// a listener registered for a super type of the event type should
			// be notified of the event.

			if (event instanceof OpenPanelEvent) {
				// OpenPanelEvent events can be listened to for specialized
				// criteria see PanelListenerKey()
				OpenPanelEvent openEvent = (OpenPanelEvent) event;
				if (openEvent.getPanel() != null) {
					// if a panel is specified, collect listeners for that panel
					// by event type and super types.
					Class<?> eventType = event.getClass();
					do {
						distinctListeners.addAll(getEventListenersByKey(new PanelListenerKey(
								Class.class.cast(eventType), (OpenPanelEvent) event)));
						eventType = eventType.getSuperclass();
					} while (ActionEvent.class.isAssignableFrom(eventType));

				} else {
					// look up the panel based on the event, action and content
					// types, including event super types and content super
					// types and interfaces
					addListenersByActionAndTargetType(distinctListeners, openEvent);
				}
			} else if (event instanceof ClosePanelEvent) {
				// ClosePanelEvent events can be listened to for specific panels
				// see PanelListenerKey()
				Class<?> eventType = event.getClass();
				do {
					distinctListeners.addAll(getEventListenersByKey(new PanelListenerKey(
							Class.class.cast(eventType), (ClosePanelEvent) event)));
					eventType = eventType.getSuperclass();
				} while (ActionEvent.class.isAssignableFrom(eventType));
			}

			Class<?> eventType = event.getClass();
			do {
				distinctListeners.addAll(getEventListenersByKey(new PanelListenerKey(Class.class
						.cast(eventType))));
				if (NavigationEvent.class.isAssignableFrom(eventType)) {
					distinctListeners.addAll(getEventListenersByKey(new PanelListenerKey(
							Class.class.cast(eventType), ((NavigationEvent) event)
									.getDestinationObject())));
				}
				eventType = eventType.getSuperclass();
			} while (ActionEvent.class.isAssignableFrom(eventType));
		}
		return distinctListeners;
	}

	protected void addListenersByActionAndTargetType(Set<ActionListener> distinctListeners,
			OpenPanelEvent openEvent) {
		Class<?> targetType = openEvent.getTargetType();
		Class<?> eventType = openEvent.getClass();
		if (targetType != null) {
			do {
				while (targetType != null) {
					distinctListeners.addAll(getEventListenersByKey(new PanelListenerKey(
							Class.class.cast(eventType), openEvent.getPanelActionType(),
							targetType, openEvent.getPanelName(), openEvent.getPanel(), null)));

					if (targetType.getInterfaces() != null) {
						for (Class<?> face : targetType.getInterfaces()) {
							distinctListeners.addAll(getEventListenersByKey(new PanelListenerKey(
									Class.class.cast(eventType), openEvent.getPanelActionType(),
									face, openEvent.getPanelName(), openEvent.getPanel(), null)));
						}
					}
					targetType = targetType.getSuperclass();
				}
				eventType = eventType.getSuperclass();
			} while (OpenPanelEvent.class.isAssignableFrom(eventType));
		} else {
			// find a panel without a target type
			distinctListeners.addAll(getEventListenersByKey(new PanelListenerKey(Class.class
					.cast(eventType), openEvent.getPanelActionType(), null, openEvent
					.getPanelName(), openEvent.getPanel(), null)));

		}
	}

	/**
	 * Get all the ActionListeners registered to receive specific ActionEvent
	 * types or based on PanelListenerKey (for listeners registered by
	 * PanelDescriptors or specific Panels) <br>
	 * NOTE: this is protected for testing purposes, it should not be called
	 * directly.
	 * 
	 * @param eventType -
	 *            an ActionEvent class or PanelListenerKey
	 * @return
	 */
	protected Collection<ActionListener> getEventListenersByKey(PanelListenerKey key) {
		Set<ActionListener> actionListeners = eventTypeActionListenerSets.get(key);
		if (actionListeners == null) {
			actionListeners = new HashSet<ActionListener>();
			eventTypeActionListenerSets.put(key, actionListeners);
		}
		return actionListeners;
	}

	public ActionListener addEventTypeActionListener(Class<? extends ActionEvent> eventType,
			ActionListener listener) {
		log.debug("eventType = " + eventType + " listener = " + listener);
		getEventListenersByKey(new PanelListenerKey(Class.class.cast(eventType))).add(listener);
		return listener;
	}

	public ActionListener addEventTypeActionListener(Class<? extends ActionEvent> eventType,
			ActionListener listener, Object destinationObject) {
		log.debug("eventType = " + eventType + " listener = " + listener + " destinationObject = "
				+ destinationObject);
		getEventListenersByKey(new PanelListenerKey(Class.class.cast(eventType), destinationObject))
				.add(listener);
		return listener;
	}

	public void removeEventTypeActionListener(Class<? extends ActionEvent> eventType,
			ActionListener listener) {
		log.debug("eventType = " + eventType + " listener = " + listener);
		getEventListenersByKey(new PanelListenerKey(Class.class.cast(eventType))).remove(listener);
	}

	public void removeEventTypeActionListener(Class<? extends ActionEvent> eventType,
			ActionListener listener, Object destinationObject) {
		log.debug("eventType = " + eventType + " listener = " + listener);
		getEventListenersByKey(new PanelListenerKey(Class.class.cast(eventType), destinationObject))
				.remove(listener);
	}

	/**
	 * add a listener for open panel events that match the supplied
	 * PanelDescriptor
	 */
	public ActionListener addOpenPanelEventActionListener(PanelDescriptor panelDescriptor,
			ActionListener listener) {
		log.debug("panelDescriptor = " + panelDescriptor + " listener = " + listener);
		getEventListenersByKey(new PanelListenerKey(panelDescriptor)).add(listener);
		return listener;
	}

	/**
	 * remove a listener for open panel events that match the supplied
	 * PanelDescriptor
	 */
	public void removeOpenPanelEventActionListener(PanelDescriptor panelDescriptor,
			ActionListener listener) {
		log.debug("panelDescriptor = " + panelDescriptor + " listener = " + listener);
		getEventListenersByKey(new PanelListenerKey(panelDescriptor)).remove(listener);
	}

	/**
	 * @see edu.harvard.fas.rregan.uiframework.navigation.event.EventDispatcher#addPanelInstanceEventActionListener(edu.harvard.fas.rregan.uiframework.panel.Panel,
	 *      nextapp.echo2.app.event.ActionListener)
	 */
	public ActionListener addPanelInstanceEventActionListener(Panel panel, ActionListener listener) {
		log.debug("panel = " + panel + " listener = " + listener);
		getEventListenersByKey(new PanelListenerKey(ActionEvent.class, panel)).add(listener);
		return listener;
	}

	/**
	 * @see edu.harvard.fas.rregan.uiframework.navigation.event.EventDispatcher#removePanelInstanceEventActionListener(edu.harvard.fas.rregan.uiframework.panel.Panel,
	 *      nextapp.echo2.app.event.ActionListener)
	 */
	public void removePanelInstanceEventActionListener(Panel panel, ActionListener listener) {
		log.debug("panel = " + panel + " listener = " + listener);
		getEventListenersByKey(new PanelListenerKey(ActionEvent.class, panel)).remove(listener);
	}

	@Override
	public String toString() {
		return getClass().getSimpleName();
	}

	/**
	 * NOTE: this class is protected for testing purposes, it should not be used
	 * directly.
	 * 
	 * @author ron
	 */
	protected static class PanelListenerKey {
		private final Class<? extends ActionEvent> eventType;
		private final PanelActionType actionType;
		private final Class<?> contentType;
		private final String panelName;
		private final Panel panel;
		private final Object destination;

		/**
		 * This is for creating a key to register a listener rather than looking
		 * up a listener.<br>
		 * Create a key for the type of event only. this is primarily for
		 * controllers that listen for specific types of events.
		 * 
		 * @param eventType
		 */
		protected PanelListenerKey(Class<? extends ActionEvent> eventType) {
			this(eventType, null, null, null, null, null);
		}

		/**
		 * This is for creating a key to register a listener rather than looking
		 * up a listener.<br>
		 * Create a key that will match OpenPanelEvents to a specific type of
		 * panel. This is used to register a panel manager to get events for the
		 * types of windows it manages.
		 * 
		 * @param panelDescriptor
		 */
		protected PanelListenerKey(PanelDescriptor panelDescriptor) {
			this(OpenPanelEvent.class, panelDescriptor.getSupportedActionType(), panelDescriptor
					.getSupportedContentType(), panelDescriptor.getPanelName(), null, null);
		}

		/**
		 * This is for creating a key to look up a listener rather than register
		 * a listener.<br>
		 * Create a key for a given event, substituting the supplied class for
		 * the event type.
		 * 
		 * @param eventType
		 * @param event
		 */
		protected PanelListenerKey(Class<? extends ActionEvent> eventType, OpenPanelEvent event) {
			this(eventType, event.getPanelActionType(), event.getTargetType(),
					event.getPanelName(), event.getPanel(), null);
		}

		/**
		 * This is for creating a key to look up a listener rather than register
		 * a listener.<br>
		 * Create a key to match a ClosePanelEvent.
		 * 
		 * @param eventType -
		 *            the type is specified seperately so that the search can
		 *            start with the narrowest type that matches, and expand to
		 *            search for super types.
		 * @param event
		 */
		protected PanelListenerKey(Class<? extends ActionEvent> eventType, ClosePanelEvent event) {
			this(eventType, event.getPanelToClose());
		}

		/**
		 * This is for creating a key to register a panel specific listener and
		 * indirectly used to looking up a panel specific listener.<br>
		 * Create a key for a specific panel instance with the specified action
		 * type.
		 * 
		 * @param destination -
		 *            the destination object, most likely the original panel or
		 *            component (source) of an event that caused a window to
		 *            open and is tied to its resulting event, like a select
		 *            object event.
		 */
		protected PanelListenerKey(Class<? extends ActionEvent> eventType, Object destination) {
			this(eventType, null, null, null, null, destination);
		}

		/**
		 * This is for creating a key to register a panel specific listener and
		 * indirectly used to looking up a panel specific listener.<br>
		 * Create a key for a specific panel instance with the specified action
		 * type.
		 * 
		 * @param panel
		 */
		protected PanelListenerKey(Class<? extends ActionEvent> eventType, Panel panel) {
			this(eventType, null, null, null, panel, null);
		}

		/**
		 * this is the root constructor used by all the other constructors.
		 * Every part of the key can be specified directly.
		 * 
		 * @param eventType
		 * @param actionType
		 * @param contentType
		 * @param panelName
		 * @param panel
		 */
		protected PanelListenerKey(Class<? extends ActionEvent> eventType,
				PanelActionType actionType, Class<?> contentType, String panelName, Panel panel,
				Object destination) {
			this.eventType = eventType;
			this.actionType = actionType;
			this.contentType = contentType;
			this.panelName = panelName;
			this.panel = panel;
			this.destination = destination;
		}

		@Override
		public int hashCode() {
			final int prime = 31;
			int result = 1;
			result = prime * result + ((eventType == null) ? 0 : eventType.hashCode());
			result = prime * result + ((actionType == null) ? 0 : actionType.hashCode());
			result = prime * result + ((contentType == null) ? 0 : contentType.hashCode());
			result = prime * result + ((panelName == null) ? 0 : panelName.hashCode());
			result = prime * result + ((panel == null) ? 0 : panel.hashCode());
			result = prime * result + ((destination == null) ? 0 : destination.hashCode());
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

			final PanelListenerKey other = (PanelListenerKey) obj;
			if (eventType == null) {
				if (other.eventType != null) {
					return false;
				}
			} else if (!eventType.equals(other.eventType)) {
				return false;
			}
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
			if (panel == null) {
				if (other.panel != null) {
					return false;
				}
			} else if (!panel.equals(other.panel)) {
				return false;
			}
			if (destination == null) {
				if (other.destination != null) {
					return false;
				}
			} else if (!destination.equals(other.destination)) {
				return false;
			}
			return true;
		}

		@Override
		public String toString() {
			return getClass().getSimpleName() + "[eventType = " + eventType + ", actionType = "
					+ actionType + ", contentType = " + contentType + ", panelName = " + panelName
					+ ", panel = " + panel + ", destination = " + destination + "]";
		}
	}

	// TODO: could this cause two different action listeners to be considered
	// equal, or
	// the same listener to not be equal to itself?
	// Order the listeners so that Screens and Panels get messages
	// before other components to solve the problem with a panel loading its
	// state and erasing a change that a control on the panel received.
	protected static class ActionListenerComparator implements Comparator<ActionListener> {
		public int compare(ActionListener o1, ActionListener o2) {
			if (o1 == o2) {
				return 0;
			} else if ((o1 instanceof Screen) && !(o2 instanceof Screen)) {
				return -1;
			} else if ((o1 instanceof Panel) && !(o2 instanceof Panel)) {
				return -1;
			} else {
				// added this because EventDispatcherTest was failing as a
				// listener was not considered equal to itself when using the
				// contains() method
				if (o1.equals(o2)) {
					return 0;
				}
				return o1.hashCode() - o2.hashCode();
			}
		}
	};

}
