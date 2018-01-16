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
package com.rreganjr.nlp.impl.wordnet;

import java.io.IOException;
import java.net.URL;
import java.net.URLDecoder;
import java.util.Map;

import net.didion.jwnl.JWNLException;
import net.didion.jwnl.JWNLRuntimeException;
import net.didion.jwnl.dictionary.file.DictionaryFile;
import net.didion.jwnl.dictionary.file_manager.FileManager;
import net.didion.jwnl.dictionary.file_manager.FileManagerImpl;
import net.didion.jwnl.util.factory.Param;

/**
 * Extends the default JWNL file manager to use a classpath relative path if the
 * supplied path starts with "classpath:"
 */
public class ClassPathFileManagerImpl extends FileManagerImpl implements FileManager {

	/**
	 * Uninitialized FileManagerImpl.
	 */
	public ClassPathFileManagerImpl() {
	}

	/**
	 * Construct a file manager backed by a set of files contained in the
	 * default WN search directory.
	 * 
	 * @param searchDir
	 * @param dictionaryFileType
	 * @throws IOException
	 */
	public ClassPathFileManagerImpl(String searchDir, Class<?> dictionaryFileType)
			throws IOException {
		super(searchDir, dictionaryFileType);
	}

	/**
	 * This is a copy of the FileManagerImpl.create() method with the addtion of
	 * the code implementing support for the PATH starting with classpath: to
	 * search for the path relative to the classpath.
	 */
	@Override
	public Object create(Map params) throws JWNLException {
		Class<?> fileClass = null;
		try {
			fileClass = Class.forName(((Param) params.get(FILE_TYPE)).getValue());
		} catch (ClassNotFoundException ex) {
			throw new JWNLRuntimeException("DICTIONARY_EXCEPTION_002", ex);
		}
		checkFileType(fileClass);

		String path = ((Param) params.get(PATH)).getValue();
		try {
			if (path.startsWith("classpath:")) {
				URL dictPath = getClass().getResource(path.substring("classpath:".length()));
				path = URLDecoder.decode(dictPath.getPath(), "utf8");
			}
			return new ClassPathFileManagerImpl(path, fileClass);
		} catch (IOException ex) {
			throw new JWNLException("DICTIONARY_EXCEPTION_016", fileClass, ex);
		}
	}

	/**
	 * Checks the type to ensure it's valid.<br>
	 * NOTE: This is an exact copy of the checkFileType() method from
	 * FileManagerImpl because it is private but used by the create() method.
	 * 
	 * @param c
	 */
	private void checkFileType(Class<?> c) {
		if (!DictionaryFile.class.isAssignableFrom(c)) {
			throw new JWNLRuntimeException("DICTIONARY_EXCEPTION_003", c);
		}
	}
}