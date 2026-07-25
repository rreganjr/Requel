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

import com.rreganjr.requel.gateway.rest.CommandInfo;
import com.rreganjr.requel.gateway.rest.RestGatewayCatalog;
import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.nio.charset.StandardCharsets;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.web.client.HttpClientErrorException;

class CommandsCommandTest {

    /** A catalog stub — overrides descriptors() so no network call is made. */
    private static RestGatewayCatalog catalogReturning(List<CommandInfo> commands) {
        return new RestGatewayCatalog("http://unused", () -> null) {
            @Override
            public List<CommandInfo> descriptors() {
                return commands;
            }
        };
    }

    private static RestGatewayCatalog catalogThrowing(HttpStatus status) {
        return new RestGatewayCatalog("http://unused", () -> null) {
            @Override
            public List<CommandInfo> descriptors() {
                throw new HttpClientErrorException(status);
            }
        };
    }

    private static CommandsCommand command(OutputFormat output, RestGatewayCatalog catalog) {
        RequelCli parent = new RequelCli();
        parent.url = "http://localhost:8080";
        parent.output = output;
        CommandsCommand cmd = new CommandsCommand();
        cmd.parent = parent;
        cmd.catalogOverride = catalog;
        return cmd;
    }

    @Test
    void listsCommandsAndReturnsSuccess() {
        RestGatewayCatalog catalog = catalogReturning(List.of(
                new CommandInfo("EditGoal", "EditGoalInput", "Edit Goal", null, true, null, null)));
        String out = captureStdout(() ->
                assertThat(command(OutputFormat.TEXT, catalog).call()).isEqualTo(ExitCode.SUCCESS));
        assertThat(out).contains("EditGoal").contains("Edit Goal");
    }

    @Test
    void emptyCatalogExplainsWritesMayBeDisabled() {
        String out = captureStdout(() ->
                assertThat(command(OutputFormat.TEXT, catalogReturning(List.of())).call())
                        .isEqualTo(ExitCode.SUCCESS));
        assertThat(out).contains("requel.gateway.write.enabled=false");
    }

    @Test
    void jsonOutputEmitsArray() {
        RestGatewayCatalog catalog = catalogReturning(List.of(
                new CommandInfo("EditGoal", "EditGoalInput", "Edit Goal", null, true, null, null)));
        String out = captureStdout(() ->
                assertThat(command(OutputFormat.JSON, catalog).call()).isEqualTo(ExitCode.SUCCESS));
        assertThat(out.trim()).startsWith("[").contains("\"commandType\" : \"EditGoal\"");
    }

    @Test
    void unauthorizedReturnsAuthCode() {
        assertThat(command(OutputFormat.TEXT, catalogThrowing(HttpStatus.UNAUTHORIZED)).call())
                .isEqualTo(ExitCode.AUTH);
    }

    @Test
    void otherHttpErrorReturnsRequestErrorCode() {
        assertThat(command(OutputFormat.TEXT, catalogThrowing(HttpStatus.INTERNAL_SERVER_ERROR)).call())
                .isEqualTo(ExitCode.REQUEST_ERROR);
    }

    private static String captureStdout(Runnable body) {
        PrintStream original = System.out;
        ByteArrayOutputStream buf = new ByteArrayOutputStream();
        System.setOut(new PrintStream(buf, true, StandardCharsets.UTF_8));
        try {
            body.run();
        } finally {
            System.setOut(original);
        }
        return buf.toString(StandardCharsets.UTF_8);
    }
}
