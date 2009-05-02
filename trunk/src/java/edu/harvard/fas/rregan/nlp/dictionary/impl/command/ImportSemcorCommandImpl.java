/*
 * $Id: ImportSemcorCommandImpl.java,v 1.2 2008/12/15 06:35:57 rregan Exp $
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

import java.net.URL;
import java.util.Set;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Controller;

import edu.harvard.fas.rregan.nlp.NLPProcessor;
import edu.harvard.fas.rregan.nlp.NLPText;
import edu.harvard.fas.rregan.nlp.ParseTag;
import edu.harvard.fas.rregan.nlp.PartOfSpeech;
import edu.harvard.fas.rregan.nlp.dictionary.Category;
import edu.harvard.fas.rregan.nlp.dictionary.DictionaryRepository;
import edu.harvard.fas.rregan.nlp.dictionary.SemcorFile;
import edu.harvard.fas.rregan.nlp.dictionary.SemcorSentence;
import edu.harvard.fas.rregan.nlp.dictionary.SemcorSentenceWord;
import edu.harvard.fas.rregan.nlp.dictionary.Sense;
import edu.harvard.fas.rregan.nlp.dictionary.Word;
import edu.harvard.fas.rregan.nlp.dictionary.command.ImportSemcorCommand;
import edu.harvard.fas.rregan.nlp.dictionary.impl.repository.NoSuchWordException;
import edu.harvard.fas.rregan.nlp.impl.NLPTextImpl;
import edu.mit.jsemcor.element.IContext;
import edu.mit.jsemcor.element.IContextID;
import edu.mit.jsemcor.element.ISentence;
import edu.mit.jsemcor.element.IWordform;
import edu.mit.jsemcor.main.IConcordance;
import edu.mit.jsemcor.main.IConcordanceSet;
import edu.mit.jsemcor.main.Semcor;

/**
 * @author ron
 */
@Controller("importSemcorCommand")
@Scope("prototype")
public class ImportSemcorCommandImpl extends AbstractDictionaryCommand implements
		ImportSemcorCommand {

	private final NLPProcessor<NLPText> lemmatizer;

	private URL baseURL;

	/**
	 * @param dictionaryRepository
	 * @param lemmatizer
	 */
	@Autowired
	public ImportSemcorCommandImpl(DictionaryRepository dictionaryRepository,
			@Qualifier("lemmatizer")
			NLPProcessor<NLPText> lemmatizer) {
		super(dictionaryRepository);
		this.lemmatizer = lemmatizer;
	}

	protected NLPProcessor<NLPText> getLemmatizer() {
		return lemmatizer;
	}

	protected URL getBaseURL() {
		return baseURL;
	}

	public void setBaseURL(URL baseURL) {
		this.baseURL = baseURL;
	}

	protected Sense getSense(String lemma, String parseTag, int senseRank)
			throws NoSuchWordException {
		PartOfSpeech pos = ParseTag.valueOf(parseTag).getPartOfSpeech();
		try {
			// the SemCor lemmas aren't always the lemma
			NLPText nlpLemma = getLemmatizer().process(new NLPTextImpl(lemma, pos));

			Word dictionaryWord = getDictionaryRepository().findWordExact(nlpLemma.getLemma(), pos);
			Set<Sense> posSenses = dictionaryWord.getSenses(pos);
			if (senseRank == 0) {
				if (posSenses.size() == 1) {
					return posSenses.iterator().next();
				}
				// senseRank of 0 may mean no wordnet sense?
				return null;
			}
			for (Sense sense : posSenses) {
				if (sense.getRank() == senseRank) {
					return sense;
				}
			}
		} catch (NoSuchWordException e) {
			try {
				if (lemma.contains("_")) {
					return getSense(lemma.replace('_', ' '), parseTag, senseRank);
				}
			} catch (Exception ee) {
				log.error(e);
			}
		}
		return null;
	}

	/**
	 * @see edu.harvard.fas.rregan.command.Command#execute()
	 */
	@Override
	public void execute() {
		try {
			// construct the semcor object and open it
			IConcordanceSet semcor = new Semcor(getBaseURL());
			semcor.open();

			for (IConcordance section : semcor.values()) {
				for (IContextID id : section.getContextIDs()) {
					IContext context = section.getContext(id);
					loadSemcorFile(section, context);
				}
			}
		} catch (Exception e) {
			log.error(e, e);
		}
	}

	protected void loadSemcorFile(IConcordance section, IContext context) {
		SemcorFile file = getDictionaryRepository().persist(
				new SemcorFile(section.getName(), context.getFilename()));
		for (ISentence sentence : context.getSentences()) {
			SemcorSentence semSentence = getDictionaryRepository().persist(
					new SemcorSentence(file, new Long(sentence.getNumber())));
			for (IWordform wordform : sentence.getWordList()) {
				if (log.isDebugEnabled()) {
					log.debug("section: " + section.getName());
					log.debug("context: " + context.getFilename());
					if (wordform.getCommand() != null) {
						log.debug("command: " + wordform.getCommand().getAttribute() + " "
								+ wordform.getCommand().getData() + " "
								+ wordform.getCommand().getMeaning() + " "
								+ wordform.getCommand().getValue());
					}
					log.debug("data: " + wordform.getData());
					log.debug("distance: " + wordform.getDistance());
					log.debug("number: " + wordform.getNumber());
					log.debug("redefinition: " + wordform.getRedefinition());
					log.debug("separator string: " + wordform.getSeparatorString());
					log.debug("text: " + wordform.getText());
					log.debug("TokenIndex: " + wordform.getTokenIndex());
					log.debug("ConstituentTokens: " + wordform.getConstituentTokens());
					if (wordform.getOtherTag() != null) {
						log.debug("OtherTag: " + wordform.getOtherTag().getAttribute() + " "
								+ wordform.getOtherTag().getData() + " "
								+ wordform.getOtherTag().getValue());
					}
					if (wordform.getPOSTag() != null) {
						log.debug("POSTag: " + wordform.getPOSTag().getAttribute() + " "
								+ wordform.getPOSTag().getData() + " "
								+ wordform.getPOSTag().getMeaning() + " "
								+ wordform.getPOSTag().getValue());
					}
					if (wordform.getSemanticTag() != null) {
						log.debug("SemanticTag: " + wordform.getSemanticTag().getLemma() + " "
								+ wordform.getSemanticTag().getData() + " {"
								+ wordform.getSemanticTag().getLexicalSense() + "} "
								+ wordform.getSemanticTag().getProperNounCategory() + " {"
								+ wordform.getSemanticTag().getSenseNumber() + "} ");
					}
				}
				ParseTag parseTag;
				try {
					parseTag = ParseTag.valueOf(wordform.getPOSTag().getValue());
				} catch (Exception e) {
					parseTag = ParseTag.X;
					log.warn("semcor pos tag error for " + section.getName() + " "
							+ context.getFilename() + "sentence " + semSentence.getSentenceIndex()
							+ " word " + wordform.getNumber() + " " + wordform.getText() + ": "
							+ wordform.getPOSTag().getValue());
				}
				SemcorSentenceWord semWord = null;
				if (wordform.getSemanticTag() == null) {
					semWord = new SemcorSentenceWord(semSentence, wordform.getNumber(), wordform
							.getText(), parseTag);
				} else if (wordform.getSemanticTag().getProperNounCategory() != null) {
					String categorySimpleName = wordform.getSemanticTag().getProperNounCategory()
							.getValue();
					Category properNounCategory = getDictionaryRepository().findCategory(
							parseTag.getPartOfSpeech(), categorySimpleName);
					Sense sense = getSense(wordform.getSemanticTag().getLemma(), wordform
							.getPOSTag().getValue(), wordform.getSemanticTag().getSenseNumber()
							.get(0));
					semWord = new SemcorSentenceWord(semSentence, wordform.getNumber(), wordform
							.getText(), ParseTag.valueOf(wordform.getPOSTag().getValue()), sense,
							properNounCategory);
				} else {
					Sense sense = getSense(wordform.getSemanticTag().getLemma(), wordform
							.getPOSTag().getValue(), wordform.getSemanticTag().getSenseNumber()
							.get(0));
					if (wordform.getRedefinition() != null) {
						// there could be a typo in the text, replace it with
						// the redefined value
						semWord = new SemcorSentenceWord(semSentence, wordform.getNumber(),
								wordform.getRedefinition(), ParseTag.valueOf(wordform.getPOSTag()
										.getValue()), sense);
					} else {
						semWord = new SemcorSentenceWord(semSentence, wordform.getNumber(),
								wordform.getText(), ParseTag.valueOf(wordform.getPOSTag()
										.getValue()), sense);
					}
				}
				semWord = getDictionaryRepository().persist(semWord);
				semSentence.getWords().add(semWord);
			}
		}
	}
}
