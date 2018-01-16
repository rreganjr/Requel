/*
 * $Id$
 * Copyright 2008, 2009 Ron Regan Jr. All Rights Reserved.
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
package com.rreganjr.nlp.impl;

import java.io.DataInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.zip.GZIPInputStream;

import opennlp.maxent.GISModel;
import opennlp.maxent.io.BinaryGISModelReader;
import opennlp.maxent.io.GISModelReader;

import org.apache.log4j.Logger;

import com.rreganjr.nlp.NLPProcessor;

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
			return (GISModel)modelReader.getModel();
		}
		throw new IOException("Could not read model file " + modelFile + ", unknown type.");
	}
}
