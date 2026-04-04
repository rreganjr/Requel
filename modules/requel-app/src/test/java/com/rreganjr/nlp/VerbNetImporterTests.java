/*
 * This file is part of Requel - the Collaborative Requirements
 * Elicitation System.
 *
 * Copyright 2008, 2009, 2025 Ron Regan Jr. All Rights Reserved.
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
package com.rreganjr.nlp;

import java.io.File;
import java.io.FileFilter;

import javax.xml.bind.JAXBElement;

import org.apache.log4j.Logger;

import com.rreganjr.TestCase;
import com.rreganjr.nlp.impl.verbnet.VerbNetImporter;
import org.junit.Ignore;
//import com.rreganjr.nlp.verbnet.VNCLASS;
//import com.rreganjr.nlp.verbnet.FRAMES.FRAME;
//import com.rreganjr.nlp.verbnet.MEMBERS.MEMBER;

/**
 * This isn't a test, but test code for importing the verbnet files.
 * 
 * @author ron
 */
@Ignore
public class VerbNetImporterTests extends TestCase {
	protected static final Logger log = Logger.getLogger(VerbNetImporter.class);

	private final VerbNetImporter importer = new VerbNetImporter();

	/**
	 * @param name
	 */
	public VerbNetImporterTests(String name) {
		super(name);
	}

	/**
	 * @see com.rreganjr.TestCase#setUp()
	 */
	@Override
	protected void setUp() throws Exception {
		super.setUp();
	}

	/**
	 * @see com.rreganjr.TestCase#tearDown()
	 */
	@Override
	protected void tearDown() throws Exception {
		super.tearDown();
	}

	/**
	 * Test method for
	 * {@link com.rreganjr.nlp.impl.verbnet.VerbNetImporter#loadVerbNetClass(java.io.InputStream)}.
	 */
	public void testLoadVerbNetClass() throws Exception {
		String verbNetSchemaFilePath = "nlp/verbnet-2.1/vn_schema-3.xsd";
		String verbNetDirectoryPath = "nlp/verbnet-2.1/";
		String verbNetFilePath = "nlp/verbnet-2.1/accompany-51.7.xml";
/*		File schemaFile = new File(getClass().getClassLoader().getResource(verbNetSchemaFilePath)
				.toURI());
		File vnDirectory = new File(getClass().getClassLoader().getResource(verbNetDirectoryPath)
				.toURI());
		if (vnDirectory.isDirectory()) {
			int fileErrorCount = 0;
			for (File dataFile : vnDirectory.listFiles(new FileFilter() {
				@Override
				public boolean accept(File pathname) {
					return pathname.toString().endsWith(".xml");
				}
			})) {
				log.debug("File: " + dataFile.getName());
				try {
					VNCLASS vnclass = importer.loadVerbNetClass(schemaFile, dataFile);
					if (true) {
						for (MEMBER member : vnclass.getMEMBERS().getMEMBERS()) {
							log.debug("member: " + member.getName() + " wns: " + member.getWns());
						}
						for (FRAME frame : vnclass.getFRAMES().getFRAMES()) {
							log.debug(frame.getDESCRIPTION().getSecondary());
							for (JAXBElement<?> syntaxElement : frame.getSYNTAX()
									.getNPSAndADVSAndADJS()) {
								log.debug(syntaxElement.getName());
							}
						}
					}
				} catch (Exception e) {
					fileErrorCount++;
					log.error(e);
				}
			}
			log.error("file errors: " + fileErrorCount);
		}
		*/
		// File dataFile = new
		// File(getClass().getClassLoader().getResource(verbNetFilePath).toURI());
	}

	private static class XMLFileFilter implements FileFilter {

		@Override
		public boolean accept(File pathname) {
			return pathname.toString().endsWith(".xml");
		}

	}
}
