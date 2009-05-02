/*
 * $Id: VerbNetImporter.java,v 1.1 2008/06/18 07:55:26 rregan Exp $
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
package edu.harvard.fas.rregan.nlp.impl.verbnet;

import java.io.File;

import javax.xml.XMLConstants;
import javax.xml.bind.JAXBContext;
import javax.xml.bind.Unmarshaller;
import javax.xml.validation.Schema;
import javax.xml.validation.SchemaFactory;

import org.apache.log4j.Logger;

import edu.harvard.fas.rregan.nlp.verbnet.VNCLASS;

/**
 * @author ron
 */
public class VerbNetImporter {
	protected static final Logger log = Logger.getLogger(VerbNetImporter.class);

	/**
	 * @param verbNetSchemaFile
	 * @param verbNetDataFile
	 * @return
	 * @throws Exception
	 */
	public VNCLASS loadVerbNetClass(File verbNetSchemaFile, File verbNetDataFile) throws Exception {
		JAXBContext context = JAXBContext.newInstance(VNCLASS.class);
		SchemaFactory schemaFactory = SchemaFactory.newInstance(XMLConstants.W3C_XML_SCHEMA_NS_URI);
		Schema schema = schemaFactory.newSchema(verbNetSchemaFile);
		Unmarshaller unmarshaller = context.createUnmarshaller();
		unmarshaller.setSchema(schema);
		return (VNCLASS) unmarshaller.unmarshal(verbNetDataFile);
	}
}
