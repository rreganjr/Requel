/*
 * $Id: CheckBoxTreeSetModel.java,v 1.6 2009/02/20 10:26:16 rregan Exp $
 * Copyright (c) 2008 Ron Regan Jr. All Rights Reserved.
 */

package edu.harvard.fas.rregan.uiframework.panel.editor;

import java.util.Enumeration;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import nextapp.echo2.app.CheckBox;
import nextapp.echo2.app.Component;
import nextapp.echo2.app.Label;
import nextapp.echo2.app.button.DefaultToggleButtonModel;
import nextapp.echo2.app.button.ToggleButtonModel;
import nextapp.echo2.app.event.ChangeEvent;
import nextapp.echo2.app.event.ChangeListener;

import org.apache.log4j.Logger;

import echopointng.tree.DefaultMutableTreeNode;
import echopointng.tree.DefaultTreeModel;

/**
 * A tree model for a CheckBoxTreeSet where each path is assigned a
 * ToggleButtonModel representing on/off or true/false for each path.
 * 
 * @author ron
 */
public class CheckBoxTreeSetModel extends DefaultTreeModel implements ChangeListener {
	static final long serialVersionUID = 0L;
	private static final Logger log = Logger.getLogger(CheckBoxTreeSetModel.class);
	private final Set<ChangeListener> changeListeners = new HashSet<ChangeListener>();
	private final Map<String, ToggleButtonModel> checkBoxModels = new HashMap<String, ToggleButtonModel>();
	private Set<String> editablePaths = null;
	private boolean checkboxesOnLeavesOnly = false;

	/**
	 * create an empty model
	 */
	public CheckBoxTreeSetModel() {
		this(null);
	}

	/**
	 * @param editablePaths
	 */
	protected CheckBoxTreeSetModel(Set<String> editablePaths) {
		super(new DefaultMutableTreeNode("", true));
		this.editablePaths = editablePaths;
	}

	/**
	 * create a model with unix style paths representing the tree nodes along
	 * with a set of paths representing the nodes that are initially checked.
	 * Each level in the tree will have a checkbox except for the root and all
	 * nodes will be enabled or disabled via the setEnabled() method.
	 * 
	 * @param optionPaths
	 * @param initialSelection
	 */
	public CheckBoxTreeSetModel(Set<String> optionPaths, Set<String> initialSelection) {
		this(optionPaths, null, initialSelection, false);
	}

	/**
	 * create a model with unix style paths representing the tree nodes along
	 * with a set of paths that are editable and a set of paths representing the
	 * nodes that are initially checked. if checkboxesOnLeavesOnly is true only
	 * the leaves will have checkboxes, otherwise all nodes at all levels will
	 * have a checkbox except for the root.<br>
	 * NOTE: the setEnabled() method will have no effect on the state of the
	 * checkboxes.
	 * 
	 * @param optionPaths
	 * @param editablePaths
	 * @param initialSelection
	 * @param checkboxesOnLeavesOnly
	 */
	public CheckBoxTreeSetModel(Set<String> optionPaths, Set<String> editablePaths,
			Set<String> initialSelection, boolean checkboxesOnLeavesOnly) {
		this(editablePaths);
		this.checkboxesOnLeavesOnly = checkboxesOnLeavesOnly;
		log.debug("optionPaths = " + optionPaths);
		log.debug("initialSelection = " + initialSelection);
		for (String optionPath : optionPaths) {
			buildPath(optionPath, initialSelection);
		}
	}

	private void buildPath(String optionPath, Set<String> initialSelection) {
		log.debug("build path: " + optionPath);
		DefaultMutableTreeNode parentNode = (DefaultMutableTreeNode) getRoot();

		String pathRoot = "";
		for (String pathSegment : optionPath.split("/")) {
			log.debug("segment: " + pathSegment);
			Enumeration<DefaultMutableTreeNode> childrenEnum = parentNode.children();
			Component nodeComponent = null;
			boolean foundMatch = false;
			while (childrenEnum.hasMoreElements()) {
				DefaultMutableTreeNode node = childrenEnum.nextElement();

				nodeComponent = (Component) node.getUserObject();
				if (nodeComponent instanceof CheckBox) {
					if (((CheckBox) nodeComponent).getText().equals(pathSegment)) {
						log.debug("found existing node for " + pathSegment);
						parentNode = node;
						foundMatch = true;
						break;
					}
				} else if (nodeComponent instanceof Label) {
					if (((Label) nodeComponent).getText().equals(pathSegment)) {
						log.debug("found existing node for " + pathSegment);
						parentNode = node;
						foundMatch = true;
						break;
					}
				}
			}
			if (!foundMatch) {
				if (!checkboxesOnLeavesOnly || optionPath.endsWith(pathSegment)) {
					log.debug("creating checkbox node for " + pathSegment);
					ToggleButtonModel optionModel = new DefaultToggleButtonModel();
					optionModel.setSelected(initialSelection.contains(pathRoot + pathSegment));
					putToggleButtonModel(pathRoot + pathSegment, optionModel);
					nodeComponent = new CheckBox(pathSegment);
					if (editablePaths != null) {
						if (editablePaths.contains(optionPath)) {
							nodeComponent.setEnabled(true);
						} else {
							nodeComponent.setEnabled(false);
						}
					}
					((CheckBox) nodeComponent).setModel(optionModel);
				} else {
					log.debug("creating label node for " + pathSegment);
					nodeComponent = new Label(pathSegment);
				}
				DefaultMutableTreeNode newNode = new DefaultMutableTreeNode(nodeComponent, true);
				insertNodeInto(newNode, parentNode, 0);
				parentNode = newNode;
			}
			pathRoot = pathRoot + pathSegment + "/";
		}
	}

	/**
	 * Add a ToggleButtonModel that holds the state for the specified check
	 * option.
	 * 
	 * @param optionPath
	 * @param model
	 */
	public void putToggleButtonModel(String optionPath, ToggleButtonModel model) {
		log.debug("adding model for path " + optionPath);
		model.addChangeListener(this);
		checkBoxModels.put(optionPath, model);
	}

	/**
	 * Return the underlying ToggleButtonModel for a specific option.
	 * 
	 * @param optionPath
	 * @return
	 */
	public ToggleButtonModel getToggleButtonModel(String optionPath) {
		return checkBoxModels.get(optionPath);
	}

	/**
	 * @param optionPath
	 * @return
	 */
	public boolean isSelected(String optionPath) {
		if (checkBoxModels.containsKey(optionPath)) {
			return checkBoxModels.get(optionPath).isSelected();
		}
		return false;
	}

	/**
	 * @return true if checkboxes are enabled/disabled by specific paths
	 *         specified in the constructor via editablePaths.
	 */
	public boolean isPathLevelEnablement() {
		return (editablePaths != null);
	}

	/**
	 * @param optionPath
	 * @param selected
	 */
	public void setSelected(String optionPath, boolean selected) {
		if (checkBoxModels.containsKey(optionPath)) {
			checkBoxModels.get(optionPath).setSelected(selected);
		}
	}

	/**
	 * Mark all the checkboxes as unchecked.
	 */
	public void clearSelection() {
		for (String optionPath : checkBoxModels.keySet()) {
			if (checkBoxModels.get(optionPath).isSelected()) {
				checkBoxModels.get(optionPath).setSelected(false);
			}
		}
	}

	/**
	 * Mark all the checkboxes as checked.
	 */
	public void selectAll() {
		for (String optionPath : checkBoxModels.keySet()) {
			if (!checkBoxModels.get(optionPath).isSelected()) {
				checkBoxModels.get(optionPath).setSelected(true);
			}
		}
	}

	/**
	 * Return the set of optionPaths
	 * 
	 * @return
	 */
	public Set<String> getOptions() {
		Set<String> options = new HashSet<String>();
		for (String optionPath : checkBoxModels.keySet()) {
			options.add(optionPath);
		}
		return options;
	}

	/**
	 * Return the set of selected options by path.
	 * 
	 * @return
	 */
	public Set<String> getSelectedOptions() {
		Set<String> selectedOptions = new HashSet<String>();
		log.debug("optionPaths: " + checkBoxModels.keySet());

		for (String optionPath : checkBoxModels.keySet()) {
			log.debug("optionPath: " + optionPath);
			if (checkBoxModels.get(optionPath).isSelected()) {
				log.debug("is checked: " + optionPath);
				selectedOptions.add(optionPath);
			}
		}
		return selectedOptions;
	}

	@Override
	public void stateChanged(ChangeEvent e) {
		// TODO: toggle parent on? disable children when parent is off?
	}

	/**
	 * @param l
	 */
	public void addChangeListener(ChangeListener l) {
		changeListeners.add(l);
	}

	/**
	 * @param l
	 */
	public void removeChangeListener(ChangeListener l) {
		changeListeners.remove(l);
	}

	/**
	 * Notifies all listeners that have registered for this event type.
	 */
	public void fireStateChanged() {
		if (!changeListeners.isEmpty()) {
			ChangeEvent e = new ChangeEvent(this);
			for (ChangeListener listener : changeListeners) {
				listener.stateChanged(e);
			}
		}
	}
}
