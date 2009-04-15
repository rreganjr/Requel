/*
 * $Id: ParserPanel.java,v 1.3 2008/12/17 08:38:05 rregan Exp $
 * Copyright (c) 2008 Ron Regan Jr. All Rights Reserved.
 */
package edu.harvard.fas.rregan.nlp.ui;

import nextapp.echo2.app.Button;
import nextapp.echo2.app.TextArea;
import nextapp.echo2.app.event.ActionEvent;
import nextapp.echo2.app.event.ActionListener;

import org.apache.log4j.Logger;

import echopointng.text.StringDocumentEx;
import edu.harvard.fas.rregan.nlp.NLPProcessorFactory;
import edu.harvard.fas.rregan.nlp.NLPText;
import edu.harvard.fas.rregan.uiframework.panel.editor.AbstractEditorPanel;

/**
 * @author ron
 */
public class ParserPanel extends AbstractEditorPanel {
	private static final Logger log = Logger.getLogger(ParserPanel.class);

	static final long serialVersionUID = 0L;

	public static final String PROP_LABEL_TEXT = "Text.Label";
	public static final String PROP_LABEL_OUTPUT = "Output.Label";
	public static final String PROP_LABEL_PARSE_BUTTON = "ParseButton.Label";

	private final NLPProcessorFactory processorFactory;
	private Button parseButton;

	/**
	 * @param commandHandler
	 * @param projectCommandFactory
	 * @param projectRepository
	 */
	public ParserPanel(NLPProcessorFactory processorFactory) {
		this(ParserPanel.class.getName(), processorFactory);
	}

	/**
	 * @param resourceBundleName
	 * @param processorFactory
	 */
	public ParserPanel(String resourceBundleName, NLPProcessorFactory processorFactory) {
		super(resourceBundleName, null, NLPPanelNames.PARSER_PANEL_NAME);
		this.processorFactory = processorFactory;
	}

	/**
	 * @see edu.harvard.fas.rregan.uiframework.panel.AbstractPanel#getTitle()
	 */
	@Override
	public String getTitle() {
		String msg = getResourceBundleHelper(getLocale()).getString(PROP_PANEL_TITLE,
				"Parser Panel");
		return msg;
	}

	@Override
	public void setup() {
		super.setup();
		addInput("text", PROP_LABEL_TEXT, "Text", new TextArea(), new StringDocumentEx());
		addInput("output", PROP_LABEL_OUTPUT, "Output", new TextArea(), new StringDocumentEx());
		parseButton = addActionButton(new Button(getResourceBundleHelper(getLocale()).getString(
				PROP_LABEL_PARSE_BUTTON, "Parse")));
		parseButton.addActionListener(new ParseListener(this));
		parseButton.setEnabled(true);
	}

	@Override
	public void dispose() {
		super.dispose();
		removeAll();
	}

	@Override
	public boolean isReadOnlyMode() {
		return false;
	}

	@Override
	public void cancel() {
		super.cancel();
	}

	@Override
	public void save() {
		super.save();
	}

	protected NLPProcessorFactory getProcessorFactory() {
		return processorFactory;
	}

	private static class ParseListener implements ActionListener {
		static final long serialVersionUID = 0L;

		private final ParserPanel panel;

		private ParseListener(ParserPanel panel) {
			this.panel = panel;
		}

		@Override
		public void actionPerformed(ActionEvent event) {
			try {
				String text = panel.getInputValue("text", String.class);
				NLPText nlpText = panel.getProcessorFactory().createNLPText(text);
				panel.getProcessorFactory().getSentencizer().process(nlpText);
				panel.getProcessorFactory().getParser().process(nlpText);
				String output = panel.getProcessorFactory().getConstituentTreePrinter().process(
						nlpText);
				panel.setInputValue("output", output);
			} catch (Exception e) {
				panel.setGeneralMessage("Could not parse text: " + e);
			}
		}
	}

}
