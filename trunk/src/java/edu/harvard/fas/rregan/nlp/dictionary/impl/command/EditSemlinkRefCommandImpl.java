/*
 * $Id: EditSemlinkRefCommandImpl.java,v 1.1 2008/12/13 00:39:58 rregan Exp $
 * Copyright (c) 2008 Ron Regan Jr. All Rights Reserved.
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
