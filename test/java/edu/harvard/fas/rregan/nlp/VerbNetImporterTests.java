/*
 * $Id: VerbNetImporterTests.java,v 1.4 2009/01/26 10:19:06 rregan Exp $
 * Copyright (c) 2008 Ron Regan Jr. All Rights Reserved.
 */

package edu.harvard.fas.rregan.nlp;

import java.io.File;
import java.io.FileFilter;

import javax.xml.bind.JAXBElement;

import org.apache.log4j.Logger;

import edu.harvard.fas.rregan.TestCase;
import edu.harvard.fas.rregan.nlp.impl.verbnet.VerbNetImporter;
import edu.harvard.fas.rregan.nlp.verbnet.VNCLASS;
import edu.harvard.fas.rregan.nlp.verbnet.FRAMES.FRAME;
import edu.harvard.fas.rregan.nlp.verbnet.MEMBERS.MEMBER;

/**
 * This isn't a test, but test code for importing the verbnet files.
 * 
 * @author ron
 */
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
	 * @see edu.harvard.fas.rregan.TestCase#setUp()
	 */
	@Override
	protected void setUp() throws Exception {
		super.setUp();
	}

	/**
	 * @see edu.harvard.fas.rregan.TestCase#tearDown()
	 */
	@Override
	protected void tearDown() throws Exception {
		super.tearDown();
	}

	/**
	 * Test method for
	 * {@link edu.harvard.fas.rregan.nlp.impl.verbnet.VerbNetImporter#loadVerbNetClass(java.io.InputStream)}.
	 */
	public void testLoadVerbNetClass() throws Exception {
		String verbNetSchemaFilePath = "resources/nlp/verbnet-2.1/vn_schema-3.xsd";
		String verbNetDirectoryPath = "resources/nlp/verbnet-2.1/";
		String verbNetFilePath = "resources/nlp/verbnet-2.1/accompany-51.7.xml";
		File schemaFile = new File(getClass().getClassLoader().getResource(verbNetSchemaFilePath)
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
