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
import com.rreganjr.requel.gateway.GatewayException;
import com.rreganjr.requel.gateway.GatewayResult;
import com.rreganjr.requel.gateway.rest.BearerTokenSource;
import java.util.Map;
import picocli.CommandLine;
import picocli.CommandLine.Command;
import picocli.CommandLine.Option;

/**
 * {@code requel} — command-line front-end over the Requel gateway (issue #70, Phase A). A picocli
 * app that dispatches through the REST-backed gateway client; authentication is a PAT (#73) or an
 * OAuth access token (#83) supplied via {@code --token}/{@code REQUEL_TOKEN}.
 */
@Command(name = "requel", mixinStandardHelpOptions = true, version = "requel-cli 2.0.0-dev",
        description = "Command-line access to a Requel server via the gateway.",
        subcommands = {RunCommand.class, UpsertGoalCommand.class, CommandsCommand.class,
                ProjectsCommand.class, ProjectCommand.class, GlossaryCommand.class,
                OpenIssuesCommand.class, SearchCommand.class, EntityCommand.class, ContextCommand.class,
                LoginCommand.class, LogoutCommand.class})
public class RequelCli implements Runnable {

    @Option(names = "--url", paramLabel = "URL",
            defaultValue = "${env:REQUEL_URL:-http://localhost:8080}",
            description = "Requel base URL (env REQUEL_URL; default ${DEFAULT-VALUE}).")
    String url;

    @Option(names = "--token", paramLabel = "TOKEN",
            defaultValue = "${env:REQUEL_TOKEN}",
            description = "Bearer token: a PAT (reqpat_...) or OAuth access token (env REQUEL_TOKEN).")
    String token;

    @Option(names = "--output", paramLabel = "FORMAT", defaultValue = "TEXT",
            description = "Output format: ${COMPLETION-CANDIDATES} (default ${DEFAULT-VALUE}).")
    OutputFormat output;

    private final ObjectMapper mapper = new ObjectMapper();

    /** Persisted PATs, keyed by server URL. Package-visible so subcommands and tests can use it. */
    CredentialStore credentialStore = new CredentialStore();

    /**
     * The per-request bearer supplier passed to the REST gateway. Precedence: {@code --token} flag /
     * {@code REQUEL_TOKEN} env (both already merged into {@link #token} by picocli) &gt; OAuth tokens
     * from {@code requel login} (auto-refreshed) &gt; a PAT stored for this {@link #url}. Blank/absent
     * → no Authorization header. See {@link CliTokenSource}.
     */
    BearerTokenSource tokenSource() {
        return new CliTokenSource(token, url, credentialStore);
    }

    void printResult(GatewayResult result) {
        Object payload = result.result();
        if (output == OutputFormat.JSON) {
            try {
                System.out.println(mapper.writerWithDefaultPrettyPrinter().writeValueAsString(payload));
            } catch (JsonProcessingException e) {
                System.out.println(String.valueOf(payload));
            }
        } else {
            System.out.println(summarize(result.commandType(), payload));
        }
    }

    void printError(GatewayException e) {
        System.err.println("Error [" + e.getKind() + "]: " + e.getMessage());
    }

    private static String summarize(String commandType, Object payload) {
        if (payload instanceof Map<?, ?> map) {
            Object id = map.get("id");
            Object name = map.get("name");
            StringBuilder sb = new StringBuilder("OK ").append(commandType);
            if (name != null) {
                sb.append(": ").append(name);
            }
            if (id != null) {
                sb.append(" (id=").append(id).append(")");
            }
            return sb.toString();
        }
        return payload == null ? "OK " + commandType : "OK " + commandType + ": " + payload;
    }

    /** No subcommand given: show usage. */
    @Override
    public void run() {
        CommandLine.usage(this, System.out);
    }

    public static void main(String[] args) {
        RequelCli root = new RequelCli();
        CommandLine commandLine = new CommandLine(root)
                // Don't treat a leading '@' (e.g. --input-file @path) as a picocli argument file.
                .setExpandAtFiles(false)
                // Accept --output json/text as well as JSON/TEXT.
                .setCaseInsensitiveEnumValuesAllowed(true);
        // Runtime-discovered typed write subcommands (issue #103): best-effort, offline-tolerant —
        // when unreachable or invoking a built-in, only the static subcommands are registered.
        TypedCommands.registerFromServer(commandLine, root, args);
        int code = commandLine.execute(args);
        System.exit(code);
    }
}
