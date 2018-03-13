/*
 * $Id$
 * Copyright 2008, 2009 Ron Regan Jr. All Rights Reserved.
 * 
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
package com.rreganjr.nlp.impl.verbnet;

import java.io.File;

import javax.xml.XMLConstants;
import javax.xml.bind.JAXBContext;
import javax.xml.bind.Unmarshaller;
import javax.xml.validation.Schema;
import javax.xml.validation.SchemaFactory;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

//import com.rreganjr.nlp.verbnet.VNCLASS;

/**
 * This class is used to load the VerbNet 2.1 data from the source files into
 * the jaxb generated class in the verbnet-2.1.jar generated via the
 * gen-verbnet-jars ant task using the jaxb xjc on the vn_schema-3.xsd schema
 * file. Only the {@link com.rreganjr.nlp.VerbNetImporterTests} uses
 * this class, development of this was abandoned and the WordNet SQL Builder
 * database is used instead.
 * 
 * @author ron
 * @see {@link com.rreganjr.nlp.VerbNetImporterTests}
 * @deprecated this effort was abandoned and the WordNet SQL Builder database is
 *             used instead.
 */
@Deprecated
public class VerbNetImporter {
	protected static final Log log = LogFactory.getLog(VerbNetImporter.class);

	/**
	 * Load a single VerbNet class from XML to a VNCLASS object.
	 * 
	 * @param verbNetSchemaFile -
	 *            the schema used to generate the Java classes.
	 * @param verbNetDataFile -
	 *            the VerbNet XML class file.
	 * @return The VNCLASS object containing the verbnet objects loaded from the
	 *         specified VerbNet file.
	 * @throws Exception
	 */
	/*
	public VNCLASS loadVerbNetClass(File verbNetSchemaFile, File verbNetDataFile) throws Exception {
		JAXBContext context = JAXBContext.newInstance(VNCLASS.class);
		SchemaFactory schemaFactory = SchemaFactory.newInstance(XMLConstants.W3C_XML_SCHEMA_NS_URI);
		Schema schema = schemaFactory.newSchema(verbNetSchemaFile);
		Unmarshaller unmarshaller = context.createUnmarshaller();
		unmarshaller.setSchema(schema);
		return (VNCLASS) unmarshaller.unmarshal(verbNetDataFile);
	}
	*/
}
