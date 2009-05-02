/*
 * $Id: NavigatorTable.java,v 1.11 2008/10/11 08:22:32 rregan Exp $
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
package edu.harvard.fas.rregan.uiframework.navigation.table;

import nextapp.echo2.app.Button;
import nextapp.echo2.app.Column;
import nextapp.echo2.app.Extent;
import nextapp.echo2.app.Insets;
import nextapp.echo2.app.Label;
import nextapp.echo2.app.Row;
import nextapp.echo2.app.event.ActionEvent;
import nextapp.echo2.app.event.ActionListener;
import nextapp.echo2.app.list.ListSelectionModel;
import nextapp.echo2.app.table.DefaultTableColumnModel;
import nextapp.echo2.app.table.TableCellRenderer;
import nextapp.echo2.app.table.TableColumn;
import nextapp.echo2.app.table.TableColumnModel;

import org.apache.log4j.Logger;

import echopointng.table.DefaultPageableSortableTableModel;
import echopointng.table.PageableSortableTable;
import echopointng.table.SortableTableColumn;
import echopointng.table.SortableTableHeaderRenderer;
import echopointng.table.SortableTableSelectionModel;
import edu.harvard.fas.rregan.uiframework.panel.Panel;
import edu.harvard.fas.rregan.uiframework.panel.editor.manipulators.ComponentManipulators;
import edu.harvard.fas.rregan.uiframework.panel.editor.manipulators.NavigatorTableManipulator;

/**
 * @author ron
 */
public class NavigatorTable extends Column {
	private static final Logger log = Logger.getLogger(NavigatorTable.class);
	static final long serialVersionUID = 0L;
	static {
		ComponentManipulators.setManipulator(NavigatorTable.class, new NavigatorTableManipulator());
	}

	/**
	 * The style name in the stylesheet for applying a style to the paging
	 * controls for the table.
	 */
	public static final String STYLE_NAME_NAVIGATOR_TABLE_PAGING_CONTROLS = "NavigatorTable.PagingControls";

	/**
	 * The style name in the stylesheet for applying a style to the first page
	 * button for the table.
	 */
	public static final String STYLE_NAME_NAVIGATOR_TABLE_FIRST_PAGE_BUTTON = "NavigatorTable.firstButton";

	/**
	 * The style name in the stylesheet for applying a style to the previous
	 * page button for the table.
	 */
	public static final String STYLE_NAME_NAVIGATOR_TABLE_PREV_PAGE_BUTTON = "NavigatorTable.prevButton";

	/**
	 * The style name in the stylesheet for applying a style to the next page
	 * button for the table.
	 */
	public static final String STYLE_NAME_NAVIGATOR_TABLE_NEXT_PAGE_BUTTON = "NavigatorTable.nextButton";

	/**
	 * The style name in the stylesheet for applying a style to the last page
	 * button for the table.
	 */
	public static final String STYLE_NAME_NAVIGATOR_TABLE_LAST_PAGE_BUTTON = "NavigatorTable.lastButton";

	private NavigatorTableConfig tableConfig;

	private final PageableSortableTable table = new PageableSortableTable();
	private final Button firstButton = new Button();
	private final Button prevButton = new Button();
	private final Button nextButton = new Button();
	private final Button lastButton = new Button();
	private final Label currentLabel = new Label("");
	private int currentPage = 0;

	/**
	 * @param tableConfig -
	 *            the configuration of the columns to display in the table
	 */
	public NavigatorTable(NavigatorTableConfig tableConfig) {
		super();
		setTableConfig(tableConfig);

		TableCellRenderer defaultHeaderRenderer = new SortableTableHeaderRenderer();
		TableCellRenderer defaultRenderer = new OddEvenTableCellRenderer("Table.OddCell",
				"Table.EvenCell");
		TableColumnModel columnModel = new DefaultTableColumnModel();
		int columnIndex = 0;
		for (NavigatorTableColumnConfig columnConfig : tableConfig.getColumnConfigs()) {
			log.debug("column [" + columnIndex + "] " + columnConfig.getTitle());
			TableColumn column = new SortableTableColumn(columnIndex);
			column.setHeaderValue(columnConfig.getTitle());
			column.setModelIndex(columnIndex);
			column.setHeaderRenderer(defaultHeaderRenderer);
			column.setCellRenderer(defaultRenderer);
			columnModel.addColumn(column);
			columnIndex++;
		}

		if (tableConfig.getRowLevelSelection()) {
			table.setSelectionEnabled(true);
		} else {
			table.setSelectionEnabled(false);
		}
		table.setWidth(new Extent(100, Extent.PERCENT));
		table.setAutoCreateColumnsFromModel(false);
		table.setColumnModel(columnModel);
		table.setStyleName(Panel.STYLE_NAME_DEFAULT);
		table.setDefaultHeaderRenderer(defaultHeaderRenderer);
		table.setDefaultRenderer(Object.class, defaultRenderer);
		add(table);
		initPagingButtons();
		Row buttons = new Row();
		buttons.setStyleName(STYLE_NAME_NAVIGATOR_TABLE_PAGING_CONTROLS);

		firstButton.setStyleName(STYLE_NAME_NAVIGATOR_TABLE_FIRST_PAGE_BUTTON);
		prevButton.setStyleName(STYLE_NAME_NAVIGATOR_TABLE_PREV_PAGE_BUTTON);
		currentLabel.setStyleName(Panel.STYLE_NAME_DEFAULT);
		nextButton.setStyleName(STYLE_NAME_NAVIGATOR_TABLE_NEXT_PAGE_BUTTON);
		lastButton.setStyleName(STYLE_NAME_NAVIGATOR_TABLE_LAST_PAGE_BUTTON);
		buttons.add(firstButton);
		buttons.add(prevButton);
		buttons.add(currentLabel);
		buttons.add(nextButton);
		buttons.add(lastButton);
		add(buttons);
		setInsets(new Insets(10));
	}

	/**
	 * @param tableModel
	 */
	public void setModel(NavigatorTableModel tableModel) {
		if (tableModel != null) {
			tableModel.setTableConfig(getTableConfig());
			DefaultPageableSortableTableModel wrapperModel = new DefaultPageableSortableTableModel(
					tableModel);
			wrapperModel.setRowsPerPage(10);
			table.setModel(wrapperModel);
			updatePagingButtons();
			if (getTableConfig().getRowLevelSelection()) {
				SortableTableSelectionModel selectionModel = new SortableTableSelectionModel(
						wrapperModel);
				selectionModel.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
				table.setSelectionModel(selectionModel);
			}
			// fix the paging
			if (((DefaultPageableSortableTableModel) table.getModel()).getTotalPages() <= currentPage) {
				currentPage = ((DefaultPageableSortableTableModel) table.getModel())
						.getTotalPages() - 1;
			}
			((DefaultPageableSortableTableModel) table.getModel()).setCurrentPage(currentPage);
		}
	}

	public NavigatorTableModel getModel() {
		return (NavigatorTableModel) ((DefaultPageableSortableTableModel) table.getModel())
				.getUnderlyingTableModel();
	}

	/**
	 * @return
	 */
	public NavigatorTableConfig getTableConfig() {
		return tableConfig;
	}

	protected void setTableConfig(NavigatorTableConfig tableConfig) {
		this.tableConfig = tableConfig;
	}

	public void addSelectionListener(ActionListener listener) {
		if (tableConfig.getRowLevelSelection()) {
			table.addActionListener(listener);
		} else {
			throw new RuntimeException(
					"Can't add a listener to a table that does not have selection enabled.");
		}
	}

	public void removeSelectionListener(ActionListener listener) {
		table.removeActionListener(listener);
	}

	public Object getSelectedObject() {
		Object selectedObject = null;
		int selectedIndex = table.getSelectionModel().getMinSelectedIndex();
		if (selectedIndex > -1) {
			selectedObject = getModel().getBackingObject(selectedIndex);
		}
		return selectedObject;
	}

	protected void updatePagingButtons() {
		// update the label
		currentLabel.setText(Integer.toString(currentPage + 1)
				+ " of "
				+ Integer.toString((((DefaultPageableSortableTableModel) table.getModel())
						.getTotalPages())));
		if (currentPage == 0) {
			firstButton.setEnabled(false);
			prevButton.setEnabled(false);
		} else {
			firstButton.setEnabled(true);
			prevButton.setEnabled(true);
		}

		if (currentPage == (((DefaultPageableSortableTableModel) table.getModel()).getTotalPages() - 1)) {
			lastButton.setEnabled(false);
			nextButton.setEnabled(false);
		} else {
			lastButton.setEnabled(true);
			nextButton.setEnabled(true);
		}
	}

	protected void initPagingButtons() {
		nextButton.addActionListener(new ActionListener() {
			static final long serialVersionUID = 0L;

			public void actionPerformed(ActionEvent e) {
				currentPage++;
				((DefaultPageableSortableTableModel) table.getModel()).setCurrentPage(currentPage);
				updatePagingButtons();
			}
		});
		prevButton.addActionListener(new ActionListener() {
			static final long serialVersionUID = 0L;

			public void actionPerformed(ActionEvent e) {
				currentPage--;
				((DefaultPageableSortableTableModel) table.getModel()).setCurrentPage(currentPage);
				updatePagingButtons();
			}
		});
		firstButton.addActionListener(new ActionListener() {
			static final long serialVersionUID = 0L;

			public void actionPerformed(ActionEvent e) {
				currentPage = 0;
				((DefaultPageableSortableTableModel) table.getModel()).setCurrentPage(0);
				updatePagingButtons();
			}
		});
		lastButton.addActionListener(new ActionListener() {
			static final long serialVersionUID = 0L;

			public void actionPerformed(ActionEvent e) {
				currentPage = (((DefaultPageableSortableTableModel) table.getModel())
						.getTotalPages() - 1);
				((DefaultPageableSortableTableModel) table.getModel()).setCurrentPage(currentPage);
				updatePagingButtons();
			}
		});
	}
}
