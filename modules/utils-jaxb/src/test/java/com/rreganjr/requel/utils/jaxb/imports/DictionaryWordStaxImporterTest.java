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
package com.rreganjr.requel.utils.jaxb.imports;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.rreganjr.requel.imports.ImportException;
import com.rreganjr.requel.imports.project.DictionaryWordImportDraft;
import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.stream.Collectors;
import org.junit.jupiter.api.Test;

/**
 * The {@code <dictionary>} reader (issue #313).
 * <p>
 * The happy path is covered end to end by the round-trip ITs; what those cannot reach is what this
 * importer does with a document that is not the one it expects — no dictionary block, an unfamiliar
 * child element, a nested element sharing a name, malformed XML. Those branches decide whether a
 * project file written by a future version imports quietly or takes the whole import down with it.
 *
 * @author ron
 */
class DictionaryWordStaxImporterTest {

    private final DictionaryWordStaxImporter importer = new DictionaryWordStaxImporter();

    @Test
    void readsEveryWordInTheDictionaryBlock() {
        List<DictionaryWordImportDraft> drafts = read("""
                <project xmlns="http://www.rreganjr.com/requel">
                  <dictionary>
                    <dictionaryWord lemma="requelspeak" phoneticCode="RKLSPK"/>
                    <dictionaryWord lemma="Elicitron"/>
                  </dictionary>
                </project>
                """);

        assertThat(drafts).extracting(DictionaryWordImportDraft::getLemma)
                .containsExactly("requelspeak", "Elicitron");
    }

    /**
     * Every project file written before this ticket. The importer runs against all of them and must
     * come back empty rather than failing.
     */
    @Test
    void aDocumentWithNoDictionaryBlockYieldsNothing() {
        List<DictionaryWordImportDraft> drafts = read("""
                <project xmlns="http://www.rreganjr.com/requel">
                  <glossary>
                    <term id="TRM_1"><name>a term</name></term>
                  </glossary>
                </project>
                """);

        assertThat(drafts).isEmpty();
    }

    /**
     * A child the importer does not recognise is skipped, not treated as a word and not fatal —
     * which is what lets a newer file import into an older installation.
     */
    @Test
    void unknownChildrenOfTheDictionaryAreSkipped() {
        List<DictionaryWordImportDraft> drafts = read("""
                <project xmlns="http://www.rreganjr.com/requel">
                  <dictionary>
                    <someFutureThing lemma="ignored"/>
                    <dictionaryWord lemma="requelspeak"/>
                  </dictionary>
                </project>
                """);

        assertThat(drafts).extracting(DictionaryWordImportDraft::getLemma)
                .containsExactly("requelspeak");
    }

    /**
     * Reading stops at the end of the dictionary block: a {@code dictionaryWord} elsewhere in the
     * document is not a project dictionary word.
     */
    @Test
    void readingStopsAtTheEndOfTheDictionaryBlock() {
        List<DictionaryWordImportDraft> drafts = read("""
                <project xmlns="http://www.rreganjr.com/requel">
                  <dictionary>
                    <dictionaryWord lemma="inside"/>
                  </dictionary>
                  <dictionaryWord lemma="outside"/>
                </project>
                """);

        assertThat(drafts).extracting(DictionaryWordImportDraft::getLemma)
                .containsExactly("inside");
    }

    @Test
    void malformedXmlFailsAsAnImportException() {
        assertThatThrownBy(() -> read("<project xmlns=\"http://www.rreganjr.com/requel\"><dictionary>"))
                .isInstanceOf(ImportException.class)
                .hasMessageContaining("failed to read the project dictionary");
    }

    @Test
    void theMapperRejectsAMissingPayload() {
        assertThatThrownBy(() -> new DictionaryWordImportXmlMapper().toDraft(null))
                .isInstanceOf(ImportException.class)
                .hasMessageContaining("dictionary word XML payload is required");
    }

    private List<DictionaryWordImportDraft> read(String xml) {
        return importer.readWords(
                new ByteArrayInputStream(xml.getBytes(StandardCharsets.UTF_8)))
                .stream().collect(Collectors.toList());
    }
}
