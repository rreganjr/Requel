/*
 * $Id: ExportDictionaryCommandImpl.java,v 1.1 2008/12/13 00:39:56 rregan Exp $
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

import java.io.OutputStream;
import java.util.ArrayList;
import java.util.List;

import javax.xml.bind.JAXBContext;
import javax.xml.bind.Marshaller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Controller;

import edu.harvard.fas.rregan.nlp.dictionary.Category;
import edu.harvard.fas.rregan.nlp.dictionary.Dictionary;
import edu.harvard.fas.rregan.nlp.dictionary.DictionaryRepository;
import edu.harvard.fas.rregan.nlp.dictionary.Sense;
import edu.harvard.fas.rregan.nlp.dictionary.Synset;
import edu.harvard.fas.rregan.nlp.dictionary.Word;
import edu.harvard.fas.rregan.nlp.dictionary.command.ExportDictionaryCommand;

/**
 * @author ron
 */
@Controller("exportDictionaryCommand")
@Scope("prototype")
public class ExportDictionaryCommandImpl extends AbstractDictionaryCommand implements
		ExportDictionaryCommand {

	/**
	 * An array of classes to pass to JAXBContext.newInstance() that includes
	 * all the classes that are used in XmlElementRef annotations
	 */
	public static final Class<?>[] CLASSES_FOR_JAXB;
	static {
		List<Class<?>> classes = new ArrayList<Class<?>>();
		classes.add(Dictionary.class);
		classes.add(Word.class);
		classes.add(Synset.class);
		classes.add(Category.class);
		classes.add(Sense.class);
		CLASSES_FOR_JAXB = classes.toArray(new Class<?>[classes.size()]);
	}

	private OutputStream outputStream;
	private String startingFrom = null;
	private String endingAt = null;

	/**
	 * @param dictionaryRepository
	 */
	@Autowired
	public ExportDictionaryCommandImpl(DictionaryRepository dictionaryRepository) {
		super(dictionaryRepository);
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see edu.harvard.fas.rregan.requel.dictionary.ExportDictionaryCommand#setStartingFrom(java.lang.String)
	 */
	public void setStartingFrom(String startingFrom) {
		this.startingFrom = startingFrom;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see edu.harvard.fas.rregan.requel.dictionary.ExportDictionaryCommand#setEndingAt(java.lang.String)
	 */
	public void setEndingAt(String endingAt) {
		this.endingAt = endingAt;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see edu.harvard.fas.rregan.requel.dictionary.ExportDictionaryCommand#setOutputStream(java.io.OutputStream)
	 */
	public void setOutputStream(OutputStream outputStream) {
		this.outputStream = outputStream;
	}

	protected OutputStream getOutputStream() {
		return outputStream;
	}

	/**
	 * @see edu.harvard.fas.rregan.command.Command#execute()
	 */
	@Override
	public void execute() {
		try {
			JAXBContext context = JAXBContext.newInstance(CLASSES_FOR_JAXB);
			Marshaller marshaller = context.createMarshaller();
			marshaller.setProperty(Marshaller.JAXB_FORMATTED_OUTPUT, Boolean.TRUE);
			marshaller.marshal(getDictionaryRepository().getDictionary(startingFrom, endingAt),
					getOutputStream());
		} catch (Exception e) {
			log.error(e, e);
		}
	}
}