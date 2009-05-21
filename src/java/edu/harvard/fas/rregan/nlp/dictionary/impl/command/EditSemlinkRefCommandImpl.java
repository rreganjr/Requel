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
package edu.harvard.fas.rregan.nlp.dictionary.impl.command;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Controller;

import edu.harvard.fas.rregan.nlp.dictionary.DictionaryRepository;
import edu.harvard.fas.rregan.nlp.dictionary.Semlinkref;
import edu.harvard.fas.rregan.nlp.dictionary.SemlinkrefId;
import edu.harvard.fas.rregan.nlp.dictionary.command.EditSemlinkRefCommand;
import edu.harvard.fas.rregan.requel.NoSuchEntityException;

/**
 * @author ron
 */
@Controller("editSemlinkRefCommand")
@Scope("prototype")
public class EditSemlinkRefCommandImpl extends AbstractDictionaryCommand implements
		EditSemlinkRefCommand {

	private Semlinkref semlinkref;
	private Long fromSynsetid;
	private Long toSynsetid;
	private Long linkdefid;
	private Integer distance;

	/**
	 * @param dictionaryRepository
	 */
	@Autowired
	public EditSemlinkRefCommandImpl(DictionaryRepository dictionaryRepository) {
		super(dictionaryRepository);
	}

	/**
	 * @see edu.harvard.fas.rregan.nlp.dictionary.command.EditSemlinkRefCommand#setSemlinkRef(edu.harvard.fas.rregan.nlp.dictionary.Semlinkref)
	 */
	@Override
	public void setSemlinkRef(Semlinkref semlinkref) {
		this.semlinkref = semlinkref;
	}

	/**
	 * @see edu.harvard.fas.rregan.nlp.dictionary.command.EditSemlinkRefCommand#getSemlinkref()
	 */
	@Override
	public Semlinkref getSemlinkref() {
		return semlinkref;
	}

	/**
	 * @see edu.harvard.fas.rregan.nlp.dictionary.command.EditSemlinkRefCommand#setFromSynset(edu.harvard.fas.rregan.requel.dictionary.Synset)
	 */
	@Override
	public void setFromSynset(Long synsetid) {
		this.fromSynsetid = synsetid;
	}

	/**
	 * @see edu.harvard.fas.rregan.nlp.dictionary.command.EditSemlinkRefCommand#setToSynset(edu.harvard.fas.rregan.requel.dictionary.Synset)
	 */
	@Override
	public void setToSynset(Long synsetid) {
		this.toSynsetid = synsetid;
	}

	/**
	 * @see edu.harvard.fas.rregan.nlp.dictionary.command.EditSemlinkRefCommand#setLinkDef(edu.harvard.fas.rregan.requel.dictionary.Linkdef)
	 */
	@Override
	public void setLinkDef(Long linkdefid) {
		this.linkdefid = linkdefid;
	}

	/**
	 * @see edu.harvard.fas.rregan.nlp.dictionary.command.EditSemlinkRefCommand#setDistance(java.lang.Integer)
	 */
	@Override
	public void setDistance(Integer distance) {
		this.distance = distance;
	}

	/**
	 * @see edu.harvard.fas.rregan.command.Command#execute()
	 */
	@Override
	public void execute() throws Exception {
		Semlinkref semlinkref = getDictionaryRepository().get(getSemlinkref());
		if (semlinkref == null) {
			SemlinkrefId semlinkrefId = new SemlinkrefId(fromSynsetid, toSynsetid, linkdefid,
					distance);
			try {
				semlinkref = getDictionaryRepository().findSemlinkref(semlinkrefId);
			} catch (NoSuchEntityException e) {
				semlinkref = getDictionaryRepository().persist(new Semlinkref(semlinkrefId));
			}
		} else {
			// TODO: allow editing existing or thrown an exception?
		}

	}

}
