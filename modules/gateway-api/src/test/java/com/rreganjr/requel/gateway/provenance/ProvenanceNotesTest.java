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
package com.rreganjr.requel.gateway.provenance;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Optional;

import org.junit.jupiter.api.Test;

/**
 * Parser/renderer tests over the exact documented provenance-note format (issue #71). Shared
 * contract with the ticket-4 similarity matcher, so these lock the format down.
 */
class ProvenanceNotesTest {

    private static final ProvenanceDescriptor SAMPLE = ProvenanceDescriptor.of(
            "claude-desktop", "jira", "PROJ-123#ac2",
            "https://example.atlassian.net/browse/PROJ-123", "AC-2", "abc123def456");

    @Test
    void renderThenParseRoundTrips() {
        String note = ProvenanceNotes.render(SAMPLE);

        Optional<ProvenanceDescriptor> parsed = ProvenanceNotes.parse(note);

        assertThat(parsed).contains(SAMPLE);
        assertThat(ProvenanceNotes.hasProvenance(note)).isTrue();
    }

    @Test
    void renderEmitsTheDocumentedMarkerBlock() {
        String note = ProvenanceNotes.render(SAMPLE);

        // Assert on structure/content, not on JSON pretty-printer whitespace (which varies across
        // Jackson versions): the marked fence is present, the payload keys/values are there, and
        // the block is closed.
        assertThat(note).contains("```" + ProvenanceNotes.MARKER);
        assertThat(note).contains("\"sourceSystem\"").contains("\"jira\"");
        assertThat(note).contains("\"criterionHash\"").contains("\"abc123def456\"");
        assertThat(note.trim()).endsWith("```");
    }

    @Test
    void parseToleratesHumanProseBeforeTheBlock() {
        String note = ProvenanceNotes.render(SAMPLE); // default preamble prose precedes the block

        assertThat(note).startsWith("Auto-generated");
        assertThat(ProvenanceNotes.parse(note)).contains(SAMPLE);
    }

    @Test
    void parseWithCustomPreambleStillRoundTrips() {
        String note = ProvenanceNotes.render(SAMPLE, "See PROJ-123 for the source.");

        assertThat(note).startsWith("See PROJ-123 for the source.");
        assertThat(ProvenanceNotes.parse(note)).contains(SAMPLE);
    }

    @Test
    void parseIgnoresUnknownFieldsForForwardCompatibility() {
        String note = """
                ```requel-provenance
                {
                  "v": 2,
                  "sourceSystem": "github",
                  "sourceRef": "org/repo#42",
                  "criterionHash": "deadbeef",
                  "futureField": "ignored"
                }
                ```
                """;

        Optional<ProvenanceDescriptor> parsed = ProvenanceNotes.parse(note);

        assertThat(parsed).isPresent();
        assertThat(parsed.get().version()).isEqualTo(2);
        assertThat(parsed.get().sourceSystem()).isEqualTo("github");
        assertThat(parsed.get().sourceRef()).isEqualTo("org/repo#42");
        assertThat(parsed.get().criterionHash()).isEqualTo("deadbeef");
    }

    @Test
    void parseReturnsEmptyWhenMarkerAbsent() {
        assertThat(ProvenanceNotes.parse("just a plain human note, no block")).isEmpty();
        assertThat(ProvenanceNotes.parse("```java\nSystem.out.println();\n```")).isEmpty();
    }

    @Test
    void parseReturnsEmptyForNullOrBlank() {
        assertThat(ProvenanceNotes.parse(null)).isEmpty();
        assertThat(ProvenanceNotes.parse("")).isEmpty();
    }

    @Test
    void parseReturnsEmptyWhenBlockBodyIsNotJson() {
        String note = """
                ```requel-provenance
                this is not json
                ```
                """;

        assertThat(ProvenanceNotes.parse(note)).isEmpty();
    }

    @Test
    void parseReturnsEmptyWhenRequiredFieldsMissing() {
        // Missing sourceSystem/sourceRef/criterionHash -> the descriptor's constructor rejects it,
        // so an incomplete block is treated as not-a-provenance-note rather than a partial parse.
        String note = """
                ```requel-provenance
                { "v": 1, "client": "claude-desktop" }
                ```
                """;

        assertThat(ProvenanceNotes.parse(note)).isEmpty();
    }

    @Test
    void parsesFirstBlockWhenMultiplePresent() {
        String note = ProvenanceNotes.render(SAMPLE) + "\n\n"
                + ProvenanceNotes.render(ProvenanceDescriptor.of("cli", "linear", "LIN-9", null,
                        null, "9999"));

        // v1 attaches a single provenance note per goal; if two blocks ever coexist, the parser is
        // deterministic and returns the first.
        assertThat(ProvenanceNotes.parse(note)).contains(SAMPLE);
    }
}
