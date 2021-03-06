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
package com.rreganjr.nlp.dictionary.impl.command;

import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Controller;

import com.rreganjr.nlp.dictionary.DictionaryRepository;
import com.rreganjr.nlp.dictionary.Linkdef;
import com.rreganjr.nlp.dictionary.Synset;
import com.rreganjr.nlp.dictionary.command.EditSynsetCommand;

/**
 * @author ron
 */
@Controller("editSynsetCommand")
@Scope("prototype")
public class EditSynsetCommandImpl extends AbstractDictionaryCommand implements EditSynsetCommand {

	private Synset synset;
	private Map<Long, Integer> subsumerCounts;

	/**
	 * @param dictionaryRepository
	 */
	@Autowired
	public EditSynsetCommandImpl(DictionaryRepository dictionaryRepository) {
		super(dictionaryRepository);
	}

	/**
	 * @see com.rreganjr.requel.dictionary.command.EditSynsetCommand#setSubsumerCounts(Set<Integer>)
	 */
	@Override
	public void setSubsumerCounts(Map<Long, Integer> subsumerCounts) {
		this.subsumerCounts = subsumerCounts;
	}

	/**
	 * @see com.rreganjr.nlp.dictionary.command.EditSynsetCommand#setSynset(com.rreganjr.nlp.dictionary.Synset)
	 */
	@Override
	public void setSynset(Synset synset) {
		this.synset = synset;
	}

	protected Integer getSubsumerCounts(Integer linkType) {
		if ((subsumerCounts != null) && subsumerCounts.containsKey(linkType)) {
			return subsumerCounts.get(linkType);
		}
		return 0;
	}

	protected Synset getSynset() {
		return synset;
	}

	/**
	 * @see com.rreganjr.command.Command#execute()
	 */
	@Override
	public void execute() throws Exception {
		Synset synset = getDictionaryRepository().get(getSynset());
		if ((synset != null) && (subsumerCounts != null)) {
			for (Long linkTypeId : subsumerCounts.keySet()) {
				Linkdef linkType = getDictionaryRepository().findLinkDef(linkTypeId);
				if (subsumerCounts.containsKey(linkTypeId)) {
					synset.setSubsumerCount(linkType, subsumerCounts.get(linkTypeId));
				} else {
					synset.setSubsumerCount(linkType, 0);
				}
			}
		}
	}
}
