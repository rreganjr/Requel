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

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.rreganjr.requel.gateway.rest.CommandInfo;
import com.rreganjr.requel.gateway.rest.RestGatewayCatalog;
import java.util.List;
import java.util.concurrent.Callable;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestClientResponseException;
import picocli.CommandLine.Command;
import picocli.CommandLine.ParentCommand;

/**
 * {@code requel commands} — lists the write commands the server currently exposes, fetched at
 * runtime from {@code /api/gateway/commands/descriptors}. The list reflects the live server, so it
 * is empty when the server has writes disabled ({@code requel.gateway.write.enabled=false}). This is
 * the runtime-primary discovery from the #70 plan: the CLI's command surface is derived from the
 * server's catalog rather than hard-coded, keeping CLI and MCP in lockstep with the gateway policy.
 */
@Command(name = "commands",
        description = "List the gateway write commands the server exposes (input: requel run <type>).")
public class CommandsCommand implements Callable<Integer> {

    @ParentCommand
    RequelCli parent;

    private final ObjectMapper mapper = new ObjectMapper();

    /** Test seam: when set, used instead of building a {@link RestGatewayCatalog} from the parent. */
    RestGatewayCatalog catalogOverride;

    @Override
    public Integer call() {
        RestGatewayCatalog catalog = (catalogOverride != null) ? catalogOverride
                : new RestGatewayCatalog(parent.url, parent.tokenSource());
        List<CommandInfo> commands;
        try {
            commands = catalog.descriptors();
        } catch (RestClientResponseException e) {
            System.err.println("Error: " + e.getStatusCode() + " " + e.getStatusText());
            int status = e.getStatusCode().value();
            return (status == 401 || status == 403) ? ExitCode.AUTH : ExitCode.REQUEST_ERROR;
        } catch (RestClientException e) {
            System.err.println("Failed to reach Requel: " + e.getMessage());
            return ExitCode.REQUEST_ERROR;
        }

        if (parent.output == OutputFormat.JSON) {
            printJson(commands);
        } else {
            printText(commands);
        }
        return ExitCode.SUCCESS;
    }

    private void printJson(List<CommandInfo> commands) {
        try {
            System.out.println(mapper.writerWithDefaultPrettyPrinter().writeValueAsString(commands));
        } catch (JsonProcessingException e) {
            System.out.println(String.valueOf(commands));
        }
    }

    private static void printText(List<CommandInfo> commands) {
        if (commands.isEmpty()) {
            System.out.println("No commands exposed (server writes may be disabled: "
                    + "requel.gateway.write.enabled=false).");
            return;
        }
        int width = commands.stream().mapToInt(c -> c.commandType().length()).max().orElse(0);
        for (CommandInfo c : commands) {
            String input = c.inputType() != null ? "  (" + c.inputType() + ")" : "";
            System.out.printf("%-" + width + "s  %s%s%n",
                    c.commandType(), c.title() != null ? c.title() : "", input);
        }
    }
}
