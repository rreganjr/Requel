/*
 * $Id: SemanticRoleLabelerException.java,v 1.5 2009/02/10 10:26:03 rregan Exp $
 * Copyright (c) 2009 Ron Regan Jr. All Rights Reserved.
 */

package edu.harvard.fas.rregan.nlp.impl.srl;

import java.util.List;

import edu.harvard.fas.rregan.nlp.NLPText;
import edu.harvard.fas.rregan.nlp.dictionary.VerbNetFrameRef;
import edu.harvard.fas.rregan.nlp.impl.NLPProcessorException;

/**
 * @author ron
 */
public class SemanticRoleLabelerException extends NLPProcessorException {
	static final long serialVersionUID = 0;

	protected static final String MSG_FORMAT_MATCHED_FAILED = "Frame match failed at rule '%s' with word '%s'";
	protected static final String MSG_FORMAT_MATCHED_FAILED_SELRES = "Frame match failed at rule '%s' with word '%s' by selectional restriction.";
	protected static final String MSG_FORMAT_UNMATCHED_SENTENCE_ELEMENTS = "Unmatched sentence elements '%s'";
	protected static final String MSG_FORMAT_UNMATCHED_RULES = "Unmatched rules '%s'";
	protected static final String MSG_FORMAT_UNKNOWN_SEMANTIC_ROLE = "The role '%s' in syntax '%s' for class '%s' is not known.";
	protected static final String MSG_FORMAT_SEMANTIC_ROLE_NOT_IN_CLASS = "The role '%s' was not expected in syntax '%s' for class '%s'";
	protected static final String MSG_FORMAT_VERB_NOT_FOUND = "Could not find primary verb in text '%s'";

	/**
	 * @param rule -
	 *            the rule that wasn't matched.
	 * @return a SemanticRoleLabelerException with a message that matching
	 *         failed on the supplied rule.
	 */
	protected static SemanticRoleLabelerException matchFailed(SyntaxMatchingRule rule, NLPText word) {
		return new SemanticRoleLabelerException(MSG_FORMAT_MATCHED_FAILED, rule, word);
	}

	protected static SemanticRoleLabelerException matchFailedBySelectionalRestriction(
			SyntaxMatchingRule rule, NLPText word) {
		return new SemanticRoleLabelerException(MSG_FORMAT_MATCHED_FAILED_SELRES, rule, word);
	}

	/**
	 * @return a SemanticRoleLabelerException with a message that all the rules
	 *         were matched and there were unmatched sentence elements
	 *         remaining.
	 */
	protected static SemanticRoleLabelerException unmatchedSentenceElementsRemaining(
			String remainSentenceText) {
		return new SemanticRoleLabelerException(MSG_FORMAT_UNMATCHED_SENTENCE_ELEMENTS,
				remainSentenceText);
	}

	/**
	 * @return a SemanticRoleLabelerException with a message that all the rules
	 *         were matched and there were unmatched sentence elements
	 *         remaining.
	 */
	protected static SemanticRoleLabelerException unmatchedRulesRemaining(
			List<SyntaxMatchingRule> remaingRules) {
		return new SemanticRoleLabelerException(MSG_FORMAT_UNMATCHED_RULES, remaingRules.toString());
	}

	/**
	 * @param semanticRole
	 * @param frameRef
	 * @return a SemanticRoleLabelerException with a message indicating an
	 *         unexpected semantic role was found.
	 */
	protected static SemanticRoleLabelerException unknownSemanticRole(String semanticRole,
			VerbNetFrameRef frameRef) {
		return new SemanticRoleLabelerException(MSG_FORMAT_UNKNOWN_SEMANTIC_ROLE, semanticRole,
				frameRef.getFrame().getSyntax(), frameRef.getClass());
	}

	/**
	 * @param semanticRole
	 * @param frameRef
	 * @return a SemanticRoleLabelerException with a message indicating a
	 *         semantic role was not expected in this class.
	 */
	protected static SemanticRoleLabelerException roleNotInClass(String semanticRole,
			VerbNetFrameRef frameRef) {
		return new SemanticRoleLabelerException(MSG_FORMAT_SEMANTIC_ROLE_NOT_IN_CLASS,
				semanticRole, frameRef.getFrame().getSyntax(), frameRef.getVnClass());
	}

	/**
	 * @param text
	 * @return
	 */
	protected static SemanticRoleLabelerException nominalSubjectNotFound(NLPText text) {
		return new SemanticRoleLabelerException(MSG_FORMAT_VERB_NOT_FOUND, text.getText());
	}

	/**
	 * @param format
	 * @param args
	 */
	protected SemanticRoleLabelerException(String format, Object... args) {
		super(format, args);
	}

	/**
	 * @param cause
	 * @param format
	 * @param args
	 */
	protected SemanticRoleLabelerException(Throwable cause, String format, Object... args) {
		super(cause, format, args);
	}

}