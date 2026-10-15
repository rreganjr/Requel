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

import static org.assertj.core.api.Assertions.assertThat;

import java.net.URL;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Stream;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

import com.rreganjr.nlp.dictionary.impl.repository.init.DictionaryInitializer;
import com.rreganjr.nlp.dictionary.impl.repository.init.DictionarySQLInitializer;
import com.rreganjr.nlp.dictionary.impl.repository.init.WordNetDefinitionWordsInitializer;
import com.rreganjr.nlp.dictionary.impl.repository.jpa.JpaDictionaryRepository;
import com.rreganjr.nlp.impl.OpenNLPParser;
import com.rreganjr.nlp.impl.OpenNLPTagger;
import com.rreganjr.nlp.impl.OpenNLPTokenizer;
import com.rreganjr.nlp.impl.Sentencizer;

/**
 * Issue #193: the NLP data moved out of {@code nlp-jpa}'s resources into the
 * {@code requel-nlp-data} jar fetched by {@code modules/nlp-data}, and 404 files nothing read were
 * dropped on the way. Every reader still loads by classpath path, and most of them swallow a
 * missing resource into a {@code log.error} at boot, so a pruning mistake or a later rename would
 * surface as quietly degraded NLP rather than a failure.
 * <p>
 * This test resolves every default data path the code names — read from the constants, not
 * copied — and requires each to be served from the data jar. That also fails if a copy of the data
 * ever reappears in a module's own resources and shadows the jar.
 * <p>
 * Deliberately absent: the JWNL WordNet database ({@code nlp/jwnl/wn30/index.sense} and friends).
 * It was pruned with the rest of the unused data in #193, and {@code WordNetSenseKeyInitializer},
 * the one class that would have read it, was removed in #314 (see doc/guides/NLP_DATA.md).
 */
class NlpDataResourcesTest {

	private static final String DATA_JAR = "requel-nlp-data-";

	static Stream<String> dataPaths() {
		List<String> paths = new ArrayList<>();
		// nlp-jpa: OpenNLP models
		paths.add(OpenNLPTokenizer.PROP_ENGLISH_TOKENIZER_MODEL_FILE_DEFAULT);
		paths.add(Sentencizer.PROP_ENGLISH_SENTENCE_DETECTOR_MODEL_FILE_DEFAULT);
		paths.add(OpenNLPParser.PROP_PARSER_HEAD_RULES_FILE_DEFAULT);
		paths.add(OpenNLPParser.PROP_PARSER_CHUNKER_MODEL_FILE_DEFAULT);
		paths.add(OpenNLPParser.PROP_PARSER_BUILD_MODEL_FILE_DEFAULT);
		paths.add(OpenNLPParser.PROP_PARSER_CHECK_MODEL_FILE_DEFAULT);
		paths.add(OpenNLPTagger.PROP_POSTAGGER_MODEL_FILE_DEFAULT);
		// dictionary-jpa: dictionary.xml.gz, the tagged-gloss files, the SQL dumps, jazzy
		paths.add(DictionaryInitializer.PROP_DICTIONARY_XML_FILE_DEFAULT);
		paths.addAll(prefixed(WordNetDefinitionWordsInitializer.PROP_WORDNET_MERGED_GLOSS_FILES_DIRECTORY_DEFAULT,
				WordNetDefinitionWordsInitializer.PROP_DWORDNET_MERGED_GLOSS_FILES_DEFAULT.split(",")));
		paths.addAll(prefixed(DictionarySQLInitializer.PROP_DICTIONARY_SQL_FILES_DIRECTORY_DEFAULT,
				DictionarySQLInitializer.PROP_DICTIONARY_SQL_FILES_DEFAULT.split(",")));
		paths.addAll(prefixed("", JpaDictionaryRepository.PROP_ENGLISH_DICTIONARY_FILES_DEFAULT.split("\\|")));
		// VerbNet, as VerbNetImporterTests reads it
		paths.add("nlp/verbnet-2.1/vn_schema-3.xsd");
		paths.add("nlp/verbnet-2.1/accompany-51.7.xml");
		// Directories the readers use as prefixes; they resolve only if the jar has directory entries.
		paths.add(WordNetDefinitionWordsInitializer.PROP_WORDNET_MERGED_GLOSS_FILES_DIRECTORY_DEFAULT);
		paths.add(DictionarySQLInitializer.PROP_DICTIONARY_SQL_FILES_DIRECTORY_DEFAULT);
		paths.add("nlp/verbnet-2.1/");
		return paths.stream();
	}

	private static List<String> prefixed(String prefix, String[] names) {
		return Arrays.stream(names).map(String::trim).filter(n -> !n.isEmpty()).map(n -> prefix + n).toList();
	}

	@ParameterizedTest(name = "{0}")
	@MethodSource("dataPaths")
	void defaultDataPathIsServedFromTheDataJar(String path) {
		URL url = NlpDataResourcesTest.class.getClassLoader().getResource(path);

		assertThat(url)
				.as("classpath resource %s (read by the NLP/dictionary code, see #193)", path)
				.isNotNull();
		assertThat(url.toString())
				.as("%s must come from the requel-nlp-data jar, not a module's own resources", path)
				.contains(DATA_JAR);
	}
}
