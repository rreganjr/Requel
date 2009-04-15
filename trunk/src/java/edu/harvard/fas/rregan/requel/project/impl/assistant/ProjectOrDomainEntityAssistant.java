/*
 * $Id: ProjectOrDomainEntityAssistant.java,v 1.11 2009/03/27 07:16:07 rregan Exp $
 * Copyright (c) 2008 Ron Regan Jr. All Rights Reserved.
 */
package edu.harvard.fas.rregan.requel.project.impl.assistant;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

import org.apache.log4j.Logger;

import edu.harvard.fas.rregan.nlp.NLPText;
import edu.harvard.fas.rregan.requel.project.ProjectOrDomain;
import edu.harvard.fas.rregan.requel.project.ProjectOrDomainEntity;
import edu.harvard.fas.rregan.requel.user.User;

/**
 * Assistant for analyzing entities based on the ProjectOrDomainEntity
 * interface, such as goals and stories. The assistant applies the lexical
 * assistant to the name and text properties of each entity.
 * 
 * @author ron
 */
public class ProjectOrDomainEntityAssistant extends AbstractAssistant {
	private static final Logger log = Logger.getLogger(ProjectOrDomainEntityAssistant.class);

	public static final String PROP_NAME = "Name";
	public static final String PROP_TEXT = "Text";
	public static final String PROP_COMBINED = "Combined";

	private final User assistantUser;
	private final LexicalAssistant lexicalAssistant;

	// the entity under analysis
	private ProjectOrDomainEntity entity;
	private final Map<String, NLPText> propertyNameToNLPText = new HashMap<String, NLPText>();

	/**
	 * @param resourceBundleName -
	 *            the full class name to use for the resource bundle.
	 * @param lexicalAssistant -
	 *            assistant for analyzing text for spelling, terms and other
	 *            word oriented analysis.
	 * @param assistantUser -
	 *            the user to use as the creator of the annotation entities.
	 */
	public ProjectOrDomainEntityAssistant(String resourceBundleName,
			LexicalAssistant lexicalAssistant, User assistantUser) {
		super(resourceBundleName, lexicalAssistant.getCommandHandler(), lexicalAssistant
				.getAnnotationCommandFactory(), lexicalAssistant.getAnnotationRepository());
		this.lexicalAssistant = lexicalAssistant;
		this.assistantUser = assistantUser;
	}

	/**
	 * @return The entity being analyzed.
	 */
	public ProjectOrDomainEntity getEntity() {
		return entity;
	}

	/**
	 * Set the entity to analyze. Must be called before analyze.
	 * 
	 * @param entity -
	 *            the entity to analyze.
	 * @throws IllegalArgumentException -
	 *             if the supplied entity is null or the type is not supported
	 *             by the assistant.
	 */
	public void setEntity(ProjectOrDomainEntity entity) throws IllegalArgumentException {
		propertyNameToNLPText.clear();
		this.entity = entity;
		setPropertyText(PROP_NAME, entity.getName());
	}

	/**
	 * Analyze the text in the name property of the entity and notify the
	 * UpdatedEntityNotifier that the entity has been updated.<br>
	 * NOTE: sub classes should call super.analyze() and not analyzeName()
	 * directly so that the UpdatedEntityNotifier is called.
	 * 
	 * @param entity
	 */
	public void analyze() {
		// remove any lexical annotations that apply to all the properties
		// collectively (for example glossary term issues) that are no longer
		// relevant.
		try {
			NLPText nlpText = getLexicalAssistant().getNLPProcessorFactory().appendText(
					new ArrayList<NLPText>(propertyNameToNLPText.values()));
			getLexicalAssistant().removeUnneedLexicalIssues(getAssistantUser(),
					entity.getProjectOrDomain(), entity, null, nlpText);
		} catch (Exception e) {
			log.error("failed to remove irrelevant annotations for " + entity, e);
			try {
				// TODO: adding a note with the exception is probably not
				// helpful to a user.
				addNote(entity.getProjectOrDomain(), getAssistantUser(), entity,
						"removing irrelevant annotations failed and was not recoverable: " + e);
			} catch (Exception e2) {
				log.error("failed to add note indicating failure to "
						+ "remove irrelevant annotations.", e2);
			}
		}
		for (String propertyName : propertyNameToNLPText.keySet()) {
			analyzeProperty(propertyName);
		}
	}

	protected NLPText getPropertyNlpText(String propertyName) {
		return propertyNameToNLPText.get(propertyName);
	}

	protected void setPropertyText(String propertyName, String text) {
		NLPText nlpText = getLexicalAssistant().getNLPProcessorFactory().processText(text);
		propertyNameToNLPText.put(propertyName, nlpText);
	}

	protected void analyzeProperty(String propertyName) {
		ProjectOrDomainEntity entity = getEntity();
		ProjectOrDomain projectOrDomain = entity.getProjectOrDomain();
		NLPText nlpText = getPropertyNlpText(propertyName);
		log.debug("lexically analyzing the " + propertyName + " of " + entity);

		// first remove any lexical issues related to the name that are no
		// longer relevant
		try {
			getLexicalAssistant().removeUnneedLexicalIssues(getAssistantUser(), projectOrDomain,
					entity, propertyName, nlpText);
		} catch (Exception e) {
			log.error("failed to remove irrelevant annotations for the " + propertyName + " of "
					+ entity, e);
			try {
				// TODO: adding a note with the exception is probably not
				// helpful to a user.
				addNote(projectOrDomain, getAssistantUser(), entity,
						"removing irrelevant annotations for the " + propertyName + " of " + entity
								+ " failed and was not recoverable: " + e);
			} catch (Exception e2) {
				log.error("failed to add note indicating failure to "
						+ " remove irrelevant annotations.", e2);
			}
		}

		try {
			getLexicalAssistant().checkSpelling(getAssistantUser(), projectOrDomain, entity,
					propertyName, nlpText);
		} catch (Exception e) {
			log.error("failed to spell check the " + propertyName + " of " + entity, e);
			try {
				// TODO: adding a note with the exception is probably not
				// helpful to a user.
				addNote(projectOrDomain, getAssistantUser(), entity, "spell checking of the "
						+ propertyName + " failed and was not recoverable: " + e);
			} catch (Exception e2) {
				log.error("failed to add note indicating failure of spell checking for the "
						+ propertyName + " of " + entity, e2);
			}
		}

		try {
			getLexicalAssistant().checkVagueWordUse(getAssistantUser(), projectOrDomain, entity,
					propertyName, nlpText);
		} catch (Exception e) {
			log.error("failed to check vague words in the " + propertyName + " of " + entity, e);
			try {
				// TODO: adding a note with the exception is probably not
				// helpful to a user.
				addNote(projectOrDomain, getAssistantUser(), entity, "vague words checking of the "
						+ propertyName + " failed and was not recoverable: " + e);
			} catch (Exception e2) {
				log.error("failed to add note indicating failure of vague words checking for the "
						+ propertyName + " of " + entity, e2);
			}
		}

		try {
			getLexicalAssistant().findPossibleGlossaryTerms(getAssistantUser(), projectOrDomain,
					entity, nlpText);
		} catch (Exception e) {
			log.error("failed to find glossary terms for the " + propertyName + " of " + entity, e);
			try {
				// TODO: adding a note with the exception is probably not
				// helpful to a user.
				addNote(projectOrDomain, getAssistantUser(), entity,
						"glossary term finding failed for the " + propertyName
								+ " and was not recoverable: " + e);
			} catch (Exception e2) {
				log.error("failed to add note indicating failure of glossary term"
						+ " identification for the " + propertyName + " of " + entity, e2);
			}
		}

		try {
			getLexicalAssistant().findPotentialComplexSentences(getAssistantUser(),
					projectOrDomain, entity, propertyName, nlpText);
		} catch (Exception e) {
			log.error("failed to check complexity of the " + propertyName + " of " + entity, e);
			try {
				// TODO: adding a note with the exception is probably not
				// helpful to a user.
				addNote(projectOrDomain, getAssistantUser(), entity, "complexity checking of the "
						+ propertyName + " failed and was not recoverable: " + e);
			} catch (Exception e2) {
				log.error("failed to add note indicating failure of complexity checking for the "
						+ propertyName + " of " + entity, e2);
			}
		}
	}

	public LexicalAssistant getLexicalAssistant() {
		return lexicalAssistant;
	}

	public User getAssistantUser() {
		return getLexicalAssistant().getAnnotationRepository().get(assistantUser);
	}
}