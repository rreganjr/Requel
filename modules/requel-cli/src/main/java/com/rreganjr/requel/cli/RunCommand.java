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

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.rreganjr.requel.gateway.CommandGateway;
import com.rreganjr.requel.gateway.GatewayException;
import com.rreganjr.requel.gateway.GatewayRequest;
import com.rreganjr.requel.gateway.GatewayResult;
import com.rreganjr.requel.gateway.rest.RestCommandGateway;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;
import java.util.concurrent.Callable;
import picocli.CommandLine.Command;
import picocli.CommandLine.Option;
import picocli.CommandLine.Parameters;
import picocli.CommandLine.ParentCommand;

/**
 * The generic gateway dispatch: {@code requel run <CommandType> --input '{...}'}. Sends the input
 * through the REST {@link CommandGateway} to {@code /api/gateway/commands/<CommandType>} and prints
 * the result. Returns a {@link ExitCode} keyed on the outcome. This is the always-available core;
 * typed convenience subcommands (runtime-discovered) are a later milestone.
 */
@Command(name = "run",
        description = "Run a gateway command, e.g. requel run EditGoal --input '{\"projectName\":\"Demo\",\"name\":\"G\"}'")
public class RunCommand implements Callable<Integer> {

    @ParentCommand
    RequelCli parent;

    @Parameters(index = "0", paramLabel = "COMMAND_TYPE",
            description = "Registered command type, e.g. EditGoal.")
    String commandType;

    @Option(names = "--input", paramLabel = "JSON",
            description = "Command input as a JSON object string.")
    String inputJson;

    @Option(names = "--input-file", paramLabel = "PATH",
            description = "Path to a file containing the JSON input object.")
    Path inputFile;

    private final ObjectMapper mapper = new ObjectMapper();

    /** Test seam: when set, used instead of building a {@link RestCommandGateway} from the parent. */
    CommandGateway gatewayOverride;

    @Override
    public Integer call() {
        Map<String, Object> input;
        try {
            input = parseInput();
        } catch (IllegalArgumentException | IOException e) {
            System.err.println("Invalid input: " + e.getMessage());
            return ExitCode.USAGE;
        }

        CommandGateway gateway = (gatewayOverride != null) ? gatewayOverride
                : new RestCommandGateway(parent.url, parent.tokenSource());
        try {
            GatewayResult result = gateway.execute(new GatewayRequest(commandType, input));
            parent.printResult(result);
            return ExitCode.SUCCESS;
        } catch (GatewayException e) {
            parent.printError(e);
            return ExitCode.forKind(e.getKind());
        }
    }

    /** @return the parsed JSON input as a map, or {@code null} when no input was supplied. */
    private Map<String, Object> parseInput() throws IOException {
        if (inputJson != null && inputFile != null) {
            throw new IllegalArgumentException("use only one of --input or --input-file");
        }
        String json = null;
        if (inputJson != null) {
            json = inputJson;
        } else if (inputFile != null) {
            json = Files.readString(inputFile);
        }
        if (json == null || json.isBlank()) {
            return null;
        }
        return mapper.readValue(json, new TypeReference<Map<String, Object>>() { });
    }
}
