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

import java.io.InputStream;

import javax.persistence.NoResultException;
import javax.xml.bind.JAXBContext;
import javax.xml.bind.Unmarshaller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Controller;

import edu.harvard.fas.rregan.nlp.dictionary.Dictionary;
import edu.harvard.fas.rregan.nlp.dictionary.DictionaryRepository;
import edu.harvard.fas.rregan.nlp.dictionary.UnmarshallerListener;
import edu.harvard.fas.rregan.nlp.dictionary.Word;
import edu.harvard.fas.rregan.nlp.dictionary.command.ImportDictionaryCommand;

/**
 * @author ron
 */
@Controller("importDictionaryCommand")
@Scope("prototype")
public class ImportDictionaryCommandImpl extends AbstractDictionaryCommand implements
		ImportDictionaryCommand {

	private InputStream inputStream;

	/**
	 * @param dictionaryRepository
	 */
	@Autowired
	public ImportDictionaryCommandImpl(DictionaryRepository dictionaryRepository) {
		super(dictionaryRepository);
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see edu.harvard.fas.rregan.requel.dictionary.ImportDictionaryCommand#setInputStream(java.io.InputStream)
	 */
	public void setInputStream(InputStream inputStream) {
		this.inputStream = inputStream;
	}

	protected InputStream getInputStream() {
		return inputStream;
	}

	/**
	 * @see edu.harvard.fas.rregan.command.Command#execute()
	 */
	@Override
	public void execute() {
		try {
			// NOTE: the annotation classes need to be explicitly supplied to
			// the newInstance or an IllegalAnnotationExceptions will occur for
			// AbstractProjectOrDomainEntity.getAnnotations()
			JAXBContext context = JAXBContext
					.newInstance(ExportDictionaryCommandImpl.CLASSES_FOR_JAXB);
			Unmarshaller unmarshaller = context.createUnmarshaller();
			unmarshaller.setListener(new UnmarshallerListener(getDictionaryRepository()));
			Dictionary dictionary = (Dictionary) unmarshaller.unmarshal(getInputStream());
			for (Word word : dictionary.getWords()) {
				try {
					getDictionaryRepository().persist(word);
					// Word existingWord =
					// wordNetRepository.getWord(word.getLemma());
				} catch (NoResultException e) {
					getDictionaryRepository().persist(word);
				} catch (Exception e) {
					log.error("could not save word '" + word.getLemma() + "': " + e, e);
				}
			}
		} catch (Exception e) {
			log.error(e, e);
		}
	}
}
