/*
 * $Id: AbstractOpenNLPTool.java,v 1.2 2009/01/24 11:08:38 rregan Exp $
 * Copyright (c) 2008 Ron Regan Jr. All Rights Reserved.
 */

package edu.harvard.fas.rregan.nlp.impl;

import java.io.DataInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.zip.GZIPInputStream;

import opennlp.maxent.GISModel;
import opennlp.maxent.io.BinaryGISModelReader;
import opennlp.maxent.io.GISModelReader;

import org.apache.log4j.Logger;

import edu.harvard.fas.rregan.nlp.NLPProcessor;

/**
 * Base class for OpenNLP tool wrappers with helpers for loading GIS models.
 * 
 * @author ron
 * @param <T> -
 *            the type of results returned by the process method
 */
public abstract class AbstractOpenNLPTool<T> implements NLPProcessor<T> {
	private static final Logger log = Logger.getLogger(AbstractOpenNLPTool.class);

	protected static GISModel readGISModel(String modelFile) throws IOException {
		log.debug("loading GISModel file " + modelFile);
		InputStream modelInputStream = Sentencizer.class.getClassLoader().getResourceAsStream(
				modelFile);
		if (modelFile.endsWith(".gz")) {
			modelInputStream = new GZIPInputStream(modelInputStream);
			modelFile = modelFile.substring(0, modelFile.length() - 3);
		}

		GISModelReader modelReader = null;
		if (modelFile.endsWith(".bin")) {
			modelReader = new BinaryGISModelReader(new DataInputStream(modelInputStream));
		} else {
			log.error("unsupported GISModel file format " + modelFile);
			// is this the only supported type?
		}

		if (modelReader != null) {
			return modelReader.getModel();
		}
		throw new IOException("Could not read model file " + modelFile + ", unknown type.");
	}
}
