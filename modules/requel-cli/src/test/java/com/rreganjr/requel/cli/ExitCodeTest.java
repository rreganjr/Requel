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
package com.rreganjr.requel.cli;

import static org.assertj.core.api.Assertions.assertThat;

import com.rreganjr.requel.gateway.GatewayException;
import org.junit.jupiter.api.Test;

class ExitCodeTest {

    @Test
    void mapsEveryGatewayKind() {
        assertThat(ExitCode.forKind(GatewayException.Kind.UNAUTHORIZED)).isEqualTo(ExitCode.AUTH);
        assertThat(ExitCode.forKind(GatewayException.Kind.NOT_ALLOWED)).isEqualTo(ExitCode.NOT_ALLOWED);
        assertThat(ExitCode.forKind(GatewayException.Kind.NOT_FOUND)).isEqualTo(ExitCode.REQUEST_ERROR);
        assertThat(ExitCode.forKind(GatewayException.Kind.INVALID_INPUT)).isEqualTo(ExitCode.REQUEST_ERROR);
        assertThat(ExitCode.forKind(GatewayException.Kind.EXECUTION_ERROR)).isEqualTo(ExitCode.REQUEST_ERROR);
    }
}
