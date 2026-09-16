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
package com.rreganjr.nlp.dictionary.impl.repository.init;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Locale;
import java.util.zip.GZIPInputStream;

import javax.sql.DataSource;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Scope;
import org.springframework.core.env.Environment;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import com.rreganjr.platform.bootstrap.AbstractSystemInitializer;
import com.rreganjr.platform.bootstrap.FatalInitializationException;
import com.rreganjr.nlp.dictionary.DictionaryRepository;

/**
 * Load the WordNet/VerbNet dictionary from the bundled {@code nlp/dictionary/*.sql.gz} dumps when
 * the dictionary is empty.
 * <p>
 * The dumps are mysqldump output ({@code SET NAMES}, {@code SET @@FOREIGN_KEY_CHECKS},
 * {@code LOCK TABLES}) and execute only on MySQL. Before #288 this class ran in every Spring
 * context and simply failed on H2, logging one line and leaving the dictionary empty — a silent,
 * dialect-dependent no-op that nothing declared. It now checks the datasource: anything that is not
 * MySQL is skipped explicitly, and a failure on MySQL — where the import was both asked for and
 * possible — is fatal rather than logged. See {@code doc/DICTIONARY_LOADING.md}.
 * 
 * @author ron
 */
@ConditionalOnProperty(name = "requel.dictionary.sql-initializer.enabled",
		havingValue = "true",
		matchIfMissing = true)
@Component("dictionarySQLInitializer")
@Scope("prototype")
public class DictionarySQLInitializer extends AbstractSystemInitializer {

	/** Classpath directory holding the dumps. */
	public static final String PROP_DICTIONARY_SQL_FILES_DIRECTORY = "requel.dictionary.sql-files-directory";
	public static final String PROP_DICTIONARY_SQL_FILES_DIRECTORY_DEFAULT = "nlp/dictionary/";

	/** Comma delimited dump file names, in load order. */
	public static final String PROP_DICTIONARY_SQL_FILES = "requel.dictionary.sql-files";

	/**
	 * Comma delimited dump file names, in load order, relative to
	 * {@link #PROP_DICTIONARY_SQL_FILES_DIRECTORY_DEFAULT}. Override with
	 * {@code requel.dictionary.sql-files}. Files may be plain {@code .sql} or gzipped
	 * {@code .sql.gz}.
	 * <p>
	 * This list is the one production has always imported. Until #288 it lived in
	 * {@code DictionarySQLInitializer.properties}, read through a {@code ResourceBundle}, and the
	 * constant here named a <em>different</em> corpus — {@code synset_definition_word.sql.gz} and
	 * the three {@code semcor_*} dumps in place of the VerbNet {@code vnroletype.sql},
	 * {@code vnselres.sql}, {@code vnroleref.sql.gz} and {@code custom_vn.sql}. The bundle won,
	 * silently, so the constant was documentation of something that never ran. The bundle is gone
	 * and its list is here.
	 */
	public static final String PROP_DICTIONARY_SQL_FILES_DEFAULT =
			"categorydef.sql.gz, word.sql.gz, morphdef.sql.gz, morphref.sql.gz, synset.sql.gz, "
					+ "sense.sql.gz, synset_subsumer_counts.sql.gz, linkdef.sql.gz, "
					+ "lexlinkref.sql.gz, semlinkref.sql.gz, vnclass.sql.gz, vnframedef.sql.gz, "
					+ "vnframeref.sql.gz, vnroletype.sql, vnselres.sql, vnroleref.sql.gz, "
					+ "custom_vn.sql";

	private final DictionaryRepository dictionaryRepository;
	private final JdbcTemplate jdbcTemplate;
	private final String dictionaryDirPath;
	private final String dictionaryFiles;

	/**
	 * Configuration is read from the {@link Environment} rather than through {@code @Value}
	 * placeholders. In this application a {@code @Value} placeholder resolves against
	 * {@code application*.properties} but <em>not</em> against properties a test supplies through
	 * {@code @TestPropertySource} or {@code @DynamicPropertySource}, while
	 * {@code Environment.getProperty} sees all of them — which is why {@code @ConditionalOnProperty}
	 * honoured the enable flag here while a {@code @Value} on this file list silently took its
	 * default and imported the whole corpus (issue #288). The underlying placeholder defect is #293;
	 * this class does not depend on it.
	 */
	@Autowired
	public DictionarySQLInitializer(DictionaryRepository dictionaryRepository,
			JdbcTemplate jdbcTemplate, Environment environment) {
		this(dictionaryRepository, jdbcTemplate,
				environment.getProperty(PROP_DICTIONARY_SQL_FILES_DIRECTORY,
						PROP_DICTIONARY_SQL_FILES_DIRECTORY_DEFAULT),
				environment.getProperty(PROP_DICTIONARY_SQL_FILES,
						PROP_DICTIONARY_SQL_FILES_DEFAULT));
	}

	/**
	 * Direct-value constructor, for tests and for any caller that already knows what it wants
	 * loaded.
	 *
	 * @param dictionaryDirPath classpath directory holding the dumps
	 * @param dictionaryFiles comma delimited dump file names, in load order
	 */
	public DictionarySQLInitializer(DictionaryRepository dictionaryRepository,
			JdbcTemplate jdbcTemplate, String dictionaryDirPath, String dictionaryFiles) {
		super(1);
		this.dictionaryRepository = dictionaryRepository;
		this.jdbcTemplate = jdbcTemplate;
		this.dictionaryDirPath = dictionaryDirPath;
		this.dictionaryFiles = dictionaryFiles;
	}

	@Override
	@Transactional(propagation = Propagation.REQUIRED)
	public void initialize() {
		if (!dictionaryRepository.findCategories().isEmpty()) {
			return;
		}
		DataSource dataSource = jdbcTemplate.getDataSource();
		if (dataSource == null) {
			throw new FatalInitializationException("the dictionary is empty: the JdbcTemplate has"
					+ " no DataSource, so the dictionary could not be imported.");
		}
		try (Connection conn = dataSource.getConnection()) {
			if (!isMySQL(conn)) {
				return;
			}
			importDictionary(conn);
		} catch (SQLException e) {
			throw new FatalInitializationException("the dictionary is empty: could not obtain a"
					+ " database connection to import it.", e);
		}
	}

	/**
	 * The dumps are mysqldump output and only execute on MySQL, so anything else is skipped — once,
	 * visibly, saying what will be missing and where the alternative is. MariaDB's driver also
	 * reports "MySQL", which is what we want: the dumps load there too.
	 */
	private boolean isMySQL(Connection conn) throws SQLException {
		String product = conn.getMetaData().getDatabaseProductName();
		if (product != null && product.toLowerCase(Locale.ROOT).contains("mysql")) {
			return true;
		}
		log.info("dictionary SQL import skipped: the datasource reports '" + product
				+ "', not MySQL. The " + dictionaryDirPath + "*.sql.gz dumps are mysqldump output"
				+ " and execute only on MySQL, so the dictionary is empty in this context."
				+ " Tests that need dictionary data call"
				+ " AbstractIntegrationTestCase.ensureDictionaryLoaded() (dictionary.xml.gz)."
				+ " See doc/DICTIONARY_LOADING.md.");
		return false;
	}

	/**
	 * Import every configured dump in one transaction on {@code conn}. A failure here is fatal: the
	 * caller asked for the import (the property is on), the dialect can do it, and it did not
	 * happen — which before #288 was logged and stepped over, leaving an empty dictionary behind a
	 * passing build.
	 */
	private void importDictionary(Connection conn) throws SQLException {
		// Say what is about to be imported, not just each file as it goes by. The resolved list is
		// the difference between a 4 KB fixture and the ~14.4 MB corpus, and without it a context
		// that quietly fell back to the default list looks identical in the log to one that did
		// not (issue #288).
		log.info("importing the dictionary from " + dictionaryDirPath + ": " + dictionaryFiles);
		boolean autoCommit = conn.getAutoCommit();
		String currentFile = null;
		try (Statement statement = conn.createStatement()) {
			conn.setAutoCommit(false);
			for (String sqlFile : dictionaryFiles.split(",")) {
				currentFile = dictionaryDirPath + sqlFile.trim();
				loadSQLFile(currentFile, statement);
			}
			conn.commit();
		} catch (Exception e) {
			try {
				conn.rollback();
			} catch (SQLException se) {
				log.error("could not rollback: " + se, se);
			}
			throw new FatalInitializationException("the dictionary is empty: loading '" + currentFile
					+ "' failed and the import was rolled back, so no dictionary data was written."
					+ " Set requel.dictionary.sql-initializer.enabled=false to start without it.",
					e);
		} finally {
			try {
				conn.setAutoCommit(autoCommit);
			} catch (SQLException se) {
				log.error("could not restore autoCommit on the dictionary connection: " + se, se);
			}
		}
	}

	private void loadSQLFile(String path, Statement statement) throws IOException, SQLException {
		log.info("loading sql file: " + path);
		InputStream resource = getClass().getClassLoader().getResourceAsStream(path);
		if (resource == null) {
			// Previously this fell through to new GZIPInputStream(null) and the NPE was swallowed
			// by the same catch as a SQL error, so a renamed dump and a broken dump looked alike.
			throw new IOException("dictionary SQL file not found on the classpath: " + path);
		}
		InputStream inputStream = path.endsWith(".gz") ? new GZIPInputStream(resource) : resource;

		try (BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream))) {
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
