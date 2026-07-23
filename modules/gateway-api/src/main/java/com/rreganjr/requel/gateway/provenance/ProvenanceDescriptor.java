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

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;

/**
 * The machine-parseable payload recorded on an auto-generated goal's provenance {@code NOTE},
 * linking it back to the source tracker item it was derived from (issue #71). It is serialized as
 * a small JSON object inside a fenced {@code requel-provenance} block by {@link ProvenanceNotes};
 * the reconciliation lookup and the ticket-4 similarity matcher both read it back by parsing that
 * block.
 *
 * <p>The workflow is source-agnostic: {@code sourceSystem}/{@code sourceRef}/{@code sourceUrl}
 * form a generic descriptor that works for Jira, GitHub Issues, Linear, or any tracker. Requel
 * never talks to the tracker itself.</p>
 *
 * <p>Field components are annotated with {@link JsonProperty} so JSON binding does not depend on
 * the {@code -parameters} compiler flag, and unknown properties are ignored so a note written by a
 * newer format version still parses.</p>
 *
 * @param version       provenance format version (current: {@link #CURRENT_VERSION})
 * @param client        the external client that authored the goal (e.g. {@code claude-desktop})
 * @param sourceSystem  the tracker family (e.g. {@code jira}, {@code github})
 * @param sourceRef     a source-specific reference to the item/criterion (e.g. {@code PROJ-123})
 * @param sourceUrl     a human-openable URL to the source item (nullable)
 * @param criterionRef  a human-readable reference to the specific criterion (e.g. {@code AC-2},
 *                      nullable)
 * @param criterionHash SHA-256 hex over the canonically normalized criterion text; the stable
 *                      key used for provenance-based reconciliation
 */
@JsonIgnoreProperties(ignoreUnknown = true)
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonPropertyOrder({ "v", "client", "sourceSystem", "sourceRef", "sourceUrl", "criterionRef",
        "criterionHash" })
public record ProvenanceDescriptor(
        @JsonProperty("v") int version,
        @JsonProperty("client") String client,
        @JsonProperty("sourceSystem") String sourceSystem,
        @JsonProperty("sourceRef") String sourceRef,
        @JsonProperty("sourceUrl") String sourceUrl,
        @JsonProperty("criterionRef") String criterionRef,
        @JsonProperty("criterionHash") String criterionHash
) {

    /** The provenance format version emitted by this build. */
    public static final int CURRENT_VERSION = 1;

    public ProvenanceDescriptor {
        Objects.requireNonNull(sourceSystem, "sourceSystem");
        Objects.requireNonNull(sourceRef, "sourceRef");
        Objects.requireNonNull(criterionHash, "criterionHash");
    }

    /**
     * Convenience factory stamping the payload with {@link #CURRENT_VERSION}.
     */
    public static ProvenanceDescriptor of(String client, String sourceSystem, String sourceRef,
            String sourceUrl, String criterionRef, String criterionHash) {
        return new ProvenanceDescriptor(CURRENT_VERSION, client, sourceSystem, sourceRef, sourceUrl,
                criterionRef, criterionHash);
    }
}
