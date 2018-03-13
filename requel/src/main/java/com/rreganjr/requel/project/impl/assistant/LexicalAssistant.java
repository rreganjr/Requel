/*
 * $Id$
 * Copyright 2008, 2009 Ron Regan Jr. All Rights Reserved.
 * This file is part of Requel - the Collaborative Requirements
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
package com.rreganjr.requel.project.impl.assistant;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.rreganjr.command.CommandHandler;
import com.rreganjr.nlp.GrammaticalStructureLevel;
import com.rreganjr.nlp.NLPProcessor;
import com.rreganjr.nlp.NLPProcessorFactory;
import com.rreganjr.nlp.NLPText;
import com.rreganjr.nlp.ParseTag;
import com.rreganjr.nlp.PartOfSpeech;
import com.rreganjr.nlp.dictionary.DictionaryRepository;
import com.rreganjr.nlp.dictionary.Linkdef;
import com.rreganjr.nlp.dictionary.Sense;
import com.rreganjr.requel.annotation.AnnotationRepository;
import com.rreganjr.requel.annotation.Issue;
import com.rreganjr.requel.annotation.NoSuchAnnotationException;
import com.rreganjr.requel.annotation.Note;
import com.rreganjr.requel.annotation.Position;
import com.rreganjr.requel.annotation.command.AnnotationCommandFactory;
import com.rreganjr.requel.annotation.command.EditAddWordToDictionaryPositionCommand;
import com.rreganjr.requel.annotation.command.EditChangeSpellingPositionCommand;
import com.rreganjr.requel.annotation.command.EditLexicalIssueCommand;
import com.rreganjr.requel.annotation.impl.LexicalIssue;
import com.rreganjr.requel.project.GlossaryTerm;
import com.rreganjr.requel.project.ProjectOrDomain;
import com.rreganjr.requel.project.ProjectOrDomainEntity;
import com.rreganjr.requel.project.ProjectRepository;
import com.rreganjr.requel.project.command.EditAddActorToProjectPositionCommand;
import com.rreganjr.requel.project.command.EditAddWordToGlossaryPositionCommand;
import com.rreganjr.requel.project.command.EditGlossaryTermCommand;
import com.rreganjr.requel.project.command.ProjectCommandFactory;
import com.rreganjr.requel.project.command.RemoveUnneedLexicalIssuesCommand;
import com.rreganjr.requel.project.exception.NoSuchActorException;
import com.rreganjr.requel.project.exception.NoSuchGlossaryTermException;
import com.rreganjr.requel.user.User;

/**
 * The lexical assistant checks spelling and identifies noun phrases that may be
 * glossary terms, actors, or entities in the domain model of a project.<br>
 * When a word in the text is not in the dictionary a spelling issue is added to
 * the object being analyzed with positions suggesting an alternate spelling and
 * a position indicating the word should be added to the dictionary.<br>
 * Phrases identified as possible glossary terms, actors or domain objects are
 * filtered by the Information Content and categories of the words in the
 * phrase.
 * 
 * @author ron
 */
public class LexicalAssistant extends AbstractAssistant {
	private static final Log log = LogFactory.getLog(LexicalAssistant.class);

	// spelling issues
	/**
	 * The name of the property in the LexicalAssistant.properties file for the
	 * issue text indicating a word is not known in the dictionary.
	 */
	public static final String PROP_UKNOWN_WORD_MSG = "UnknownWordMessage";
	public static final String PROP_UKNOWN_WORD_MSG_DEFAULT = "The word \"{0}\" in the {1} is not recognized and may be spelled incorrectly.";

	/**
	 * The name of the property in the LexicalAssistant.properties file for the
	 * position text indicating the word should be ignored.
	 */
	public static final String PROP_IGNORE_WORD_MSG = "IgnoreWordMessage";
	public static final String PROP_IGNORE_WORD_MSG_DEFAULT = "Ignore this word.";

	/**
	 * The name of the property in the LexicalAssistant.properties file for the
	 * position text indicating the word should be added to the system
	 * dictionary.
	 */
	public static final String PROP_ADD_TO_DICTIONARY_MSG = "AddToDictionaryMessage";
	public static final String PROP_ADD_TO_DICTIONARY_MSG_DEFAULT = "Add \"{0}\" to the dictionary.";

	/**
	 * The name of the property in the LexicalAssistant.properties file for the
	 * position text indicating the word should be replaced by an alternate
	 * spelling. This message takes two parameters. The original word and the
	 * suggested replacement word.
	 */
	public static final String PROP_SUGGESTED_SPELLING_MSG = "SuggestedSpellingMessage";
	public static final String PROP_SUGGESTED_SPELLING_MSG_DEFAULT = "Change the word \"{0}\" to \"{1}\".";

	// term, actor, domain phrase issues and position text

	/**
	 * The name of the property in the LexicalAssistant.properties file for the
	 * issue text indicating a phrase may be a glossary term, actor or domain
	 * object/property. The message contains a single parameter for the phrase.
	 */
	public static final String PROP_TERM_ACTOR_DOMAIN_MSG = "TermActorDomainObjectMessage";
	public static final String PROP_TERM_ACTOR_DOMAIN_MSG_DEFAULT = "The phrase \"{0}\" is a potential glossary term, actor, or domain object/property";

	/**
	 * The name of the property in the LexicalAssistant.properties file for the
	 * issue text indicating a phrase may be a glossary term or domain
	 * object/property. The message contains a single parameter for the phrase.
	 */
	public static final String PROP_TERM_DOMAIN_MSG = "TermDomainObjectMessage";
	public static final String PROP_TERM_DOMAIN_MSG_DEFAULT = "The phrase \"{0}\" is a potential glossary term, or domain object/property.";

	/**
	 * The name of the property in the LexicalAssistant.properties file for the
	 * position text indicating to ignore the phrase.
	 */
	public static final String PROP_IGNORE_PHRASE_MSG = "IgnorePhraseMessage";
	public static final String PROP_IGNORE_PHRASE_MSG_DEFAULT = "Ignore this phrase.";

	/**
	 * The name of the property in the LexicalAssistant.properties file for the
	 * position text indicating to add the phrase to the project glossary.
	 */
	public static final String PROP_ADD_TO_GLOSSARY_MSG = "AddToGlossary";
	public static final String PROP_ADD_TO_GLOSSARY_MSG_DEFAULT = "Add \"{0}\" to the project glossary.";

	/**
	 * The name of the property in the LexicalAssistant.properties file for the
	 * position text indicating to add the phrase as an actor to the project.
	 */
	public static final String PROP_ADD_AS_ACTOR_MSG = "AddAsActor";
	public static final String PROP_ADD_AS_ACTOR_MSG_DEFAULT = "Add \"{0}\" as an actor to the project.";

	/**
	 * The name of the property in the LexicalAssistant.properties file for the
	 * note text indicating the use of a word sense by one of the words in the
	 * processed text.
	 */
	public static final String PROP_SENSE_USE_MSG = "SenseUseMessage";
	public static final String PROP_SENSE_USE_MSG_DEFAULT = "The word \"{0}\" was guessed to be the sense \"{1}\" meaning \"{2}\"";

	/**
	 * The name of the property in the LexicalAssistant.properties file for the
	 * note text indicating the use of a word sense by multiple words in the
	 * processed text.
	 */
	public static final String PROP_MULTI_SENSE_USE_MSG = "MultiSenseUseMessage";
	public static final String PROP_MULTI_SENSE_USE_MSG_DEFAULT = "The words \"{0}\" were guessed to be the sense \"{1}\" meaning \"{2}\"";

	// complex sentences
	/**
	 * The name of the property in the LexicalAssistant.properties file for the
	 * issue text indicating a sentence may be overly complex.
	 */
	public static final String PROP_COMPLEX_TEXT_MSG = "ComplexTextMessage";
	public static final String PROP_COMPLEX_TEXT_MSG_DEFAULT = "The text \"{0}\" in the {1} is complex and may be hard to understand.";

	/**
	 * The name of the property in the LexicalAssistant.properties file for the
	 * syntax tree depth complexity setting. Text with a deeper syntax level
	 * will be annotated with an issue that the text may be overly complex.
	 */
	public static final String PROP_COMPLEXITY_DEPTH = "ComplexityDepth";
	public static final int PROP_COMPLEXITY_DEPTH_DEFAULT = 12;

	// vague word use
	/**
	 * The name of the property in the LexicalAssistant.properties file for the
	 * issue text indicating a word may be overly vague.
	 */
	public static final String PROP_VAGUE_WORD_MSG = "VagueWordMessage";
	public static final String PROP_VAGUE_WORD_MSG_DEFAULT = "The word \"{0}\" in the {1} is vague and may lead to ambiguity.";

	/**
	 * The name of the property in the LexicalAssistant.properties file for the
	 * information content threshold for detecting vague words.
	 */
	public static final String PROP_INFO_CONTENT_THRESHOLD = "InfoContentThreshold";
	public static final double PROP_INFO_CONTENT_THRESHOLD_DEFAULT = 0.50;

	/**
	 * The name of the property in the LexicalAssistant.properties file for the
	 * position text indicating an alternate more specific word.
	 */
	public static final String PROP_SUGGESTED_MORE_SPECIFIC_WORD_MSG = "SuggestedMoreSpecificWordMessage";
	public static final String PROP_SUGGESTED_MORE_SPECIFIC_WORD_MSG_DEFAULT = "Change the word \"{0}\" to \"{1}\".";

	private final DictionaryRepository dictionaryRepository;
	private final ProjectCommandFactory projectCommandFactory;
	private final ProjectRepository projectRepository;
	private final NLPProcessorFactory nlpProcessorFactory;

	/**
	 * @param commandHandler -
	 *            handler for executing annotation commands.
	 * @param projectCommandFactory
	 * @param annotationCommandFactory -
	 *            factory for creating commands to add annotations to the goal.
	 * @param annotationRepository
	 * @param projectRepository
	 * @param dictionaryRepository
	 * @param nlpProcessorFactory -
	 *            natural language processing factory.
	 */
	public LexicalAssistant(CommandHandler commandHandler,
			ProjectCommandFactory projectCommandFactory,
			AnnotationCommandFactory annotationCommandFactory,
			AnnotationRepository annotationRepository, ProjectRepository projectRepository,
			DictionaryRepository dictionaryRepository, NLPProcessorFactory nlpProcessorFactory) {
		super(LexicalAssistant.class.getName(), commandHandler, annotationCommandFactory,
				annotationRepository);
		this.projectCommandFactory = projectCommandFactory;
		this.projectRepository = projectRepository;
		this.nlpProcessorFactory = nlpProcessorFactory;
		this.dictionaryRepository = dictionaryRepository;
	}

	protected NLPProcessorFactory getNLPProcessorFactory() {
		return nlpProcessorFactory;
	}

	protected ProjectCommandFactory getProjectCommandFactory() {
		return projectCommandFactory;
	}

	protected ProjectRepository getProjectRepository() {
		return projectRepository;
	}

	protected DictionaryRepository getDictionaryRepository() {
		return dictionaryRepository;
	}

	/**
	 * Analyze the given text for complexity.
	 * 
	 * @param assistantUser -
	 *            the user to mark as the creator of annotations
	 * @param projectOrDomain
	 * @param thingBeingAnalyzed
	 * @param annotatableEntityPropertyName -
	 *            The name of the text property in the thing being analyzed of
	 *            the supplied nlpText.
	 * @param nlpText -
	 *            the text to be analyzed in an NLP Text format, already
	 *            processed.
	 * @throws Exception
	 */
	public void findPotentialComplexSentences(User assistantUser, ProjectOrDomain projectOrDomain,
			ProjectOrDomainEntity thingBeingAnalyzed, String annotatableEntityPropertyName,
			NLPText nlpText) throws Exception {

		if (nlpText.is(GrammaticalStructureLevel.PARAGRAPH)) {
			for (NLPText sentence : nlpText.getChildren()) {
				findPotentialComplexSentences(assistantUser, projectOrDomain, thingBeingAnalyzed,
						annotatableEntityPropertyName, sentence);
			}
		} else if (nlpText.is(GrammaticalStructureLevel.SENTENCE)) {
			Integer maxDepth = getResourceBundleHelper().getInteger(PROP_COMPLEXITY_DEPTH,
					PROP_COMPLEXITY_DEPTH_DEFAULT);
			NLPProcessor<Integer> depthFinder = getNLPProcessorFactory()
					.getConstituentTreeDepthFinder();

			Integer depth = depthFinder.process(nlpText);
			if (depth > maxDepth) {
				addComplexityIssue(projectOrDomain, assistantUser, thingBeingAnalyzed, nlpText,
						annotatableEntityPropertyName);
			}
		}
	}

	/**
	 * Analyze the given text for possible glossary terms. If any are found add
	 * issues to the supplied "thingBeingAnalyzed".
	 * 
	 * @param assistantUser -
	 *            the user to mark as the creator of annotations
	 * @param projectOrDomain
	 * @param thingBeingAnalyzed
	 * @param nlpText -
	 *            the text to be analyzed in an NLP Text format, already
	 *            processed.
	 * @throws Exception
	 */
	public void findPossibleGlossaryTerms(User assistantUser, ProjectOrDomain projectOrDomain,
			ProjectOrDomainEntity thingBeingAnalyzed, NLPText nlpText) throws Exception {

		// TODO: adding a note is for checking the nlp processing and should be
		// removed or made configurable
		addNLPNote(projectOrDomain, assistantUser, thingBeingAnalyzed, "Phrase Structure:\n"
				+ getNLPProcessorFactory().getConstituentTreePrinter().process(nlpText));
		addNLPNote(projectOrDomain, assistantUser, thingBeingAnalyzed, "Syntax Dependencies:\n"
				+ getNLPProcessorFactory().getDependencyPrinter().process(nlpText));
		addNLPNote(projectOrDomain, assistantUser, thingBeingAnalyzed, "Semantic Roles:\n"
				+ getNLPProcessorFactory().getSemanticRolePrinter().process(nlpText));

		// add a note for each word sense used by which words
		Map<Sense, List<NLPText>> senseMap = new HashMap<Sense, List<NLPText>>(nlpText.getLeaves()
				.size());
		for (NLPText nlpWord : nlpText.getLeaves()) {
			Sense sense = nlpWord.getDictionaryWordSense();
			if (sense != null) {
				List<NLPText> senseUse = senseMap.get(sense);
				if (senseUse == null) {
					senseUse = new ArrayList<NLPText>(5);
				}
				senseUse.add(nlpWord);
			}
		}
		for (Sense sense : senseMap.keySet()) {
			List<NLPText> words = senseMap.get(sense);
			if (words.size() > 1) {
				StringBuilder wordsString = new StringBuilder();
				wordsString.append(words.get(0).getText());
				for (NLPText word : words.subList(1, words.size())) {
					wordsString.append(", ");
					wordsString.append(word.getText());
				}
				addNLPNote(projectOrDomain, assistantUser, thingBeingAnalyzed, createMessage(
						PROP_SENSE_USE_MSG, PROP_SENSE_USE_MSG_DEFAULT, wordsString, sense
								.toString(), sense.getSynset().getDefinition()));
			} else {
				addNLPNote(projectOrDomain, assistantUser, thingBeingAnalyzed, createMessage(
						PROP_MULTI_SENSE_USE_MSG, PROP_MULTI_SENSE_USE_MSG_DEFAULT, words.get(0)
								.getText(), sense.toString(), sense.getSynset().getDefinition()));
			}
		}

		Set<NLPText> potentialTerms = new HashSet<NLPText>(nlpText.getLeaves().size());
		for (NLPText nounPhrase : getNLPProcessorFactory().getNounPhraseFinder().process(nlpText)) {
			// TODO: the "rules" for what can be a glossary term
			// should be externalized to make it easy to customize

			// TODO: use named entity information
			// TODO: use word sense information
			if (nounPhrase.getLeaves().size() == 1) {
				NLPText singleWord = nounPhrase.getLeaves().iterator().next();
				if (singleWord.is(ParseTag.NNP) || singleWord.is(ParseTag.NNPS)) {
					// add an issue for single proper noun phrases if the phrase
					// isn't a sub phrase of a larger phrase in the same text

					// TODO: use word sense category?
					potentialTerms.add(singleWord);
				}
			} else {
				// walk the parse tree and ignore phrases that contain full
				// clauses or other clause types.
				boolean addAsPotentialTerm = true;
				List<NLPText> todo = new ArrayList<NLPText>();
				todo.add(nounPhrase);
				while (!todo.isEmpty()) {
					NLPText current = todo.remove(0);
					if (current.is(GrammaticalStructureLevel.CLAUSE)
							|| current.in(ParseTag.PP, ParseTag.VP, ParseTag.INTJ, ParseTag.LST,
									ParseTag.UCP, ParseTag.WHADJP, ParseTag.WHAVP, ParseTag.WHNP,
									ParseTag.WHPP)
							|| current.in(PartOfSpeech.NUMBER, PartOfSpeech.PUNCTUATION,
									PartOfSpeech.SYMBOL)) {
						addAsPotentialTerm = false;
						break;
					} else {
						todo.addAll(current.getChildren());
						// filter out things that are statements:
						// "his shoes"
						// 
						for (NLPText word : current.getLeaves()) {
							if (word.is(ParseTag.PRP$)) {
								addAsPotentialTerm = false;
							}
						}
					}
				}
				if (addAsPotentialTerm) {
					potentialTerms.add(nounPhrase);
				}
			}
		}
		// TODO: should this take into account the tags or part of speech of
		// each word?
		// fiter out phrases that are sub-phrases of a longer phrase
		Set<NLPText> filteredTerms = new HashSet<NLPText>(potentialTerms);
		for (NLPText outerPhrase : potentialTerms) {
			for (NLPText innerPhrase : potentialTerms) {
				if (!outerPhrase.equals(innerPhrase)) {
					if (innerPhrase.getText().toLowerCase().contains(
							outerPhrase.getText().toLowerCase())) {
						filteredTerms.remove(outerPhrase);
						break;
					}
					if (outerPhrase.getText().toLowerCase().contains(
							innerPhrase.getText().toLowerCase())) {
						filteredTerms.remove(innerPhrase);
						break;
					}
				}
			}
		}
		if (!filteredTerms.isEmpty()) {
			for (NLPText term : filteredTerms) {
				try {
					// if a term exists then add a reference from the thing
					// being analyzed.
					GlossaryTerm glossaryTerm = getProjectRepository()
							.findGlossaryTermForProjectOrDomain(projectOrDomain, term.getText());
					addProjectOrDomainEntityAsRefererToGlossaryTerm(assistantUser, glossaryTerm,
							thingBeingAnalyzed);
				} catch (NoSuchGlossaryTermException e) {
					// if the phrase matches an actor name. TODO: ignore or add
					// an issue to actor containers to add a referece to the
					// actor?
					boolean addIssue = true;
					Set<String> namesToMatch = new HashSet<String>();
					namesToMatch.add(term.getText());
					if (!term.isLeaf() && term.getLeaves().get(0).in(PartOfSpeech.DETERMINER)) {
						namesToMatch.add(term.getTextRange(1));
					}
					for (String str : namesToMatch) {
						try {
							getProjectRepository().findActorByProjectOrDomainAndName(
									projectOrDomain, str);
							addIssue = false;
						} catch (NoSuchActorException e2) {
						}
					}
					if (addIssue) {
						// add an issue to create a term
						addGlossaryIssue(projectOrDomain, assistantUser, thingBeingAnalyzed, term);
					}
				}
			}
		}
	}

	/**
	 * Check the spelling of the supplied text adding LexicalIssues to the
	 * "thingBeingAnalyzed" for words not found in the dictionary.
	 * 
	 * @param groupingObject -
	 *            the object that acts as the owner of the created annotations.
	 * @param assistantUser -
	 *            the user to assign as the creator/editor of any issues and
	 *            possitions created.
	 * @param projectOrDomain
	 * @param thingBeingAnalyzed
	 * @param annotatableEntityPropertyName
	 * @param nlpText -
	 *            the text to be analyzed in an NLP Text format, already
	 *            processed.
	 * @throws Exception
	 */
	public void checkSpelling(User assistantUser, ProjectOrDomain projectOrDomain,
			ProjectOrDomainEntity thingBeingAnalyzed, String annotatableEntityPropertyName,
			NLPText nlpText) throws Exception {
		NLPProcessor<Boolean> spellChecker = getNLPProcessorFactory().getSpellingChecker();
		NLPProcessor<Collection<NLPText>> similarWordFinder = getNLPProcessorFactory()
				.getSimilarWordFinder();

		for (NLPText word : nlpText.getLeaves()) {
			if (!word.in(PartOfSpeech.PUNCTUATION, PartOfSpeech.NUMBER, PartOfSpeech.SYMBOL)
					&& !word.in(ParseTag.POS, ParseTag.CD)) {
				log.debug("analyzing word : '" + word + "' pos: " + word.getPartOfSpeech());
				if (!spellChecker.process(word)) {
					addSpellingIssue(similarWordFinder, projectOrDomain, assistantUser,
							thingBeingAnalyzed, word, annotatableEntityPropertyName);
				}
			}
		}
	}

	/**
	 * Check the senses of the words in the nlpText for vague words (low
	 * information content) and suggest some more specific words to use instead.
	 * 
	 * @param assistantUser
	 * @param projectOrDomain
	 * @param thingBeingAnalyzed
	 * @param annotatableEntityPropertyName
	 * @param nlpText
	 * @throws Exception
	 */
	public void checkVagueWordUse(User assistantUser, ProjectOrDomain projectOrDomain,
			ProjectOrDomainEntity thingBeingAnalyzed, String annotatableEntityPropertyName,
			NLPText nlpText) throws Exception {
		NLPProcessor<Collection<NLPText>> moreSpecificWordSuggester = getNLPProcessorFactory()
				.getMoreSpecificWordSuggester();
		double infoContentThreshold = getResourceBundleHelper().getDouble(
				PROP_INFO_CONTENT_THRESHOLD, PROP_INFO_CONTENT_THRESHOLD_DEFAULT);
		Linkdef linkType = getDictionaryRepository().findLinkDef(1L);

		// Proper nouns may have entity#n#1 assigned as the sense, if that's the
		// case don't mark it as vague.
		Sense rootNounSense = getDictionaryRepository().findSense("entity", PartOfSpeech.NOUN, 1);

		for (NLPText word : nlpText.getLeaves()) {
			if ((!word.isNamedEntity())
					&& (word.getDictionaryWordSense() != null)
					&& (word.getDictionaryWordSense().getSynset() != null)
					&& !(word.getDictionaryWordSense().equals(rootNounSense) && word.in(
							ParseTag.NNP, ParseTag.NNPS))) {
				double infoContent = getDictionaryRepository().infoContent(
						word.getDictionaryWordSense().getSynset(), linkType);
				if (infoContent < infoContentThreshold) {
					addVagueWordUseIssue(moreSpecificWordSuggester, projectOrDomain, assistantUser,
							thingBeingAnalyzed, word, annotatableEntityPropertyName);
				}
			}
		}
	}

	/**
	 * Analyze updated text of an entity to see if a user has changed the
	 * supplied property fixing lexical issues manually and remove annotations
	 * that are no longer relevant.
	 * 
	 * @param assistantUser
	 * @param projectOrDomain
	 * @param thingBeingAnalyzed
	 * @param annotatableEntityPropertyName
	 * @param nlpText
	 * @throws Exception
	 */
	public void removeUnneedLexicalIssues(User assistantUser, ProjectOrDomain projectOrDomain,
			ProjectOrDomainEntity thingBeingAnalyzed, String annotatableEntityPropertyName,
			NLPText nlpText) throws Exception {
		RemoveUnneedLexicalIssuesCommand command = getProjectCommandFactory()
				.newRemoveUnneedLexicalIssuesCommand();
		command.setAnnotatableEntityPropertyName(annotatableEntityPropertyName);
		command.setNlpText(nlpText);
		command.setThingBeingAnalyzed(thingBeingAnalyzed);
		command.setProjectOrDomain(projectOrDomain);
		command.setEditedBy(assistantUser);
		command = getCommandHandler().execute(command);
	}

	protected void addGlossaryIssue(ProjectOrDomain projectOrDomain, User assistantUser,
			ProjectOrDomainEntity thingBeingAnalyzed, NLPText term) throws Exception {
		EditLexicalIssueCommand editIssueCommand = getAnnotationCommandFactory()
				.newEditLexicalIssueCommand();
		// TODO: add issues/positions for partial matches by removing
		// determiners from the text.
		try {
			editIssueCommand.setIssue(getAnnotationRepository().findLexicalIssue(projectOrDomain,
					thingBeingAnalyzed, term.getText()));
			editIssueCommand.setAnnotatable(thingBeingAnalyzed);
			editIssueCommand.setEditedBy(assistantUser);
			editIssueCommand = getCommandHandler().execute(editIssueCommand);
		} catch (NoSuchAnnotationException e) {
			editIssueCommand.setGroupingObject(projectOrDomain);
			editIssueCommand.setWord(term.getText());
			editIssueCommand.setText(createMessage(PROP_TERM_ACTOR_DOMAIN_MSG,
					PROP_TERM_ACTOR_DOMAIN_MSG_DEFAULT, term.getText()));
			editIssueCommand.setMustBeResolved(true);
			editIssueCommand.setAnnotatable(thingBeingAnalyzed);
			editIssueCommand.setEditedBy(assistantUser);
			editIssueCommand = getCommandHandler().execute(editIssueCommand);
			LexicalIssue issue = (LexicalIssue) editIssueCommand.getIssue();
			addSimplePositionToIssue(projectOrDomain, assistantUser, issue, createMessage(
					PROP_IGNORE_PHRASE_MSG, PROP_IGNORE_PHRASE_MSG_DEFAULT));
			addAddWordToGlossaryPositionToIssue(assistantUser, projectOrDomain, issue);
			addAddActorToProjectPositionToIssue(assistantUser, projectOrDomain, issue);
		}
	}

	protected void addSpellingIssue(NLPProcessor<Collection<NLPText>> similarWordFinder,
			ProjectOrDomain projectOrDomain, User assistantUser,
			ProjectOrDomainEntity thingBeingAnalyzed, NLPText word,
			String annotatableEntityPropertyName) throws Exception {
		try {
			getAnnotationRepository().findLexicalIssue(projectOrDomain, thingBeingAnalyzed,
					word.getText(), annotatableEntityPropertyName);
		} catch (NoSuchAnnotationException e) {
			EditLexicalIssueCommand editIssueCommand = getAnnotationCommandFactory()
					.newEditLexicalIssueCommand();
			editIssueCommand.setGroupingObject(projectOrDomain);
			editIssueCommand.setWord(word.getText());
			editIssueCommand.setAnnotatableEntityPropertyName(annotatableEntityPropertyName);
			editIssueCommand.setText(createMessage(PROP_UKNOWN_WORD_MSG,
					PROP_UKNOWN_WORD_MSG_DEFAULT, word, annotatableEntityPropertyName));
			editIssueCommand.setMustBeResolved(true);
			editIssueCommand.setAnnotatable(thingBeingAnalyzed);
			editIssueCommand.setEditedBy(assistantUser);
			editIssueCommand = getCommandHandler().execute(editIssueCommand);
			LexicalIssue issue = (LexicalIssue) editIssueCommand.getIssue();
			addAddWordToDictionaryPositionToIssue(assistantUser, issue);
			addSimplePositionToIssue(projectOrDomain, assistantUser, issue, createMessage(
					PROP_IGNORE_WORD_MSG, PROP_IGNORE_WORD_MSG_DEFAULT));
			addAddWordToGlossaryPositionToIssue(assistantUser, projectOrDomain, issue);
			addAddActorToProjectPositionToIssue(assistantUser, projectOrDomain, issue);
			for (NLPText similarWord : similarWordFinder.process(word)) {
				addChangeSpellingPositionToIssue(assistantUser, issue, createMessage(
						PROP_SUGGESTED_SPELLING_MSG, PROP_SUGGESTED_SPELLING_MSG_DEFAULT, word,
						similarWord.getText()), similarWord.getText());
			}

		}
	}

	protected void addComplexityIssue(ProjectOrDomain projectOrDomain, User assistantUser,
			ProjectOrDomainEntity thingBeingAnalyzed, NLPText text,
			String annotatableEntityPropertyName) throws Exception {
		try {
			getAnnotationRepository()
					.findIssue(projectOrDomain, thingBeingAnalyzed, text.getText());
		} catch (NoSuchAnnotationException e) {
			// TODO: this uses a lexical issue, but isn't word oriented,
			// although it is property related so an issue with
			// annotatableEntityPropertyName is needed. Should add an in between
			// issue type PropertyRelatedIssue
			EditLexicalIssueCommand editIssueCommand = getAnnotationCommandFactory()
					.newEditLexicalIssueCommand();
			editIssueCommand.setGroupingObject(projectOrDomain);
			editIssueCommand.setAnnotatableEntityPropertyName(annotatableEntityPropertyName);
			editIssueCommand.setText(createMessage(PROP_COMPLEX_TEXT_MSG,
					PROP_COMPLEX_TEXT_MSG_DEFAULT, text, annotatableEntityPropertyName));
			editIssueCommand.setMustBeResolved(true);
			editIssueCommand.setAnnotatable(thingBeingAnalyzed);
			editIssueCommand.setEditedBy(assistantUser);
			editIssueCommand = getCommandHandler().execute(editIssueCommand);
			LexicalIssue issue = (LexicalIssue) editIssueCommand.getIssue();
			addAddWordToDictionaryPositionToIssue(assistantUser, issue);
			addSimplePositionToIssue(projectOrDomain, assistantUser, issue, createMessage(
					PROP_IGNORE_WORD_MSG, PROP_IGNORE_WORD_MSG_DEFAULT));
		}
	}

	protected void addVagueWordUseIssue(NLPProcessor<Collection<NLPText>> moreSpecificWordFinder,
			ProjectOrDomain projectOrDomain, User assistantUser,
			ProjectOrDomainEntity thingBeingAnalyzed, NLPText word,
			String annotatableEntityPropertyName) throws Exception {
		try {
			getAnnotationRepository().findLexicalIssue(projectOrDomain, thingBeingAnalyzed,
					word.getText(), annotatableEntityPropertyName);
		} catch (NoSuchAnnotationException e) {
			EditLexicalIssueCommand editIssueCommand = getAnnotationCommandFactory()
					.newEditLexicalIssueCommand();
			editIssueCommand.setGroupingObject(projectOrDomain);
			editIssueCommand.setWord(word.getText());
			editIssueCommand.setAnnotatableEntityPropertyName(annotatableEntityPropertyName);
			editIssueCommand.setText(createMessage(PROP_VAGUE_WORD_MSG,
					PROP_VAGUE_WORD_MSG_DEFAULT, word, annotatableEntityPropertyName));
			editIssueCommand.setMustBeResolved(true);
			editIssueCommand.setAnnotatable(thingBeingAnalyzed);
			editIssueCommand.setEditedBy(assistantUser);
			editIssueCommand = getCommandHandler().execute(editIssueCommand);
			LexicalIssue issue = (LexicalIssue) editIssueCommand.getIssue();
			addSimplePositionToIssue(projectOrDomain, assistantUser, issue, createMessage(
					PROP_IGNORE_WORD_MSG, PROP_IGNORE_WORD_MSG_DEFAULT));
			for (NLPText moreSpecificWord : moreSpecificWordFinder.process(word)) {
				addChangeSpellingPositionToIssue(assistantUser, issue, createMessage(
						PROP_SUGGESTED_MORE_SPECIFIC_WORD_MSG,
						PROP_SUGGESTED_MORE_SPECIFIC_WORD_MSG_DEFAULT, word, moreSpecificWord
								.getText()), moreSpecificWord.getText());
			}
		}
	}

	// TODO: create a new annotation type for nlp meta data
	protected Note addNLPNote(Object groupingObject, User assistantUser,
			ProjectOrDomainEntity thingBeingAnalyzed, String noteText) throws Exception {
		return super.addNote(groupingObject, assistantUser, thingBeingAnalyzed, noteText);
	}

	protected Position addChangeSpellingPositionToIssue(User assistantUser, Issue issue,
			String positionText, String proposedWord) throws Exception {
		EditChangeSpellingPositionCommand editPositionCommand = getAnnotationCommandFactory()
				.newEditChangeSpellingPositionCommand();
		editPositionCommand.setIssue(issue);
		editPositionCommand.setText(positionText);
		editPositionCommand.setEditedBy(assistantUser);
		editPositionCommand.setProposedWord(proposedWord);
		editPositionCommand = getCommandHandler().execute(editPositionCommand);
		return editPositionCommand.getPosition();
	}

	protected Position addAddWordToDictionaryPositionToIssue(User assistantUser, LexicalIssue issue)
			throws Exception {
		EditAddWordToDictionaryPositionCommand editPositionCommand = getAnnotationCommandFactory()
				.newEditAddWordToDictionaryPositionCommand();
		editPositionCommand.setIssue(issue);
		editPositionCommand.setText(createMessage(PROP_ADD_TO_DICTIONARY_MSG,
				PROP_ADD_TO_DICTIONARY_MSG_DEFAULT, issue.getWord()));
		editPositionCommand.setEditedBy(assistantUser);
		editPositionCommand = getCommandHandler().execute(editPositionCommand);
		return editPositionCommand.getPosition();
	}

	protected Position addAddWordToGlossaryPositionToIssue(User assistantUser,
			ProjectOrDomain projectOrDomain, LexicalIssue issue) throws Exception {
		EditAddWordToGlossaryPositionCommand editPositionCommand = getProjectCommandFactory()
				.newEditAddWordToGlossaryPositionCommand();
		editPositionCommand.setEditedBy(assistantUser);
		editPositionCommand.setIssue(issue);
		editPositionCommand.setProjectOrDomain(projectOrDomain);
		editPositionCommand.setText(createMessage(PROP_ADD_TO_GLOSSARY_MSG,
				PROP_ADD_TO_GLOSSARY_MSG_DEFAULT, issue.getWord()));
		editPositionCommand = getCommandHandler().execute(editPositionCommand);
		return editPositionCommand.getPosition();
	}

	protected Position addAddActorToProjectPositionToIssue(User assistantUser,
			ProjectOrDomain projectOrDomain, LexicalIssue issue) throws Exception {
		EditAddActorToProjectPositionCommand editPositionCommand = getProjectCommandFactory()
				.newEditAddActorToProjectPositionCommand();
		editPositionCommand.setEditedBy(assistantUser);
		editPositionCommand.setIssue(issue);
		editPositionCommand.setProjectOrDomain(projectOrDomain);
		editPositionCommand.setText(createMessage(PROP_ADD_AS_ACTOR_MSG,
				PROP_ADD_AS_ACTOR_MSG_DEFAULT, issue.getWord()));
		editPositionCommand = getCommandHandler().execute(editPositionCommand);
		return editPositionCommand.getPosition();
	}

	protected void addProjectOrDomainEntityAsRefererToGlossaryTerm(User assistantUser,
			GlossaryTerm glossaryTerm, ProjectOrDomainEntity thingBeingAnalyzed) throws Exception {
		EditGlossaryTermCommand command = getProjectCommandFactory().newEditGlossaryTermCommand();
		Set<ProjectOrDomainEntity> entities = new HashSet<ProjectOrDomainEntity>(1);
		entities.add(thingBeingAnalyzed);
		command.setAddReferers(entities);
		command.setEditedBy(assistantUser);
		command.setGlossaryTerm(glossaryTerm);
		command = getCommandHandler().execute(command);
	}
}
