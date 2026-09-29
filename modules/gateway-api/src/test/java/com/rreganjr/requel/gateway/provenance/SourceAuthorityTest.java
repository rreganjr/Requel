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

import java.util.List;

import org.junit.jupiter.api.Test;

import com.rreganjr.requel.project.SourceAuthority;
import com.rreganjr.requel.project.SourceAuthority.Edge;
import com.rreganjr.requel.project.SourceAuthority.Winner;

/**
 * Issue #273: precedence between sources. Ids stand for sources; "1 defers to 2" is
 * {@code new Edge(1L, 2L)}.
 */
class SourceAuthorityTest {

    // The roundtable shape: the guide (1) defers to RUNBOOK.md (2), which defers to the repo (3);
    // the review (4) defers to the repo too. The tickets (5) are unordered.
    private static final List<Edge> ROUNDTABLE = List.of(
            new Edge(1L, 2L), new Edge(2L, 3L), new Edge(4L, 3L));

    @Test
    void aDirectEdgeMakesTheSuperiorWin() {
        assertThat(SourceAuthority.resolve(ROUNDTABLE, 1L, 2L).winner()).isEqualTo(Winner.B);
        assertThat(SourceAuthority.resolve(ROUNDTABLE, 2L, 1L).winner()).isEqualTo(Winner.A);
    }

    @Test
    void precedenceIsTransitiveAndReportsTheChainFromLoserToWinner() {
        SourceAuthority.Resolution resolution = SourceAuthority.resolve(ROUNDTABLE, 3L, 1L);
        assertThat(resolution.winner()).isEqualTo(Winner.A);
        assertThat(resolution.chain()).containsExactly(1L, 2L, 3L);
    }

    @Test
    void sourcesWithNoChainBetweenThemAreUnordered() {
        assertThat(SourceAuthority.resolve(ROUNDTABLE, 1L, 4L).winner()).isEqualTo(Winner.NONE);
        assertThat(SourceAuthority.resolve(ROUNDTABLE, 5L, 3L).winner()).isEqualTo(Winner.NONE);
        assertThat(SourceAuthority.resolve(ROUNDTABLE, 1L, 4L).chain()).isEmpty();
    }

    @Test
    void aSourceComparedWithItselfIsUnordered() {
        assertThat(SourceAuthority.resolve(ROUNDTABLE, 2L, 2L).winner()).isEqualTo(Winner.NONE);
    }

    @Test
    void aDiamondGivesOneShortestChainWithoutRepeats() {
        List<Edge> diamond = List.of(new Edge(1L, 2L), new Edge(1L, 3L), new Edge(2L, 4L),
                new Edge(3L, 4L));
        SourceAuthority.Resolution resolution = SourceAuthority.resolve(diamond, 1L, 4L);
        assertThat(resolution.winner()).isEqualTo(Winner.B);
        assertThat(resolution.chain()).hasSize(3).startsWith(1L).endsWith(4L).doesNotHaveDuplicates();
    }

    @Test
    void selfEdgesAndEveryCycleLengthAreDetected() {
        assertThat(SourceAuthority.wouldCycle(ROUNDTABLE, 1L, 1L)).isTrue();
        assertThat(SourceAuthority.wouldCycle(ROUNDTABLE, 2L, 1L)).isTrue();
        assertThat(SourceAuthority.wouldCycle(ROUNDTABLE, 3L, 1L)).isTrue();
        assertThat(SourceAuthority.wouldCycle(ROUNDTABLE, 1L, 3L)).isFalse();
        assertThat(SourceAuthority.wouldCycle(ROUNDTABLE, 5L, 1L)).isFalse();
    }

    @Test
    void aCycleInStoredDataNeverLoops() {
        List<Edge> cycle = List.of(new Edge(1L, 2L), new Edge(2L, 1L));
        assertThat(SourceAuthority.resolve(cycle, 1L, 3L).winner()).isEqualTo(Winner.NONE);
        assertThat(SourceAuthority.resolve(cycle, 1L, 2L).winner()).isEqualTo(Winner.B);
    }

    @Test
    void directSuperiorsAndSubordinates() {
        assertThat(SourceAuthority.superiorsOf(ROUNDTABLE, 1L)).containsExactly(2L);
        assertThat(SourceAuthority.subordinatesOf(ROUNDTABLE, 3L)).containsExactly(2L, 4L);
        assertThat(SourceAuthority.superiorsOf(ROUNDTABLE, 3L)).isEmpty();
    }
}
