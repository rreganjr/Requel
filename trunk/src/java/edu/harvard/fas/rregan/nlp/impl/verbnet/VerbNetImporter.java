/*
 * $Id: VerbNetImporter.java,v 1.1 2008/06/18 07:55:26 rregan Exp $
 * Copyright (c) 2008 Ron Regan Jr. All Rights Reserved.
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
