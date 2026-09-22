/*
 * This file is part of Requel - the Collaborative Requirements
 * Elicitation System.
 *
 * Copyright 2008, 2009, 2025 Ron Regan Jr. All Rights Reserved.
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
package com.rreganjr.nlp.dictionary.impl.command;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Controller;

import com.rreganjr.nlp.dictionary.DictionaryRepository;
import com.rreganjr.nlp.dictionary.command.EditDictionaryWordCommand;

/**
 * @author ron
 */
@Controller("editDictionaryWordCommand")
@Scope("prototype")
public class EditDictionaryWordCommandImpl extends AbstractDictionaryCommand implements
		EditDictionaryWordCommand {

	private String lemma;

	private Long projectId;

	/**
	 * @param dictionaryRepository
	 */
	@Autowired
	public EditDictionaryWordCommandImpl(DictionaryRepository dictionaryRepository) {
		super(dictionaryRepository);
	}

	protected String getLemma() {
		return lemma;
	}

	public void setLemma(String lemma) {
		this.lemma = lemma;
	}

	protected Long getProjectId() {
		return projectId;
	}

	@Override
	public void setProjectId(Long projectId) {
		this.projectId = projectId;
	}

	/**
	 * @see com.rreganjr.command.Command#execute()
	 */
	/**
	 * Issue #313 changed where the word goes — the project's own dictionary rather than the
	 * installation-wide WordNet table — and nothing else about this method.
	 * <p>
	 * The swallowed exception is deliberately left as it is. It is a real defect: a failed
	 * dictionary write is reported as a successful resolve. It belongs to #312 along with the
	 * missing authorization on this command, and #312 was blocked on this ticket precisely because
	 * the gate could not be scoped while the write was installation-wide. Fixing it here would put
	 * half of #312 in an unrelated diff.
	 */
	@Override
	public void execute() {
		try {
			getDictionaryRepository().addToDictionary(getProjectId(), getLemma());
		} catch (Exception e) {
			log.error(e, e);
		}
	}
}
