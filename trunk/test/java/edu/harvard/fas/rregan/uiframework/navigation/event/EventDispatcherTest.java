/*
 * $Id: EventDispatcherTest.java,v 1.7 2008/09/11 09:47:00 rregan Exp $
 * Copyright (c) 2008 Ron Regan Jr. All Rights Reserved.
 */

package edu.harvard.fas.rregan.uiframework.navigation.event;

import java.util.HashSet;
import java.util.Set;

import nextapp.echo2.app.event.ActionEvent;
import nextapp.echo2.app.event.ActionListener;
import edu.harvard.fas.rregan.TestCase;
import edu.harvard.fas.rregan.uiframework.TestActionListener;
import edu.harvard.fas.rregan.uiframework.TestPanel;
import edu.harvard.fas.rregan.uiframework.TestPanelDescriptor;
import edu.harvard.fas.rregan.uiframework.navigation.WorkflowDisposition;
import edu.harvard.fas.rregan.uiframework.panel.PanelActionType;

/**
 * @author ron
 */
public class EventDispatcherTest extends TestCase {

	/**
	 * Test method for
	 */
	public void testGetDistinctListenersForNullEvent() {
		DefaultEventDispatcher eventDispatcher = new DefaultEventDispatcher();
		try {
			eventDispatcher.getDistinctListenersForEvent(null);
		} catch (Exception e) {
			fail("getDistinctListenersForEvent threw an exception for a null event, but expected no exception: "
					+ e);
		}
	}

	/**
	 * Test method for:
	 * {@link edu.harvard.fas.rregan.uiframework.navigation.event.DefaultEventDispatcher#addEventTypeActionListener(java.lang.Class, nextapp.echo2.app.event.ActionListener)}
	 * {@link edu.harvard.fas.rregan.uiframework.navigation.event.DefaultEventDispatcher#removeEventTypeActionListener(java.lang.Class, nextapp.echo2.app.event.ActionListener)}
	 * {@link edu.harvard.fas.rregan.uiframework.navigation.event.DefaultEventDispatcher#getEventTypeActionListeners(java.lang.Object)}
	 */
	public void testGetAddRemoveEventTypeActionListener() {
		DefaultEventDispatcher eventDispatcher = new DefaultEventDispatcher();
		Set<ActionListener> expectedActionListeners = new HashSet<ActionListener>();
		assertEquals(expectedActionListeners, eventDispatcher.getDistinctListenersForEvent(null));

		ActionEvent testEvent1 = new ActionEvent(this, "test1");
		TestActionListener listener1 = new TestActionListener("listener1");
		expectedActionListeners.add(listener1);
		eventDispatcher.addEventTypeActionListener(testEvent1.getClass(), listener1, null);
		assertEquals(expectedActionListeners, eventDispatcher
				.getDistinctListenersForEvent(testEvent1));

		ActionEvent testEvent2 = new ActionEvent(this, "test2");
		TestActionListener listener2 = new TestActionListener("listener2");
		expectedActionListeners.add(listener2);
		eventDispatcher.addEventTypeActionListener(testEvent2.getClass(), listener2, null);
		assertEquals(expectedActionListeners, eventDispatcher
				.getDistinctListenersForEvent(testEvent1));
		assertEquals(expectedActionListeners, eventDispatcher
				.getDistinctListenersForEvent(testEvent2));

		expectedActionListeners.remove(listener1);
		eventDispatcher.removeEventTypeActionListener(testEvent1.getClass(), listener1, null);
		assertEquals(expectedActionListeners, eventDispatcher
				.getDistinctListenersForEvent(testEvent1));
		assertEquals(expectedActionListeners, eventDispatcher
				.getDistinctListenersForEvent(testEvent2));

		expectedActionListeners.remove(listener2);
		eventDispatcher.removeEventTypeActionListener(testEvent2.getClass(), listener2, null);
		assertEquals(expectedActionListeners, eventDispatcher
				.getDistinctListenersForEvent(testEvent1));
		assertEquals(expectedActionListeners, eventDispatcher
				.getDistinctListenersForEvent(testEvent2));

	}

	/**
	 * Test method for:
	 * {@link edu.harvard.fas.rregan.uiframework.navigation.event.DefaultEventDispatcher#addEventTypeActionListener(java.lang.Class, nextapp.echo2.app.event.ActionListener)}
	 * {@link edu.harvard.fas.rregan.uiframework.navigation.event.DefaultEventDispatcher#removeEventTypeActionListener(java.lang.Class, nextapp.echo2.app.event.ActionListener)}
	 * {@link edu.harvard.fas.rregan.uiframework.navigation.event.DefaultEventDispatcher#getEventTypeActionListeners(java.lang.Object)}
	 */
	public void testGetAddRemoveEventTypeActionListenerOpenPanelEvent() {
		DefaultEventDispatcher eventDispatcher = new DefaultEventDispatcher();
		Set<ActionListener> expectedActionListeners = new HashSet<ActionListener>();

		ActionEvent testEvent1 = new OpenPanelEvent(this, PanelActionType.Editor, null,
				Object.class, null);
		TestActionListener listener1 = new TestActionListener("listener1");
		expectedActionListeners.add(listener1);
		eventDispatcher.addEventTypeActionListener(testEvent1.getClass(), listener1, null);
		assertEquals(expectedActionListeners, eventDispatcher
				.getDistinctListenersForEvent(testEvent1));

		ActionEvent testEvent2 = new OpenPanelEvent(this, PanelActionType.Navigator, new Object(),
				Object.class, "fakePanelName");
		TestActionListener listener2 = new TestActionListener("listener2");
		expectedActionListeners.add(listener2);
		eventDispatcher.addEventTypeActionListener(testEvent2.getClass(), listener2, null);
		assertEquals(expectedActionListeners, eventDispatcher
				.getDistinctListenersForEvent(testEvent1));
		assertEquals(expectedActionListeners, eventDispatcher
				.getDistinctListenersForEvent(testEvent2));

		expectedActionListeners.remove(listener1);
		eventDispatcher.removeEventTypeActionListener(testEvent1.getClass(), listener1, null);
		assertEquals(expectedActionListeners, eventDispatcher
				.getDistinctListenersForEvent(testEvent1));
		assertEquals(expectedActionListeners, eventDispatcher
				.getDistinctListenersForEvent(testEvent2));

		expectedActionListeners.remove(listener2);
		eventDispatcher.removeEventTypeActionListener(testEvent2.getClass(), listener2, null);
		assertEquals(expectedActionListeners, eventDispatcher
				.getDistinctListenersForEvent(testEvent1));
		assertEquals(expectedActionListeners, eventDispatcher
				.getDistinctListenersForEvent(testEvent2));
	}

	/**
	 * Test method for:
	 * {@link edu.harvard.fas.rregan.uiframework.navigation.event.DefaultEventDispatcher#addEventTypeActionListener(java.lang.Class, nextapp.echo2.app.event.ActionListener)}
	 * {@link edu.harvard.fas.rregan.uiframework.navigation.event.DefaultEventDispatcher#removeEventTypeActionListener(java.lang.Class, nextapp.echo2.app.event.ActionListener)}
	 * {@link edu.harvard.fas.rregan.uiframework.navigation.event.DefaultEventDispatcher#getEventTypeActionListeners(java.lang.Object)}
	 */
	public void testGetAddRemoveEventTypeActionListenerDifferentEventTypes() {
		DefaultEventDispatcher eventDispatcher = new DefaultEventDispatcher();
		Set<ActionListener> expectedActionListeners1 = new HashSet<ActionListener>();
		Set<ActionListener> expectedActionListeners2 = new HashSet<ActionListener>();

		ActionEvent testEvent1 = new OpenPanelEvent(this, null);
		TestActionListener listener1 = new TestActionListener("listener1");
		expectedActionListeners1.add(listener1);
		eventDispatcher.addEventTypeActionListener(testEvent1.getClass(), listener1, null);
		assertEquals(expectedActionListeners1, eventDispatcher
				.getDistinctListenersForEvent(testEvent1));

		// different type of event
		ActionEvent testEvent2 = new ClosePanelEvent(this, null);
		TestActionListener listener2 = new TestActionListener("listener2");
		expectedActionListeners2.add(listener2);
		eventDispatcher.addEventTypeActionListener(testEvent2.getClass(), listener2, null);
		assertEquals(expectedActionListeners1, eventDispatcher
				.getDistinctListenersForEvent(testEvent1));
		assertEquals(expectedActionListeners2, eventDispatcher
				.getDistinctListenersForEvent(testEvent2));

		expectedActionListeners1.remove(listener1);
		eventDispatcher.removeEventTypeActionListener(testEvent1.getClass(), listener1, null);
		assertEquals(expectedActionListeners1, eventDispatcher
				.getDistinctListenersForEvent(testEvent1));
		assertEquals(expectedActionListeners2, eventDispatcher
				.getDistinctListenersForEvent(testEvent2));

		expectedActionListeners2.remove(listener2);
		eventDispatcher.removeEventTypeActionListener(testEvent2.getClass(), listener2, null);
		assertEquals(expectedActionListeners1, eventDispatcher
				.getDistinctListenersForEvent(testEvent1));
		assertEquals(expectedActionListeners2, eventDispatcher
				.getDistinctListenersForEvent(testEvent2));
	}

	/**
	 * Test method for:
	 * {@link edu.harvard.fas.rregan.uiframework.navigation.event.DefaultEventDispatcher#addEventTypeActionListener(java.lang.Class, nextapp.echo2.app.event.ActionListener)}
	 * {@link edu.harvard.fas.rregan.uiframework.navigation.event.DefaultEventDispatcher#removeEventTypeActionListener(java.lang.Class, nextapp.echo2.app.event.ActionListener)}
	 * {@link edu.harvard.fas.rregan.uiframework.navigation.event.DefaultEventDispatcher#getEventTypeActionListeners(java.lang.Object)}
	 */
	public void testGetAddRemoveEventTypeActionListenerSuperEventTypes() {
		DefaultEventDispatcher eventDispatcher = new DefaultEventDispatcher();

		TestActionListener testNavListener = new TestActionListener("testNavListener");
		TestActionListener testOpenListener = new TestActionListener("testOpenListener");
		TestActionListener testCloseListener = new TestActionListener("testCloseListener");

		Set<ActionListener> expectedNavEventListeners = new HashSet<ActionListener>();
		expectedNavEventListeners.add(testNavListener);

		Set<ActionListener> expectedForCloseEventListeners = new HashSet<ActionListener>();
		expectedForCloseEventListeners.add(testNavListener);
		expectedForCloseEventListeners.add(testCloseListener);

		Set<ActionListener> expectedOpenEventListeners = new HashSet<ActionListener>();
		expectedOpenEventListeners.add(testNavListener);
		expectedOpenEventListeners.add(testOpenListener);

		eventDispatcher.addEventTypeActionListener(NavigationEvent.class, testNavListener, null);
		eventDispatcher.addEventTypeActionListener(OpenPanelEvent.class, testOpenListener, null);
		eventDispatcher.addEventTypeActionListener(ClosePanelEvent.class, testCloseListener, null);

		ActionEvent testNavEvent = new NavigationEvent(this, "");
		ActionEvent testOpenEvent = new OpenPanelEvent(this, null);
		ActionEvent testCloseEvent = new ClosePanelEvent(this, null);

		assertEquals(expectedNavEventListeners, eventDispatcher
				.getDistinctListenersForEvent(testNavEvent));
		assertEquals(expectedForCloseEventListeners, eventDispatcher
				.getDistinctListenersForEvent(testCloseEvent));
		assertEquals(expectedOpenEventListeners, eventDispatcher
				.getDistinctListenersForEvent(testOpenEvent));

		eventDispatcher.removeEventTypeActionListener(NavigationEvent.class, testNavListener, null);
		expectedNavEventListeners.remove(testNavListener);
		expectedForCloseEventListeners.remove(testNavListener);
		expectedOpenEventListeners.remove(testNavListener);

		assertEquals(expectedNavEventListeners, eventDispatcher
				.getDistinctListenersForEvent(testNavEvent));
		assertEquals(expectedForCloseEventListeners, eventDispatcher
				.getDistinctListenersForEvent(testCloseEvent));
		assertEquals(expectedOpenEventListeners, eventDispatcher
				.getDistinctListenersForEvent(testOpenEvent));

		eventDispatcher.removeEventTypeActionListener(ClosePanelEvent.class, testCloseListener,
				null);
		expectedForCloseEventListeners.remove(testCloseListener);

		assertEquals(expectedNavEventListeners, eventDispatcher
				.getDistinctListenersForEvent(testNavEvent));
		assertEquals(expectedForCloseEventListeners, eventDispatcher
				.getDistinctListenersForEvent(testCloseEvent));
		assertEquals(expectedOpenEventListeners, eventDispatcher
				.getDistinctListenersForEvent(testOpenEvent));

	}

	/**
	 * Test method for:
	 * {@link edu.harvard.fas.rregan.uiframework.navigation.event.DefaultEventDispatcher#addOpenPanelEventActionListener(edu.harvard.fas.rregan.uiframework.panel.PanelDescriptor, nextapp.echo2.app.event.ActionListener)}
	 * {@link edu.harvard.fas.rregan.uiframework.navigation.event.DefaultEventDispatcher#removeOpenPanelEventActionListener(edu.harvard.fas.rregan.uiframework.panel.PanelDescriptor, nextapp.echo2.app.event.ActionListener)}
	 * {@link edu.harvard.fas.rregan.uiframework.navigation.event.DefaultEventDispatcher#getEventTypeActionListeners(java.lang.Object)}
	 */
	public void testGetAddRemoveOpenPanelEventActionListener() {
		DefaultEventDispatcher eventDispatcher = new DefaultEventDispatcher();

		TestPanelDescriptor testDescriptor1et1 = new TestPanelDescriptor(TestPanelType1.class,
				TestTargetType1.class, PanelActionType.Editor);
		TestActionListener testListener1et1 = new TestActionListener("testListener1et1");
		eventDispatcher.addOpenPanelEventActionListener(testDescriptor1et1, testListener1et1);

		TestPanelDescriptor testDescriptor1nt1 = new TestPanelDescriptor(TestPanelType1.class,
				TestTargetType1.class, PanelActionType.Navigator);
		TestActionListener testListener1nt1 = new TestActionListener("testListener1nt1");
		eventDispatcher.addOpenPanelEventActionListener(testDescriptor1nt1, testListener1nt1);

		TestPanelDescriptor testDescriptor1st1 = new TestPanelDescriptor(TestPanelType1.class,
				TestTargetType1.class, PanelActionType.Selector);
		TestActionListener testListener1st1 = new TestActionListener("testListener1st1");
		eventDispatcher.addOpenPanelEventActionListener(testDescriptor1st1, testListener1st1);

		TestPanelDescriptor testDescriptor1et2 = new TestPanelDescriptor(TestPanelType1.class,
				TestTargetType2.class, PanelActionType.Editor);
		TestActionListener testListener1et2 = new TestActionListener("testListener1et2");
		eventDispatcher.addOpenPanelEventActionListener(testDescriptor1et2, testListener1et2);

		TestPanelDescriptor testDescriptor1nt2 = new TestPanelDescriptor(TestPanelType1.class,
				TestTargetType2.class, PanelActionType.Navigator);
		TestActionListener testListener1nt2 = new TestActionListener("testListener1nt2");
		eventDispatcher.addOpenPanelEventActionListener(testDescriptor1nt2, testListener1nt2);

		TestPanelDescriptor testDescriptor1st2 = new TestPanelDescriptor(TestPanelType1.class,
				TestTargetType2.class, PanelActionType.Selector);
		TestActionListener testListener1st2 = new TestActionListener("testListener1st2");
		eventDispatcher.addOpenPanelEventActionListener(testDescriptor1st2, testListener1st2);

		TestPanelDescriptor testDescriptor1et3 = new TestPanelDescriptor(TestPanelType1.class,
				TestTargetType3.class, PanelActionType.Editor);
		TestActionListener testListener1et3 = new TestActionListener("testListener1et3");
		eventDispatcher.addOpenPanelEventActionListener(testDescriptor1et3, testListener1et3);

		TestPanelDescriptor testDescriptor1nt3 = new TestPanelDescriptor(TestPanelType1.class,
				TestTargetType3.class, PanelActionType.Navigator);
		TestActionListener testListener1nt3 = new TestActionListener("testListener1nt3");
		eventDispatcher.addOpenPanelEventActionListener(testDescriptor1nt3, testListener1nt3);

		TestPanelDescriptor testDescriptor1st3 = new TestPanelDescriptor(TestPanelType1.class,
				TestTargetType3.class, PanelActionType.Selector);
		TestActionListener testListener1st3 = new TestActionListener("testListener1st3");
		eventDispatcher.addOpenPanelEventActionListener(testDescriptor1st3, testListener1st3);

		TestPanelDescriptor testDescriptor2et1 = new TestPanelDescriptor(TestPanelType2.class,
				TestTargetType1.class, PanelActionType.Editor);
		TestActionListener testListener2et1 = new TestActionListener("testListener2et1");
		eventDispatcher.addOpenPanelEventActionListener(testDescriptor2et1, testListener2et1);

		TestPanelDescriptor testDescriptor2nt1 = new TestPanelDescriptor(TestPanelType2.class,
				TestTargetType1.class, PanelActionType.Navigator);
		TestActionListener testListener2nt1 = new TestActionListener("testListener2nt1");
		eventDispatcher.addOpenPanelEventActionListener(testDescriptor2nt1, testListener2nt1);

		TestPanelDescriptor testDescriptor2st1 = new TestPanelDescriptor(TestPanelType2.class,
				TestTargetType1.class, PanelActionType.Selector);
		TestActionListener testListener2st1 = new TestActionListener("testListener2st1");
		eventDispatcher.addOpenPanelEventActionListener(testDescriptor2st1, testListener2st1);

		TestPanelDescriptor testDescriptor2et2 = new TestPanelDescriptor(TestPanelType2.class,
				TestTargetType2.class, PanelActionType.Editor);
		TestActionListener testListener2et2 = new TestActionListener("testListener2et2");
		eventDispatcher.addOpenPanelEventActionListener(testDescriptor2et2, testListener2et2);

		TestPanelDescriptor testDescriptor2nt2 = new TestPanelDescriptor(TestPanelType2.class,
				TestTargetType2.class, PanelActionType.Navigator);
		TestActionListener testListener2nt2 = new TestActionListener("testListener2nt2");
		eventDispatcher.addOpenPanelEventActionListener(testDescriptor2nt2, testListener2nt2);

		TestPanelDescriptor testDescriptor2st2 = new TestPanelDescriptor(TestPanelType2.class,
				TestTargetType2.class, PanelActionType.Selector);
		TestActionListener testListener2st2 = new TestActionListener("testListener2st2");
		eventDispatcher.addOpenPanelEventActionListener(testDescriptor2st2, testListener2st2);

		TestPanelDescriptor testDescriptor2et3 = new TestPanelDescriptor(TestPanelType2.class,
				TestTargetType3.class, PanelActionType.Editor);
		TestActionListener testListener2et3 = new TestActionListener("testListener2et3");
		eventDispatcher.addOpenPanelEventActionListener(testDescriptor2et3, testListener2et3);

		TestPanelDescriptor testDescriptor2nt3 = new TestPanelDescriptor(TestPanelType2.class,
				TestTargetType3.class, PanelActionType.Navigator);
		TestActionListener testListener2nt3 = new TestActionListener("testListener2nt3");
		eventDispatcher.addOpenPanelEventActionListener(testDescriptor2nt3, testListener2nt3);

		TestPanelDescriptor testDescriptor2st3 = new TestPanelDescriptor(TestPanelType2.class,
				TestTargetType3.class, PanelActionType.Selector);
		TestActionListener testListener2st3 = new TestActionListener("testListener2st3");
		eventDispatcher.addOpenPanelEventActionListener(testDescriptor2st3, testListener2st3);

		TestPanelDescriptor testDescriptor3et1 = new TestPanelDescriptor(TestPanelType3.class,
				TestTargetType1.class, PanelActionType.Editor);
		TestActionListener testListener3et1 = new TestActionListener("testListener3et1");
		eventDispatcher.addOpenPanelEventActionListener(testDescriptor3et1, testListener3et1);

		TestPanelDescriptor testDescriptor3nt1 = new TestPanelDescriptor(TestPanelType3.class,
				TestTargetType1.class, PanelActionType.Navigator);
		TestActionListener testListener3nt1 = new TestActionListener("testListener3nt1");
		eventDispatcher.addOpenPanelEventActionListener(testDescriptor3nt1, testListener3nt1);

		TestPanelDescriptor testDescriptor3st1 = new TestPanelDescriptor(TestPanelType3.class,
				TestTargetType1.class, PanelActionType.Selector);
		TestActionListener testListener3st1 = new TestActionListener("testListener3st1");
		eventDispatcher.addOpenPanelEventActionListener(testDescriptor3st1, testListener3st1);

		TestPanelDescriptor testDescriptor3et2 = new TestPanelDescriptor(TestPanelType3.class,
				TestTargetType2.class, PanelActionType.Editor);
		TestActionListener testListener3et2 = new TestActionListener("testListener3et2");
		eventDispatcher.addOpenPanelEventActionListener(testDescriptor3et2, testListener3et2);

		TestPanelDescriptor testDescriptor3nt2 = new TestPanelDescriptor(TestPanelType3.class,
				TestTargetType2.class, PanelActionType.Navigator);
		TestActionListener testListener3nt2 = new TestActionListener("testListener3nt2");
		eventDispatcher.addOpenPanelEventActionListener(testDescriptor3nt2, testListener3nt2);

		TestPanelDescriptor testDescriptor3st2 = new TestPanelDescriptor(TestPanelType3.class,
				TestTargetType2.class, PanelActionType.Selector);
		TestActionListener testListener3st2 = new TestActionListener("testListener3st2");
		eventDispatcher.addOpenPanelEventActionListener(testDescriptor3st2, testListener3st2);

		TestPanelDescriptor testDescriptor3et3 = new TestPanelDescriptor(TestPanelType3.class,
				TestTargetType3.class, PanelActionType.Editor);
		TestActionListener testListener3et3 = new TestActionListener("testListener3et3");
		eventDispatcher.addOpenPanelEventActionListener(testDescriptor3et3, testListener3et3);

		TestPanelDescriptor testDescriptor3nt3 = new TestPanelDescriptor(TestPanelType3.class,
				TestTargetType3.class, PanelActionType.Navigator);
		TestActionListener testListener3nt3 = new TestActionListener("testListener3nt3");
		eventDispatcher.addOpenPanelEventActionListener(testDescriptor3nt3, testListener3nt3);

		TestPanelDescriptor testDescriptor3st3 = new TestPanelDescriptor(TestPanelType3.class,
				TestTargetType3.class, PanelActionType.Selector);
		TestActionListener testListener3st3 = new TestActionListener("testListener3st3");
		eventDispatcher.addOpenPanelEventActionListener(testDescriptor3st3, testListener3st3);

		ActionEvent testEventEt1 = new OpenPanelEvent(this, PanelActionType.Editor, null,
				TestTargetType1.class, null, WorkflowDisposition.NewFlow);
		ActionEvent testEventNt1 = new OpenPanelEvent(this, PanelActionType.Navigator, null,
				TestTargetType1.class, null, WorkflowDisposition.NewFlow);
		ActionEvent testEventSt1 = new OpenPanelEvent(this, PanelActionType.Selector, null,
				TestTargetType1.class, null, WorkflowDisposition.NewFlow);

		ActionEvent testEventEt2 = new OpenPanelEvent(this, PanelActionType.Editor, null,
				TestTargetType2.class, null, WorkflowDisposition.NewFlow);
		ActionEvent testEventNt2 = new OpenPanelEvent(this, PanelActionType.Navigator, null,
				TestTargetType2.class, null, WorkflowDisposition.NewFlow);
		ActionEvent testEventSt2 = new OpenPanelEvent(this, PanelActionType.Selector, null,
				TestTargetType2.class, null, WorkflowDisposition.NewFlow);

		ActionEvent testEventEt3 = new OpenPanelEvent(this, PanelActionType.Editor, null,
				TestTargetType3.class, null, WorkflowDisposition.NewFlow);
		ActionEvent testEventNt3 = new OpenPanelEvent(this, PanelActionType.Navigator, null,
				TestTargetType3.class, null, WorkflowDisposition.NewFlow);
		ActionEvent testEventSt3 = new OpenPanelEvent(this, PanelActionType.Selector, null,
				TestTargetType3.class, null, WorkflowDisposition.NewFlow);

		Set<ActionListener> expectedListenersForTestEventEt1 = new HashSet<ActionListener>();
		expectedListenersForTestEventEt1.add(testListener1et1);
		expectedListenersForTestEventEt1.add(testListener2et1);
		expectedListenersForTestEventEt1.add(testListener3et1);
		assertEquals(expectedListenersForTestEventEt1, eventDispatcher
				.getDistinctListenersForEvent(testEventEt1));

		eventDispatcher.removeOpenPanelEventActionListener(testDescriptor1et1, testListener1et1);
		expectedListenersForTestEventEt1.remove(testListener1et1);
		assertEquals(expectedListenersForTestEventEt1, eventDispatcher
				.getDistinctListenersForEvent(testEventEt1));

		eventDispatcher.removeOpenPanelEventActionListener(testDescriptor2et1, testListener2et1);
		expectedListenersForTestEventEt1.remove(testListener2et1);
		assertEquals(expectedListenersForTestEventEt1, eventDispatcher
				.getDistinctListenersForEvent(testEventEt1));

		eventDispatcher.removeOpenPanelEventActionListener(testDescriptor3et1, testListener3et1);
		expectedListenersForTestEventEt1.remove(testListener3et1);
		assertEquals(expectedListenersForTestEventEt1, eventDispatcher
				.getDistinctListenersForEvent(testEventEt1));
		assertEquals(0, eventDispatcher.getDistinctListenersForEvent(testEventEt1).size());

		Set<ActionListener> expectedListenersForTestEventNt1 = new HashSet<ActionListener>();
		expectedListenersForTestEventNt1.add(testListener1nt1);
		expectedListenersForTestEventNt1.add(testListener2nt1);
		expectedListenersForTestEventNt1.add(testListener3nt1);
		assertEquals(expectedListenersForTestEventNt1, eventDispatcher
				.getDistinctListenersForEvent(testEventNt1));

		Set<ActionListener> expectedListenersForTestEventSt1 = new HashSet<ActionListener>();
		expectedListenersForTestEventSt1.add(testListener1st1);
		expectedListenersForTestEventSt1.add(testListener2st1);
		expectedListenersForTestEventSt1.add(testListener3st1);
		assertEquals(expectedListenersForTestEventSt1, eventDispatcher
				.getDistinctListenersForEvent(testEventSt1));

		Set<ActionListener> expectedListenersForTestEventEt2 = new HashSet<ActionListener>();
		expectedListenersForTestEventEt2.add(testListener1et2);
		expectedListenersForTestEventEt2.add(testListener2et2);
		expectedListenersForTestEventEt2.add(testListener3et2);
		assertEquals(expectedListenersForTestEventEt2, eventDispatcher
				.getDistinctListenersForEvent(testEventEt2));

		Set<ActionListener> expectedListenersForTestEventNt2 = new HashSet<ActionListener>();
		expectedListenersForTestEventNt2.add(testListener1nt2);
		expectedListenersForTestEventNt2.add(testListener2nt2);
		expectedListenersForTestEventNt2.add(testListener3nt2);
		assertEquals(expectedListenersForTestEventNt2, eventDispatcher
				.getDistinctListenersForEvent(testEventNt2));

		Set<ActionListener> expectedListenersForTestEventSt2 = new HashSet<ActionListener>();
		expectedListenersForTestEventSt2.add(testListener1st2);
		expectedListenersForTestEventSt2.add(testListener2st2);
		expectedListenersForTestEventSt2.add(testListener3st2);
		assertEquals(expectedListenersForTestEventSt2, eventDispatcher
				.getDistinctListenersForEvent(testEventSt2));

		Set<ActionListener> expectedListenersForTestEventEt3 = new HashSet<ActionListener>();
		expectedListenersForTestEventEt3.add(testListener1et3);
		expectedListenersForTestEventEt3.add(testListener2et3);
		expectedListenersForTestEventEt3.add(testListener3et3);
		assertEquals(expectedListenersForTestEventEt3, eventDispatcher
				.getDistinctListenersForEvent(testEventEt3));

		Set<ActionListener> expectedListenersForTestEventNt3 = new HashSet<ActionListener>();
		expectedListenersForTestEventNt3.add(testListener1nt3);
		expectedListenersForTestEventNt3.add(testListener2nt3);
		expectedListenersForTestEventNt3.add(testListener3nt3);
		assertEquals(expectedListenersForTestEventNt3, eventDispatcher
				.getDistinctListenersForEvent(testEventNt3));

		Set<ActionListener> expectedListenersForTestEventSt3 = new HashSet<ActionListener>();
		expectedListenersForTestEventSt3.add(testListener1st3);
		expectedListenersForTestEventSt3.add(testListener2st3);
		expectedListenersForTestEventSt3.add(testListener3st3);
		assertEquals(expectedListenersForTestEventSt3, eventDispatcher
				.getDistinctListenersForEvent(testEventSt3));
	}

	private static class TestPanelType1 extends TestPanel {
		public TestPanelType1(String panelName, PanelActionType supportActionType,
				Class<?> supportedContentType) {
			super(panelName, supportActionType, supportedContentType);
		}
	}

	private static class TestPanelType2 extends TestPanel {
		public TestPanelType2(String panelName, PanelActionType supportActionType,
				Class<?> supportedContentType) {
			super(panelName, supportActionType, supportedContentType);
		}
	}

	private static class TestPanelType3 extends TestPanel {
		public TestPanelType3(String panelName, PanelActionType supportActionType,
				Class<?> supportedContentType) {
			super(panelName, supportActionType, supportedContentType);
		}
	}

	private static interface TestSuperType {
		public void dummy();
	}

	private static class TestSubType implements TestSuperType {
		public void dummy() {
			int i = 0;
			i++;
		}
	}

	private static class TestTargetType1 {
		private final String name;

		public TestTargetType1(String name) {
			this.name = name;
		}

		@Override
		public String toString() {
			return name;
		}
	}

	private static class TestTargetType2 {
		private final String name;

		public TestTargetType2(String name) {
			this.name = name;
		}

		@Override
		public String toString() {
			return name;
		}
	}

	private static class TestTargetType3 {
		private final String name;

		public TestTargetType3(String name) {
			this.name = name;
		}

		@Override
		public String toString() {
			return name;
		}
	}

	/**
	 * Test method for:
	 * {@link edu.harvard.fas.rregan.uiframework.navigation.event.DefaultEventDispatcher#addPanelInstanceEventActionListener(edu.harvard.fas.rregan.uiframework.panel.Panel, nextapp.echo2.app.event.ActionListener)}
	 * {@link edu.harvard.fas.rregan.uiframework.navigation.event.DefaultEventDispatcher#removePanelInstanceEventActionListener(edu.harvard.fas.rregan.uiframework.panel.Panel, nextapp.echo2.app.event.ActionListener)}
	 * {@link edu.harvard.fas.rregan.uiframework.navigation.event.DefaultEventDispatcher#getEventTypeActionListeners(java.lang.Object)}
	 */
	public void testGetAddRemovePanelInstanceEventActionListener() {
		DefaultEventDispatcher eventDispatcher = new DefaultEventDispatcher();
		TestPanelType1 testPanel1 = new TestPanelType1(null, PanelActionType.Unspecified,
				Object.class);
		TestActionListener testListenerPanel1 = new TestActionListener("testListener1");

		eventDispatcher.addPanelInstanceEventActionListener(testPanel1, testListenerPanel1);

		ActionEvent testEventOpen = new OpenPanelEvent(this, testPanel1);

		ActionEvent testEventClose = new ClosePanelEvent(this, testPanel1);

		Set<ActionListener> expectedListenersForTestEventEt1 = new HashSet<ActionListener>();
		expectedListenersForTestEventEt1.add(testListenerPanel1);

		assertEquals(expectedListenersForTestEventEt1, eventDispatcher
				.getDistinctListenersForEvent(testEventOpen));

		assertEquals(expectedListenersForTestEventEt1, eventDispatcher
				.getDistinctListenersForEvent(testEventClose));

		eventDispatcher.removePanelInstanceEventActionListener(testPanel1, testListenerPanel1);
		expectedListenersForTestEventEt1.remove(testListenerPanel1);

		assertEquals(expectedListenersForTestEventEt1, eventDispatcher
				.getDistinctListenersForEvent(testEventOpen));

		assertEquals(expectedListenersForTestEventEt1, eventDispatcher
				.getDistinctListenersForEvent(testEventClose));
	}

	/**
	 * This test simulates having a few different panel types available for
	 * opening, adding panel instance listeners as if a panel is open and how
	 * that impacts which listeners should be notified of events. Test method
	 * for:
	 * {@link edu.harvard.fas.rregan.uiframework.navigation.event.DefaultEventDispatcher#addEventTypeActionListener(java.lang.Class, nextapp.echo2.app.event.ActionListener)}
	 * {@link edu.harvard.fas.rregan.uiframework.navigation.event.DefaultEventDispatcher#removeEventTypeActionListener(java.lang.Class, nextapp.echo2.app.event.ActionListener)}
	 * {@link edu.harvard.fas.rregan.uiframework.navigation.event.DefaultEventDispatcher#getEventTypeActionListeners(java.lang.Object)}
	 * {@link edu.harvard.fas.rregan.uiframework.navigation.event.DefaultEventDispatcher#addPanelInstanceEventActionListener(edu.harvard.fas.rregan.uiframework.panel.Panel, nextapp.echo2.app.event.ActionListener)}
	 * {@link edu.harvard.fas.rregan.uiframework.navigation.event.DefaultEventDispatcher#removePanelInstanceEventActionListener(edu.harvard.fas.rregan.uiframework.panel.Panel, nextapp.echo2.app.event.ActionListener)}
	 * {@link edu.harvard.fas.rregan.uiframework.navigation.event.DefaultEventDispatcher#getEventTypeActionListeners(java.lang.Object)}
	 */
	public void testGetAddRemoveMixed() {
		DefaultEventDispatcher eventDispatcher = new DefaultEventDispatcher();

		TestPanelDescriptor testDescriptor1et1 = new TestPanelDescriptor(TestPanelType1.class,
				TestTargetType1.class, PanelActionType.Editor);
		TestActionListener testListener1et1 = new TestActionListener("testListener1et1");
		eventDispatcher.addOpenPanelEventActionListener(testDescriptor1et1, testListener1et1);

		TestPanelDescriptor testDescriptor1nt1 = new TestPanelDescriptor(TestPanelType1.class,
				TestTargetType1.class, PanelActionType.Navigator);
		TestActionListener testListener1nt1 = new TestActionListener("testListener1nt1");
		eventDispatcher.addOpenPanelEventActionListener(testDescriptor1nt1, testListener1nt1);

		TestPanelDescriptor testDescriptor1st1 = new TestPanelDescriptor(TestPanelType1.class,
				TestTargetType1.class, PanelActionType.Selector);
		TestActionListener testListener1st1 = new TestActionListener("testListener1st1");
		eventDispatcher.addOpenPanelEventActionListener(testDescriptor1st1, testListener1st1);

		TestTargetType1 testTarget1 = new TestTargetType1("testTarget1");
		ActionEvent testEventEt1 = new OpenPanelEvent(this, PanelActionType.Editor, testTarget1,
				TestTargetType1.class, null, WorkflowDisposition.NewFlow);
		ActionEvent testEventNt1 = new OpenPanelEvent(this, PanelActionType.Navigator, testTarget1,
				TestTargetType1.class, null, WorkflowDisposition.NewFlow);
		ActionEvent testEventSt1 = new OpenPanelEvent(this, PanelActionType.Selector, testTarget1,
				TestTargetType1.class, null, WorkflowDisposition.NewFlow);

		Set<ActionListener> expectedListenersForTestEventEt1 = new HashSet<ActionListener>();
		expectedListenersForTestEventEt1.add(testListener1et1);
		assertEquals(expectedListenersForTestEventEt1, eventDispatcher
				.getDistinctListenersForEvent(testEventEt1));

		Set<ActionListener> expectedListenersForTestEventNt1 = new HashSet<ActionListener>();
		expectedListenersForTestEventNt1.add(testListener1nt1);
		assertEquals(expectedListenersForTestEventNt1, eventDispatcher
				.getDistinctListenersForEvent(testEventNt1));

		Set<ActionListener> expectedListenersForTestEventSt1 = new HashSet<ActionListener>();
		expectedListenersForTestEventSt1.add(testListener1st1);
		assertEquals(expectedListenersForTestEventSt1, eventDispatcher
				.getDistinctListenersForEvent(testEventSt1));

		// simulate that a panel is opened
		TestPanelType1 testPanel1 = new TestPanelType1(null, PanelActionType.Editor,
				TestTargetType1.class);
		testPanel1.setTargetObject(testTarget1);
		TestActionListener testListenerPanel1 = new TestActionListener("testListener1");
		Set<ActionListener> expectedListenersForTestEventPanel1 = new HashSet<ActionListener>();
		expectedListenersForTestEventPanel1.add(testListenerPanel1);
		eventDispatcher.addPanelInstanceEventActionListener(testPanel1, testListenerPanel1);
		ActionEvent testEventPanel1 = new OpenPanelEvent(this, testPanel1);

		assertEquals(expectedListenersForTestEventPanel1, eventDispatcher
				.getDistinctListenersForEvent(testEventPanel1));

		// see that it doesn't break the other listeners
		assertEquals(expectedListenersForTestEventEt1, eventDispatcher
				.getDistinctListenersForEvent(testEventEt1));

		assertEquals(expectedListenersForTestEventNt1, eventDispatcher
				.getDistinctListenersForEvent(testEventNt1));

		assertEquals(expectedListenersForTestEventSt1, eventDispatcher
				.getDistinctListenersForEvent(testEventSt1));

	}

	/**
	 * Test method for:
	 * {@link edu.harvard.fas.rregan.uiframework.navigation.event.DefaultEventDispatcher#dispatchEvent(nextapp.echo2.app.event.ActionEvent)}
	 * {@link edu.harvard.fas.rregan.uiframework.navigation.event.DefaultEventDispatcher#actionPerformed(nextapp.echo2.app.event.ActionEvent)}
	 */
	public void testDispatchEvent() {
		DefaultEventDispatcher eventDispatcher = new DefaultEventDispatcher();
		Set<TestActionListener> allListeners = new HashSet<TestActionListener>();
		Set<TestActionListener> expectedListeners = new HashSet<TestActionListener>();

		TestPanelDescriptor testDescriptor1et1 = new TestPanelDescriptor(TestPanelType1.class,
				TestTargetType1.class, PanelActionType.Editor);
		TestActionListener testListener1et1 = new TestActionListener("testListener1et1");
		eventDispatcher.addOpenPanelEventActionListener(testDescriptor1et1, testListener1et1);

		TestPanelDescriptor testDescriptor1nt1 = new TestPanelDescriptor(TestPanelType1.class,
				TestTargetType1.class, PanelActionType.Navigator);
		TestActionListener testListener1nt1 = new TestActionListener("testListener1nt1");
		eventDispatcher.addOpenPanelEventActionListener(testDescriptor1nt1, testListener1nt1);

		TestPanelDescriptor testDescriptor1st1 = new TestPanelDescriptor(TestPanelType1.class,
				TestTargetType1.class, PanelActionType.Selector);
		TestActionListener testListener1st1 = new TestActionListener("testListener1st1");
		eventDispatcher.addOpenPanelEventActionListener(testDescriptor1st1, testListener1st1);

		TestTargetType1 testTarget1 = new TestTargetType1("testTarget1");

		ActionEvent testEventEt1 = new OpenPanelEvent(this, PanelActionType.Editor, testTarget1,
				TestTargetType1.class, null, WorkflowDisposition.NewFlow);
		ActionEvent testEventNt1 = new OpenPanelEvent(this, PanelActionType.Navigator, testTarget1,
				TestTargetType1.class, null, WorkflowDisposition.NewFlow);
		ActionEvent testEventSt1 = new OpenPanelEvent(this, PanelActionType.Selector, testTarget1,
				TestTargetType1.class, null, WorkflowDisposition.NewFlow);

		allListeners.add(testListener1et1);
		allListeners.add(testListener1nt1);
		allListeners.add(testListener1st1);

		// open editor event
		expectedListeners.add(testListener1et1);
		eventDispatcher.dispatchEvent(testEventEt1);
		for (TestActionListener listener : allListeners) {
			if (expectedListeners.contains(listener)) {
				assertEquals(testEventEt1, listener.getEventReceived());
				listener.clearEventReceived();
			} else {
				assertNull(listener.getEventReceived());
			}
		}
		expectedListeners.clear();

		// open navigator event
		expectedListeners.add(testListener1nt1);
		eventDispatcher.dispatchEvent(testEventNt1);
		for (TestActionListener listener : allListeners) {
			if (expectedListeners.contains(listener)) {
				assertEquals(testEventNt1, listener.getEventReceived());
				listener.clearEventReceived();
			} else {
				assertNull(listener.getEventReceived());
			}
		}
		expectedListeners.clear();

		// open selector event
		expectedListeners.add(testListener1st1);
		eventDispatcher.dispatchEvent(testEventSt1);
		for (TestActionListener listener : allListeners) {
			if (expectedListeners.contains(listener)) {
				assertEquals(testEventSt1, listener.getEventReceived());
				listener.clearEventReceived();
			} else {
				assertNull(listener.getEventReceived());
			}
		}
		expectedListeners.clear();
	}

	public void testEditEventWithConcreteType() {
		DefaultEventDispatcher eventDispatcher = new DefaultEventDispatcher();
		Set<TestActionListener> allListeners = new HashSet<TestActionListener>();
		Set<TestActionListener> expectedListeners = new HashSet<TestActionListener>();

		TestPanelDescriptor testDescriptor = new TestPanelDescriptor(TestPanelType1.class,
				TestSuperType.class, PanelActionType.Editor);
		TestActionListener testListener = new TestActionListener("testListener1et1");
		eventDispatcher.addOpenPanelEventActionListener(testDescriptor, testListener);

		TestSubType testTarget1 = new TestSubType();

		ActionEvent testEvent1 = new OpenPanelEvent(this, PanelActionType.Editor, testTarget1,
				testTarget1.getClass(), null, WorkflowDisposition.NewFlow);

		allListeners.add(testListener);

		// open editor event
		expectedListeners.add(testListener);
		eventDispatcher.dispatchEvent(testEvent1);
		for (TestActionListener listener : allListeners) {
			if (expectedListeners.contains(listener)) {
				assertEquals(testEvent1, listener.getEventReceived());
				listener.clearEventReceived();
			} else {
				assertNull(listener.getEventReceived());
			}
		}
		expectedListeners.clear();
	}
}
