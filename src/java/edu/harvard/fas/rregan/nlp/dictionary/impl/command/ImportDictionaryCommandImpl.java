/*
 * $Id: ImportDictionaryCommandImpl.java,v 1.1 2008/12/13 00:39:58 rregan Exp $
 * Copyright (c) 2008 Ron Regan Jr. All Rights Reserved.
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
