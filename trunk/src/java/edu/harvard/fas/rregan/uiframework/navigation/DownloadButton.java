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

	@Override
	public void actionPerformed(ActionEvent e) {
		Download download = new Download();
		download.setProvider(downloadProvider);
		download.setActive(true);
		getApplicationInstance().enqueueCommand(download);
	}

}
