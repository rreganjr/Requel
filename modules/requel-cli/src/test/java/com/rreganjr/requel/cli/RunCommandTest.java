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

import com.rreganjr.requel.gateway.CommandGateway;
import com.rreganjr.requel.gateway.GatewayException;
import com.rreganjr.requel.gateway.GatewayResult;
import java.util.Map;
import org.junit.jupiter.api.Test;

class RunCommandTest {

    /** Build a RunCommand wired to a parent (TEXT output) and a stub gateway. */
    private static RunCommand runCommand(String inputJson, CommandGateway gateway) {
        RequelCli parent = new RequelCli();
        parent.url = "http://localhost:8080";
        parent.output = OutputFormat.TEXT;
        RunCommand run = new RunCommand();
        run.parent = parent;
        run.commandType = "EditGoal";
        run.inputJson = inputJson;
        run.gatewayOverride = gateway;
        return run;
    }

    private static CommandGateway throwing(GatewayException.Kind kind) {
        return request -> {
            throw new GatewayException(kind, "boom");
        };
    }

    @Test
    void successReturnsZero() {
        CommandGateway ok = request -> new GatewayResult(request.commandType(), Map.of("id", 1, "name", "G"));
        assertThat(runCommand("{\"name\":\"G\"}", ok).call()).isEqualTo(ExitCode.SUCCESS);
    }

    @Test
    void unauthorizedReturnsAuthCode() {
        assertThat(runCommand("{}", throwing(GatewayException.Kind.UNAUTHORIZED)).call())
                .isEqualTo(ExitCode.AUTH);
    }

    @Test
    void notAllowedReturnsNotAllowedCode() {
        assertThat(runCommand("{}", throwing(GatewayException.Kind.NOT_ALLOWED)).call())
                .isEqualTo(ExitCode.NOT_ALLOWED);
    }

    @Test
    void executionErrorReturnsRequestErrorCode() {
        assertThat(runCommand("{}", throwing(GatewayException.Kind.EXECUTION_ERROR)).call())
                .isEqualTo(ExitCode.REQUEST_ERROR);
    }

    @Test
    void malformedInputJsonReturnsUsageCodeWithoutCallingGateway() {
        CommandGateway neverCalled = request -> {
            throw new AssertionError("gateway must not be called when input is invalid");
        };
        assertThat(runCommand("{not json", neverCalled).call()).isEqualTo(ExitCode.USAGE);
    }

    @Test
    void bothInputAndInputFileIsUsageError() {
        RunCommand run = runCommand("{}", request -> new GatewayResult("EditGoal", null));
        run.inputFile = java.nio.file.Path.of("ignored.json");
        assertThat(run.call()).isEqualTo(ExitCode.USAGE);
    }
}
