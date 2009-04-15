/*
 * $Id: EditSynsetCommandImpl.java,v 1.1 2008/12/13 00:39:59 rregan Exp $
 * Copyright (c) 2008 Ron Regan Jr. All Rights Reserved.
 */

package edu.harvard.fas.rregan.nlp.dictionary.impl.command;

import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Controller;

import edu.harvard.fas.rregan.nlp.dictionary.DictionaryRepository;
import edu.harvard.fas.rregan.nlp.dictionary.Linkdef;
import edu.harvard.fas.rregan.nlp.dictionary.Synset;
import edu.harvard.fas.rregan.nlp.dictionary.command.EditSynsetCommand;

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
	 * @see edu.harvard.fas.rregan.requel.dictionary.command.EditSynsetCommand#setSubsumerCounts(Set<Integer>)
	 */
	@Override
	public void setSubsumerCounts(Map<Long, Integer> subsumerCounts) {
		this.subsumerCounts = subsumerCounts;
	}

	/**
	 * @see edu.harvard.fas.rregan.nlp.dictionary.command.EditSynsetCommand#setSynset(edu.harvard.fas.rregan.nlp.dictionary.Synset)
	 */
	@Override
	public void setSynset(Synset synset) {
		this.synset = synset;
	}

	protected Integer getSubsumerCounts(Integer linkType) {
		if (subsumerCounts != null && subsumerCounts.containsKey(linkType)) {
			return subsumerCounts.get(linkType);
		}
		return 0;
	}

	protected Synset getSynset() {
		return synset;
	}

	/**
	 * @see edu.harvard.fas.rregan.command.Command#execute()
	 */
	@Override
	public void execute() throws Exception {
		Synset synset = getDictionaryRepository().get(getSynset());
		if (synset != null && subsumerCounts != null) {
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
