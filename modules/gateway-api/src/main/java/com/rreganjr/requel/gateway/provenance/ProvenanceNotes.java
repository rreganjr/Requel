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

import java.util.Objects;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;

/**
 * Renders and parses the provenance block embedded in an auto-generated goal's {@code NOTE}
 * (issue #71).
 *
 * <p>The note text carries a single fenced code block behind the stable marker
 * {@value #MARKER}:</p>
 *
 * <pre>
 * ```requel-provenance
 * { "v": 1, "sourceSystem": "jira", "sourceRef": "PROJ-123", "criterionHash": "..." }
 * ```
 * </pre>
 *
 * <p>Human-readable prose may precede the block; only the fenced {@value #MARKER} block is
 * parsed. This is the exact, documented, machine-parseable contract that the reconciliation
 * lookup and the ticket-4 similarity matcher rely on, so it is covered by parser tests.</p>
 *
 * <p>This type is stateless and thread-safe.</p>
 */
public final class ProvenanceNotes {

    /** The fence info-string marking the provenance block. */
    public static final String MARKER = "requel-provenance";

    /** Default prose shown above the machine block so the note reads sensibly in the UI. */
    private static final String DEFAULT_PREAMBLE =
            "Auto-generated from a source tracker item. Do not edit the block below — it is used "
                    + "by Requel to reconcile this goal on re-runs.";

    /**
     * Matches a fenced block whose info string is exactly the marker (optionally surrounded by
     * spaces), capturing the block body. DOTALL so the body may span lines; the opening fence must
     * start at the beginning of a line.
     */
    private static final Pattern BLOCK = Pattern.compile(
            "(?m)^```[ \\t]*" + Pattern.quote(MARKER) + "[ \\t]*\\r?\\n(.*?)\\r?\\n```",
            Pattern.DOTALL);

    private static final ObjectMapper MAPPER = new ObjectMapper()
            .enable(SerializationFeature.INDENT_OUTPUT);

    private ProvenanceNotes() {
    }

    /**
     * Build note text for {@code descriptor}: the default preamble followed by the fenced
     * provenance block.
     */
    public static String render(ProvenanceDescriptor descriptor) {
        return render(descriptor, DEFAULT_PREAMBLE);
    }

    /**
     * Build note text with a caller-supplied {@code preamble} (may be {@code null}/blank to emit
     * the bare block).
     */
    public static String render(ProvenanceDescriptor descriptor, String preamble) {
        Objects.requireNonNull(descriptor, "descriptor");
        String json;
        try {
            json = MAPPER.writeValueAsString(descriptor);
        } catch (JsonProcessingException e) {
            // ProvenanceDescriptor is a plain record of strings/int; serialization cannot fail in
            // practice. Rethrow unchecked so callers are not forced to handle an impossible case.
            throw new IllegalStateException("Failed to serialize provenance descriptor", e);
        }
        StringBuilder sb = new StringBuilder();
        if (preamble != null && !preamble.isBlank()) {
            sb.append(preamble.strip()).append("\n\n");
        }
        sb.append("```").append(MARKER).append('\n');
        sb.append(json).append('\n');
        sb.append("```");
        return sb.toString();
    }

    /**
     * Extract the provenance descriptor from {@code noteText}, if it contains a well-formed
     * {@value #MARKER} block. Returns empty when the marker is absent or the block body is not
     * parseable as a {@link ProvenanceDescriptor} (e.g. a note authored by a human).
     */
    public static Optional<ProvenanceDescriptor> parse(String noteText) {
        if (noteText == null || noteText.isEmpty()) {
            return Optional.empty();
        }
        Matcher m = BLOCK.matcher(noteText);
        if (!m.find()) {
            return Optional.empty();
        }
        String body = m.group(1).strip();
        try {
            return Optional.ofNullable(MAPPER.readValue(body, ProvenanceDescriptor.class));
        } catch (JsonProcessingException | IllegalArgumentException e) {
            return Optional.empty();
        }
    }

    /** True when {@code noteText} carries a parseable provenance block. */
    public static boolean hasProvenance(String noteText) {
        return parse(noteText).isPresent();
    }
}
