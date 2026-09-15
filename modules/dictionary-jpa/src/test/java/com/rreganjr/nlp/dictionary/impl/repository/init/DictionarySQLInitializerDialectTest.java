/*
 * This file is part of Requel - the Collaborative Requirements
 * Elicitation System.
 *
 * Copyright 2026 Ron Regan Jr. All Rights Reserved.
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

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.contains;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.List;

import javax.sql.DataSource;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.jdbc.core.JdbcTemplate;

import com.rreganjr.nlp.dictionary.Category;
import com.rreganjr.nlp.dictionary.DictionaryRepository;
import com.rreganjr.platform.bootstrap.FatalInitializationException;

/**
 * The dialect branch of {@link DictionarySQLInitializer} (issue #288), with no database.
 * <p>
 * The dumps themselves live in {@code nlp-jpa}, which this module does not depend on, so these
 * cases load {@code nlp/dictionary/test-dump.sql} from this module's own test resources — the same
 * mysqldump shape, two rows. The real import against a real MySQL is covered by
 * {@code DictionarySQLMySqlImportIT} in {@code requel-app}, which loads a single 45-row dump.
 * Neither test ever imports the full corpus.
 *
 * @author ron
 */
class DictionarySQLInitializerDialectTest {

	private DictionaryRepository dictionaryRepository;
	private DataSource dataSource;
	private Connection connection;
	private Statement statement;
	private JdbcTemplate jdbcTemplate;

	@BeforeEach
	void setUp() throws SQLException {
		dictionaryRepository = mock(DictionaryRepository.class);
		dataSource = mock(DataSource.class);
		connection = mock(Connection.class);
		statement = mock(Statement.class);
		jdbcTemplate = mock(JdbcTemplate.class);

		when(jdbcTemplate.getDataSource()).thenReturn(dataSource);
		when(dataSource.getConnection()).thenReturn(connection);
		when(connection.createStatement()).thenReturn(statement);
		// an empty dictionary, so the initializer has work to do
		when(dictionaryRepository.findCategories()).thenReturn(List.<Category> of());
	}

	private DictionarySQLInitializer initializer(String files) {
		return new DictionarySQLInitializer(dictionaryRepository, jdbcTemplate, "nlp/dictionary/",
				files);
	}

	private void databaseProductIs(String product) throws SQLException {
		DatabaseMetaData metaData = mock(DatabaseMetaData.class);
		when(metaData.getDatabaseProductName()).thenReturn(product);
		when(connection.getMetaData()).thenReturn(metaData);
	}

	@Test
	void skipsSilentlyOnH2WithoutExecutingAnything() throws SQLException {
		databaseProductIs("H2");

		assertThatCode(() -> initializer("test-dump.sql").initialize())
				.doesNotThrowAnyException();

		verify(statement, never()).executeUpdate(anyString());
		verify(connection, never()).commit();
	}

	@Test
	void doesNotSkipOnMariaDbWhichReportsMySql() throws SQLException {
		// MariaDB's driver reports "MySQL"; the dumps load there, so it must not be skipped.
		databaseProductIs("MySQL");
		when(statement.executeUpdate(anyString())).thenReturn(1);

		initializer("test-dump.sql").initialize();

		verify(connection).commit();
	}

	@Test
	void gzippedDumpsAreDecompressed() throws SQLException {
		databaseProductIs("MySQL");
		when(statement.executeUpdate(anyString())).thenReturn(1);

		initializer("test-dump-gzipped.sql.gz").initialize();

		// LOCK TABLES is filtered; the inserts and the SET statements are not.
		verify(statement, never()).executeUpdate(contains("LOCK TABLES `categorydef` WRITE"));
		verify(statement, atLeastOnce()).executeUpdate(contains("insert into `categorydef`"));
		verify(connection).commit();
	}

	@Test
	void aFailedImportOnMySqlIsFatalAndSaysTheDictionaryIsEmpty() throws SQLException {
		databaseProductIs("MySQL");
		when(statement.executeUpdate(anyString()))
				.thenThrow(new SQLException("Unknown system variable 'FOREIGN_KEY_CHECKS'"));

		assertThatThrownBy(() -> initializer("test-dump.sql").initialize())
				.isInstanceOf(FatalInitializationException.class)
				.hasMessageContaining("the dictionary is empty")
				.hasMessageContaining("test-dump.sql")
				.hasMessageContaining("requel.dictionary.sql-initializer.enabled=false");

		verify(connection).rollback();
	}

	@Test
	void aMissingDumpIsFatalAndNamesTheFileRatherThanNpeing() throws SQLException {
		databaseProductIs("MySQL");

		assertThatThrownBy(() -> initializer("no-such-dump.sql.gz").initialize())
				.isInstanceOf(FatalInitializationException.class)
				.hasMessageContaining("no-such-dump.sql.gz")
				.cause().hasMessageContaining("not found on the classpath");
	}

	@Test
	void anAlreadyPopulatedDictionaryTouchesNoConnection() {
		when(dictionaryRepository.findCategories()).thenReturn(List.of(mock(Category.class)));

		initializer("test-dump.sql").initialize();

		verify(jdbcTemplate, never()).getDataSource();
	}

	@Test
	void autoCommitIsRestoredAndTheConnectionClosedAfterAFailure() throws SQLException {
		databaseProductIs("MySQL");
		when(connection.getAutoCommit()).thenReturn(true);
		when(statement.executeUpdate(anyString())).thenThrow(new SQLException("boom"));

		assertThatThrownBy(() -> initializer("test-dump.sql").initialize())
				.isInstanceOf(FatalInitializationException.class);

		verify(connection).setAutoCommit(false);
		verify(connection).setAutoCommit(true);
		verify(connection).close();
	}

	@Test
	void theDefaultFileListIsTheOneProductionImports() {
		// Guards the #288 transcription: the bundle's list, not the constant that never ran.
		assertThat(DictionarySQLInitializer.PROP_DICTIONARY_SQL_FILES_DEFAULT)
				.contains("vnroletype.sql", "vnselres.sql", "vnroleref.sql.gz", "custom_vn.sql")
				.doesNotContain("synset_definition_word.sql.gz", "semcor_file.sql.gz",
						"semcor_sentence.sql.gz", "semcor_sentence_word.sql.gz");
	}

	@Test
	void aJdbcTemplateWithNoDataSourceIsFatal() {
		when(jdbcTemplate.getDataSource()).thenReturn(null);

		assertThatThrownBy(() -> initializer("test-dump.sql").initialize())
				.isInstanceOf(FatalInitializationException.class)
				.hasMessageContaining("the dictionary is empty")
				.hasMessageContaining("no DataSource");
	}

	@Test
	void aConnectionFailureIsFatal() throws SQLException {
		when(dataSource.getConnection()).thenThrow(new SQLException("pool exhausted"));

		assertThatThrownBy(() -> initializer("test-dump.sql").initialize())
				.isInstanceOf(FatalInitializationException.class)
				.hasMessageContaining("could not obtain a database connection")
				.cause().hasMessageContaining("pool exhausted");
	}

	@Test
	void aNullDatabaseProductNameIsTreatedAsNotMySql() throws SQLException {
		// A driver is not obliged to return a product name; absent one, do not run mysqldump SQL.
		databaseProductIs(null);

		assertThatCode(() -> initializer("test-dump.sql").initialize())
				.doesNotThrowAnyException();

		verify(statement, never()).executeUpdate(anyString());
	}

	@Test
	void aFailedRollbackDoesNotMaskTheImportFailure() throws SQLException {
		databaseProductIs("MySQL");
		when(statement.executeUpdate(anyString())).thenThrow(new SQLException("import blew up"));
		doThrow(new SQLException("connection already dead")).when(connection).rollback();

		// The rollback failure is logged; what the caller sees is still why the import failed.
		assertThatThrownBy(() -> initializer("test-dump.sql").initialize())
				.isInstanceOf(FatalInitializationException.class)
				.hasMessageContaining("the dictionary is empty")
				.cause().hasMessageContaining("import blew up");
	}

	@Test
	void aFailedAutoCommitRestoreDoesNotFailASuccessfulImport() throws SQLException {
		databaseProductIs("MySQL");
		when(connection.getAutoCommit()).thenReturn(true);
		when(statement.executeUpdate(anyString())).thenReturn(1);
		doThrow(new SQLException("connection returned to the pool")).when(connection)
				.setAutoCommit(true);

		// Restoring autoCommit is housekeeping — it must not turn a good import into a failure.
		assertThatCode(() -> initializer("test-dump.sql").initialize())
				.doesNotThrowAnyException();

		verify(connection).commit();
	}
}
