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
package edu.harvard.fas.rregan.uiframework.navigation;

import nextapp.echo2.app.Button;
import nextapp.echo2.app.event.ActionEvent;
import nextapp.echo2.app.event.ActionListener;
import nextapp.echo2.app.filetransfer.Download;
import nextapp.echo2.app.filetransfer.DownloadProvider;

import org.apache.log4j.Logger;

/**
 * A generic button for sending content to a user as a downloadable file.
 * 
 * @author ron
 */
public class DownloadButton extends Button implements ActionListener {
	private static final Logger log = Logger.getLogger(DownloadButton.class);
	static final long serialVersionUID = 0;

	private DownloadProvider downloadProvider;

	/**
	 * @param label -
	 *            the label for the button
	 * @param downloadProvider -
	 *            the provider of the content to download
	 */
	public DownloadButton(String label, DownloadProvider downloadProvider) {
		super(label);
		setDownloadProvider(downloadProvider);
		addActionListener(this);
	}

	/**
	 * @param label
	 */
	public DownloadButton(String label) {
		this(label, null);
	}

	/**
	 * @return
	 */
	public DownloadProvider getDownloadProvider() {
		return downloadProvider;
	}

	/**
	 * @param downloadProvider
	 */
	public void setDownloadProvider(DownloadProvider downloadProvider) {
		this.downloadProvider = downloadProvider;
	}

	public void actionPerformed(ActionEvent e) {
		Download download = new Download();
		download.setProvider(downloadProvider);
		download.setActive(true);
		getApplicationInstance().enqueueCommand(download);
	}

}
