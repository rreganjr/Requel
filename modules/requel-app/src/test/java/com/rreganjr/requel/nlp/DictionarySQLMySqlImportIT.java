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
package com.rreganjr.requel.nlp;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

import org.springframework.jdbc.core.JdbcTemplate;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.MySQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import com.rreganjr.nlp.dictionary.DictionaryRepository;

/**
 * Proves {@code DictionarySQLInitializer} really imports on a real MySQL (issue #288).
 * <p>
 * The {@code nlp/dictionary/*.sql.gz} dumps are mysqldump output — {@code SET NAMES utf8},
 * {@code SET @OLD_FOREIGN_KEY_CHECKS=@@FOREIGN_KEY_CHECKS}, {@code LOCK TABLES},
 * {@code UNLOCK TABLES}. None of that executes on H2, which is why the import was a silent no-op
 * there for years, and why the dialect guard added by #288 skips anything that is not MySQL. That
 * guard is only correct if the import genuinely works when it is <em>not</em> skipped; nothing in
 * the H2 suite can show that.
 * <p>
 * So this loads exactly one dump — {@code categorydef.sql.gz}, 45 rows, 4 KB — by overriding
 * {@code requel.dictionary.sql-files}. Importing the full corpus takes minutes (measured at 268s in
 * #287) and is what made CI's {@code Build & test} job double; it is never done by a test. One
 * small file exercises the same preamble, the same statement filter and the same commit.
 * <p>
 * Skipped (not failed) when Docker is not available, matching {@code DeleteProjectMySqlIT} — so a
 * green local build on a machine without Docker is not evidence this path works.
 *
 * @author ron
 */
@Testcontainers(disabledWithoutDocker = true)
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.MOCK)
@ActiveProfiles("test")
public class DictionarySQLMySqlImportIT {

	/** {@code categorydef.sql.gz} holds the 45 WordNet lexicographer categories. */
	private static final int WORDNET_CATEGORY_COUNT = 45;

	@Container
	static final MySQLContainer<?> MYSQL = new MySQLContainer<>("mysql:8.4")
			.withDatabaseName("requel")
			.withUsername("requel")
			.withPassword("requel")
			.withCommand("--character-set-server=utf8mb4",
					"--collation-server=utf8mb4_0900_ai_ci");

	@DynamicPropertySource
	static void mysql(DynamicPropertyRegistry registry) {
		if (!MYSQL.isRunning()) {
			MYSQL.start();
		}
		registry.add("spring.datasource.url", () -> MYSQL.getJdbcUrl()
				+ "?allowPublicKeyRetrieval=true&useSSL=false&serverTimezone=UTC");
		registry.add("spring.datasource.username", MYSQL::getUsername);
		registry.add("spring.datasource.password", MYSQL::getPassword);
		registry.add("spring.datasource.driver-class-name", () -> "com.mysql.cj.jdbc.Driver");
		registry.add("spring.jpa.database-platform", () -> "org.hibernate.dialect.MySQLDialect");
		// Flyway builds the schema; `categorydef` comes from V1__init.sql.
		registry.add("spring.jpa.hibernate.ddl-auto", () -> "none");
		registry.add("spring.flyway.enabled", () -> "true");
		registry.add("spring.flyway.baseline-on-migrate", () -> "true");
		registry.add("spring.flyway.baseline-version", () -> "0");
		// The `test` profile turns the initializer off (#287). Turn it back on for this one
		// context, and point it at a single small dump instead of the 17-file corpus.
		registry.add("requel.dictionary.sql-initializer.enabled", () -> "true");
		registry.add("requel.dictionary.sql-files", () -> "categorydef.sql.gz");
	}

	@Autowired
	private DictionaryRepository dictionaryRepository;

	@Autowired
	private JdbcTemplate jdbcTemplate;

	/**
	 * The initializer runs on {@code ApplicationReadyEvent}, so by the time a test method runs the
	 * import has already happened — or the context failed to start, which is the other half of
	 * what #288 asks for.
	 */
	@Test
	void theMysqldumpPreambleExecutesAndTheCategoriesLand() {
		assertFalse(dictionaryRepository.findCategories().isEmpty(),
				"categorydef.sql.gz should have imported on MySQL, not been skipped");
		assertEquals(WORDNET_CATEGORY_COUNT, dictionaryRepository.findCategories().size());
	}

	/**
	 * The assertion above passes whether one dump loads or all seventeen — {@code categorydef} is
	 * the first file either way — which is exactly how an earlier revision of this IT imported the
	 * whole ~14.4 MB corpus, added eight minutes to every CI run, and still went green. This is the
	 * assertion that fails when the file-list override stops working.
	 */
	@Test
	void onlyTheOneConfiguredDumpIsImported() {
		assertEquals(0, jdbcTemplate.queryForObject("SELECT COUNT(*) FROM word", Integer.class),
				"word.sql.gz is not in requel.dictionary.sql-files, so `word` must be empty");
	}
}
