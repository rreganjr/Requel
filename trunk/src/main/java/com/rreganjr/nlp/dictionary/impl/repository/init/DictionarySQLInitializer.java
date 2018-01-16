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
package com.rreganjr.nlp.dictionary.impl.repository.init;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.zip.GZIPInputStream;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Scope;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import com.rreganjr.AbstractSystemInitializer;
import net.sf.echopm.ResourceBundleHelper;
import com.rreganjr.nlp.dictionary.DictionaryRepository;

/**
 * Load the dictionary from SQL if no words exist.
 * 
 * @author ron
 */
@Component("dictionarySQLInitializer")
@Scope("prototype")
public class DictionarySQLInitializer extends AbstractSystemInitializer {

	/**
	 * The name of the property in the DictionarySQLInitializer.properties file
	 * that contains the path to the directory containing all the sql files, if
	 * not supplied the default file is "nlp/dictionary/"
	 */
	public static final String PROP_DICTIONARY_SQL_FILES_DIRECTORY = "DictionarySQLFilesDirectory";
	public static final String PROP_DICTIONARY_SQL_FILES_DIRECTORY_DEFAULT = "nlp/dictionary/";

	/**
	 * The name of the property in the DictionarySQLInitializer.properties file
	 * that contains a comma delimited list of sql file names in the order they
	 * should be loaded. The files are expected to be in the directory defined
	 * by PROP_DICTIONARY_SQL_FILES_DIRECTORY.<br>
	 * If not supplied the default list is "categorydef.sql.gz, word.sql.gz,
	 * morphdef.sql.gz, morphref.sql.gz, synset.sql.gz, sense.sql.gz,
	 * synset_definition_word.sql.gz, synset_subsumer_counts.sql.gz,
	 * linkdef.sql.gz, lexlinkref.sql.gz, semlinkref.sql.gz, vnclass.sql.gz,
	 * vnframedef.sql.gz, vnframeref.sql.gz, semcor_file.sql.gz,
	 * semcor_sentence.sql.gz, semcor_sentence_word.sql.gz"<br>
	 * NOTE: the files may be plain sql files or zipped sql files.
	 */
	public static final String PROP_DICTIONARY_SQL_FILES = "DictionarySQLFiles";
	public static final String PROP_DICTIONARY_SQL_FILES_DEFAULT = "categorydef.sql.gz, word.sql.gz, morphdef.sql.gz, morphref.sql.gz, synset.sql.gz, sense.sql.gz, synset_definition_word.sql.gz, synset_subsumer_counts.sql.gz, linkdef.sql.gz, lexlinkref.sql.gz, semlinkref.sql.gz, vnclass.sql.gz, vnframedef.sql.gz, vnframeref.sql.gz, semcor_file.sql.gz, semcor_sentence.sql.gz, semcor_sentence_word.sql.gz";

	private final DictionaryRepository dictionaryRepository;
	private final JdbcTemplate jdbcTemplate;

	/**
	 * @param dictionaryRepository
	 * @param jdbcTemplate
	 */
	@Autowired
	public DictionarySQLInitializer(DictionaryRepository dictionaryRepository,
			JdbcTemplate jdbcTemplate) {
		super(1);
		this.dictionaryRepository = dictionaryRepository;
		this.jdbcTemplate = jdbcTemplate;
	}

	@Override
	@Transactional(propagation = Propagation.REQUIRED)
	public void initialize() {
		if (dictionaryRepository.findCategories().isEmpty()) {
			Connection conn = null;
			try {
				ResourceBundleHelper resourceBundleHelper = new ResourceBundleHelper(
						DictionarySQLInitializer.class.getName());
				String dictionaryDirPath = resourceBundleHelper.getString(
						PROP_DICTIONARY_SQL_FILES_DIRECTORY,
						PROP_DICTIONARY_SQL_FILES_DIRECTORY_DEFAULT);
				conn = jdbcTemplate.getDataSource().getConnection();
				conn.setAutoCommit(false);
				Statement statement = conn.createStatement();

				String dictionaryFiles = resourceBundleHelper.getString(PROP_DICTIONARY_SQL_FILES,
						PROP_DICTIONARY_SQL_FILES_DEFAULT);
				for (String sqlFile : dictionaryFiles.split(",")) {
					loadSQLFile(dictionaryDirPath + sqlFile.trim(), statement);
				}
				conn.commit();
			} catch (Exception e) {
				if (conn != null) {
					try {
						conn.rollback();
					} catch (SQLException se) {
						log.error("could not rollback: " + se, se);
					}
				}
				log.error("could not load dictionary via SQL: " + e, e);
			}
		}
	}

	private void loadSQLFile(String path, Statement statement) throws IOException, SQLException {
		log.info("loading sql file: " + path);
		InputStream inputStream = getClass().getClassLoader().getResourceAsStream(path);
		if (path.endsWith(".gz")) {
			inputStream = new GZIPInputStream(inputStream);
		}

		BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream));
		StringBuilder sqlBuffer = new StringBuilder(1500);

		while (true) {
			String sql = readSQL(reader, sqlBuffer);
			if (sql.length() == 0) {
				break;
			}
			log.debug("sql = " + sql);
			if (!sql.toLowerCase().startsWith("lock tables")) {
				statement.executeUpdate(sql);
			}
			sqlBuffer.setLength(0);
		}
	}

	private static final int STATE_NORMAL = 0;
	private static final int STATE_START_COMMENT = 1;
	private static final int STATE_IN_COMMENT = 2;
	private static final int STATE_END_COMMENT = 3;
	private static final int STATE_IN_STRING = 4;
	private static final int STATE_ESCAPE = 5;

	protected String readSQL(Reader reader, StringBuilder queryBuffer) throws IOException {
		int state = 0;
		int ch;
		while ((ch = reader.read()) != -1) {
			if ((STATE_NORMAL == state) && (ch == '/')) {
				state = STATE_START_COMMENT;
			} else if ((STATE_ESCAPE == state)) {
				state = STATE_IN_STRING;
				queryBuffer.append((char) ch);
			} else if ((STATE_IN_STRING == state) && (ch == '\\')) {
				state = STATE_ESCAPE;
				queryBuffer.append('\\');
			} else if ((STATE_NORMAL == state) && (ch == '\'')) {
				state = STATE_IN_STRING;
				queryBuffer.append('\'');
			} else if ((STATE_IN_STRING == state) && (ch == '\'')) {
				state = STATE_NORMAL;
				queryBuffer.append('\'');
			} else if (STATE_START_COMMENT == state) {
				if (ch == '/') {
					throw new IOException("// unsupported");
				} else if (ch == '*') {
					state = STATE_IN_COMMENT;
				} else {
					state = STATE_NORMAL;
					queryBuffer.append('/');
					queryBuffer.append((char) ch);
				}
			} else if ((STATE_END_COMMENT == state) && (ch == '/')) {
				state = STATE_NORMAL;
			} else if (STATE_IN_COMMENT == state) {
				if (ch == '*') {
					state = STATE_END_COMMENT;
				}
				// ignore text in comments
			} else if ((STATE_NORMAL == state) || (STATE_IN_STRING == state)) {
				queryBuffer.append((char) ch);
				if ((STATE_IN_STRING != state) && (ch == ';')) {
					break;
				}
			}
		}
		return queryBuffer.toString().trim();
	}
}
