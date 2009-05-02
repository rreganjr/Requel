/*
 * $Id: LoadWordNetTaggedGlossesCommandImpl.java,v 1.1 2008/12/13 00:40:00 rregan Exp $
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
package edu.harvard.fas.rregan.nlp.dictionary.impl.command;

import java.io.InputStream;

import org.apache.commons.digester.Digester;
import org.apache.commons.digester.Rule;
import org.apache.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Controller;
import org.xml.sax.Attributes;
import org.xml.sax.ErrorHandler;
import org.xml.sax.SAXException;
import org.xml.sax.SAXParseException;

import edu.harvard.fas.rregan.command.BatchCommand;
import edu.harvard.fas.rregan.command.CommandHandler;
import edu.harvard.fas.rregan.nlp.ParseTag;
import edu.harvard.fas.rregan.nlp.dictionary.DictionaryRepository;
import edu.harvard.fas.rregan.nlp.dictionary.Synset;
import edu.harvard.fas.rregan.nlp.dictionary.SynsetDefinitionWord;
import edu.harvard.fas.rregan.nlp.dictionary.command.DictionaryCommandFactory;
import edu.harvard.fas.rregan.nlp.dictionary.command.EditSynsetDefinitionWordCommand;
import edu.harvard.fas.rregan.nlp.dictionary.command.LoadWordNetTaggedGlossesCommand;
import edu.harvard.fas.rregan.requel.NoSuchEntityException;

/**
 * An XML processor for the WordNet synset merged gloss parse and word sense
 * files.<br>
 * The processor parses the xml and adds {@link SynsetDefinitionWord} to the
 * {@link Synset}s<br>
 * 
 * @see {http://wordnet.princeton.edu/glosstag-files/}
 * @author ron
 */
@Controller("wordNetTaggedGlossaryDigester")
@Scope("prototype")
public class LoadWordNetTaggedGlossesCommandImpl extends Digester implements
		LoadWordNetTaggedGlossesCommand {
	private static final Logger log = Logger.getLogger(LoadWordNetTaggedGlossesCommandImpl.class);

	private final DictionaryRepository dictionaryRepository;
	private final DictionaryCommandFactory dictionaryCommandFactory;
	private final CommandHandler commandHandler;

	private InputStream inputStream;

	/**
	 * @param dictionaryRepository
	 * @param dictionaryCommandFactory
	 * @param commandHandler
	 */
	@Autowired
	public LoadWordNetTaggedGlossesCommandImpl(DictionaryRepository dictionaryRepository,
			DictionaryCommandFactory dictionaryCommandFactory, CommandHandler commandHandler) {
		this.dictionaryRepository = dictionaryRepository;
		this.dictionaryCommandFactory = dictionaryCommandFactory;
		this.commandHandler = commandHandler;

		addRule("*/synset", new DigesterRuleLoggingDecorator(new SynsetXMLDigesterRule()));
		addRule("*/synset/gloss/def/wf", new DigesterRuleLoggingDecorator(
				new DefinitionWordFormXMLDigesterRule()));
		addRule("*/synset/gloss/def/wf/id", new DigesterRuleLoggingDecorator(
				new WordFormIdXMLDigesterRule()));
		addRule("*/synset/gloss/def/cf", new DigesterRuleLoggingDecorator(
				new DefinitionColocationFormXMLDigesterRule()));
		addRule("*/synset/gloss/def/cf/id", new DigesterRuleLoggingDecorator(
				new WordFormIdXMLDigesterRule()));
	}

	@Override
	public void setInputStream(InputStream inputStream) {
		this.inputStream = inputStream;
	}

	@Override
	public void execute() throws Exception {
		try {
			// set a parser error handler
			getParser().getXMLReader().setErrorHandler(new TheErrorHandler());
		} catch (Exception e) {
			log.warn("Could not set ErrorHandler for logging of xml parse errors: " + e, e);
		}
		log.debug("Starting parsing.");
		super.parse(inputStream);
		log.debug("Ending parsing.");
	}

	/**
	 * @return
	 */
	protected DictionaryRepository getDictionaryRepository() {
		return dictionaryRepository;
	}

	protected DictionaryCommandFactory getDictionaryCommandFactory() {
		return dictionaryCommandFactory;
	}

	protected CommandHandler getCommandHandler() {
		return commandHandler;
	}

	/**
	 * @param ofs
	 * @param pos
	 * @return
	 */
	protected Synset getSynset(String ofs, String pos) {
		return getDictionaryRepository().findSynset(Long.parseLong(ofs) + posToIdBase(pos));
	}

	/**
	 * @param pos
	 * @return the base value to add to the synset ofs to get the id of the
	 *         synset.
	 */
	protected long posToIdBase(String pos) {
		if ("n".equals(pos)) {
			return 100000000;
		} else if ("v".equals(pos)) {
			return 200000000;
		} else if ("a".equals(pos)) {
			return 300000000;
		} else if ("s".equals(pos)) {
			return 300000000;
		} else if ("r".equals(pos)) {
			return 400000000;
		}
		throw new RuntimeException("unexpected pos " + pos);
	}

	/**
	 * The base class for all rules used by this digester.
	 */
	public static abstract class WordNetTaggedGlossaryDigesterRule extends Rule {

		/**
		 * @return the digester instance that called this rule
		 */
		@Override
		public LoadWordNetTaggedGlossesCommandImpl getDigester() {
			return (LoadWordNetTaggedGlossesCommandImpl) super.getDigester();
		}
	}

	private static class SynsetXMLData {
		final private Synset synset;
		private BatchCommand batchCommand;
		String currentColocationId = null;

		protected SynsetXMLData(Synset synset) {
			this.synset = synset;
		}
	}

	private static class SynsetXMLDigesterRule extends WordNetTaggedGlossaryDigesterRule {

		@Override
		public void begin(String elementNamespace, String elementName, Attributes attributes)
				throws Exception {
			final String ofs = attributes.getValue("ofs");
			final String pos = attributes.getValue("pos");
			SynsetXMLData data = new SynsetXMLData(getDigester().getSynset(ofs, pos));
			data.batchCommand = getDigester().getDictionaryCommandFactory().newBatchCommand();
			getDigester().push(data);
		}

		@Override
		public void end(String elementNamespace, String elementName) throws Exception {
			SynsetXMLData data = (SynsetXMLData) getDigester().pop();
			getDigester().getCommandHandler().execute(data.batchCommand);
		}
	}

	private static class DefinitionWordFormXMLDigesterRule extends
			WordNetTaggedGlossaryDigesterRule {

		@Override
		public void begin(String elementNamespace, String elementName, Attributes attributes)
				throws Exception {
			final String pos = attributes.getValue("pos");
			final String type = attributes.getValue("type");
			SynsetXMLData data = (SynsetXMLData) getDigester().peek();
			if (data.currentColocationId != null) {
				// a word form ends the last colocation in this synset.
				data.currentColocationId = null;
			}

			EditSynsetDefinitionWordCommand command = getDigester().getDictionaryCommandFactory()
					.newEditSynsetDefinitionWordCommand();
			command.setIndex(data.batchCommand.getCommands().size());
			if ("punc".equals(type)) {
				command.setParseTag(ParseTag.PUNC_NON_TERMINATOR);
			} else if ("symb".equals(type)) {
				command.setParseTag(ParseTag.SYM);
			} else {
				command.setParseTag(ParseTag.tagOf(pos));
			}
			command.setSynset(data.synset);
			getDigester().push(command);
		}

		@Override
		public void body(String namespace, String name, String text) throws Exception {
			// a colocation element may be ignored if so the top of the stack
			// won't be a command
			if (getDigester().peek() instanceof EditSynsetDefinitionWordCommand) {
				EditSynsetDefinitionWordCommand command = (EditSynsetDefinitionWordCommand) getDigester()
						.peek();
				command.setText(text.trim());
			}
		}

		@Override
		public void end(String elementNamespace, String elementName) throws Exception {
			// a colocation element may be ignored if so the top of the stack
			// won't be a command
			if (getDigester().peek() instanceof EditSynsetDefinitionWordCommand) {
				EditSynsetDefinitionWordCommand command = (EditSynsetDefinitionWordCommand) getDigester()
						.pop();
				SynsetXMLData data = (SynsetXMLData) getDigester().peek();
				data.batchCommand.addCommand(command);
			}
		}
	}

	private static class DefinitionColocationFormXMLDigesterRule extends
			DefinitionWordFormXMLDigesterRule {

		@Override
		public void begin(String elementNamespace, String elementName, Attributes attributes)
				throws Exception {
			final String pos = attributes.getValue("pos");
			final String colocationId = attributes.getValue("coll");
			SynsetXMLData data = (SynsetXMLData) getDigester().peek();
			if (colocationId.equals(data.currentColocationId)) {
				// the primary element of the colocation was already found,
				// ignore the rest of the words as the primary colocation
				// element accounts for all of the words.
			} else {
				// the primary element of the colocation
				data.currentColocationId = colocationId;
				EditSynsetDefinitionWordCommand command = getDigester()
						.getDictionaryCommandFactory().newEditSynsetDefinitionWordCommand();
				command.setIndex(data.batchCommand.getCommands().size());
				command.setParseTag(ParseTag.tagOf(pos));
				command.setSynset(data.synset);
				getDigester().push(command);
			}
		}
	}

	private static class WordFormIdXMLDigesterRule extends WordNetTaggedGlossaryDigesterRule {

		@Override
		public void begin(String elementNamespace, String elementName, Attributes attributes)
				throws Exception {
			final String senseKey = attributes.getValue("sk");
			EditSynsetDefinitionWordCommand command = (EditSynsetDefinitionWordCommand) getDigester()
					.peek();
			try {
				command.setSense(getDigester().getDictionaryRepository()
						.findSensesByWordnetSenseKey(senseKey));
			} catch (NoSuchEntityException e) {
				log.warn("no sense for key " + senseKey);
			}
		}
	}

	private static class TheErrorHandler implements ErrorHandler {
		private static final Logger log = Logger
				.getLogger(LoadWordNetTaggedGlossesCommandImpl.class);

		protected TheErrorHandler() {
			super();
		}

		public void error(SAXParseException exception) throws SAXException {
			log.error(exception, exception);
		}

		public void fatalError(SAXParseException exception) throws SAXException {
			log.fatal(exception, exception);
		}

		public void warning(SAXParseException exception) throws SAXException {
			log.warn(exception, exception);
		}
	}
}
