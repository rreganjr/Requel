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
import com.rreganjr.requel.gateway.GatewayRequest;
import com.rreganjr.requel.gateway.GatewayResult;
import com.rreganjr.requel.gateway.rest.CommandInfo;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Function;
import org.junit.jupiter.api.Test;
import picocli.CommandLine;
import picocli.CommandLine.Model.CommandSpec;

/**
 * Unit tests for {@link TypedCommands}: generating typed picocli subcommands from a command
 * descriptor's input schema, dispatching the assembled input through the gateway, required-flag
 * enforcement, raw-JSON handling for nested fields, built-in collision avoidance, and the
 * fetch-gating that keeps built-in/offline invocations from hitting the network. No network: the
 * gateway is stubbed via the injectable factory.
 */
class TypedCommandsTest {

    private static Map<String, Object> schema(Map<String, Object> properties, List<String> required) {
        return Map.of("type", "object", "properties", properties, "required", required,
                "additionalProperties", false);
    }

    /** EditGoal with a required projectName (string) and optional name (string). */
    private static CommandInfo editGoal() {
        Map<String, Object> props = new LinkedHashMap<>();
        props.put("projectName", Map.of("type", "string"));
        props.put("name", Map.of("type", "string"));
        return new CommandInfo("EditGoal", "EditGoalInput", "Edit Goal", null, true, "Goal[Edit]",
                schema(props, List.of("projectName")));
    }

    private record Harness(CommandLine root, RequelCli parent,
            AtomicReference<GatewayRequest> lastRequest) {
    }

    private static Harness harnessFor(CommandInfo... descriptors) {
        RequelCli parent = new RequelCli();
        parent.url = "http://localhost:8080";
        CommandLine root = new CommandLine(parent);
        AtomicReference<GatewayRequest> lastRequest = new AtomicReference<>();
        CommandGateway stub = request -> {
            lastRequest.set(request);
            return new GatewayResult(request.commandType(), Map.of("id", 1));
        };
        Function<RequelCli, CommandGateway> factory = p -> stub;
        TypedCommands.register(root, parent, List.of(descriptors), factory);
        return new Harness(root, parent, lastRequest);
    }

    @Test
    void generatesAKebabSubcommandWithTypedFlagsAndRequiredness() {
        Harness h = harnessFor(editGoal());

        CommandLine sub = h.root().getSubcommands().get("edit-goal");
        assertThat(sub).isNotNull();
        CommandSpec spec = sub.getCommandSpec();
        assertThat(spec.findOption("--project-name")).isNotNull();
        assertThat(spec.findOption("--project-name").required()).isTrue();
        assertThat(spec.findOption("--name")).isNotNull();
        assertThat(spec.findOption("--name").required()).isFalse();
    }

    @Test
    @SuppressWarnings("unchecked")
    void invokingATypedSubcommandDispatchesTheAssembledInput() {
        Harness h = harnessFor(editGoal());

        int code = h.root().execute("edit-goal", "--project-name", "Demo", "--name", "G");

        assertThat(code).isEqualTo(ExitCode.SUCCESS);
        GatewayRequest request = h.lastRequest().get();
        assertThat(request).isNotNull();
        assertThat(request.commandType()).isEqualTo("EditGoal");
        // Flags map back to the ORIGINAL DTO field names, not the kebab flag names.
        assertThat((Map<String, Object>) request.input())
                .containsEntry("projectName", "Demo")
                .containsEntry("name", "G");
    }

    @Test
    void omittingARequiredFlagIsAUsageErrorAndNeverDispatches() {
        Harness h = harnessFor(editGoal());

        int code = h.root().execute("edit-goal", "--name", "G"); // missing required --project-name

        assertThat(code).isEqualTo(ExitCode.USAGE);
        assertThat(h.lastRequest().get()).isNull();
    }

    @Test
    @SuppressWarnings("unchecked")
    void nestedArrayFieldIsParsedFromRawJson() {
        Map<String, Object> props = new LinkedHashMap<>();
        props.put("name", Map.of("type", "string"));
        props.put("tags", Map.of("type", "array"));
        CommandInfo editThing = new CommandInfo("EditThing", "EditThingInput", "Edit Thing", null,
                true, null, schema(props, List.of("name")));
        Harness h = harnessFor(editThing);

        int code = h.root().execute("edit-thing", "--name", "X", "--tags", "[\"a\",\"b\"]");

        assertThat(code).isEqualTo(ExitCode.SUCCESS);
        Map<String, Object> input = (Map<String, Object>) h.lastRequest().get().input();
        assertThat(input).containsEntry("name", "X");
        assertThat(input.get("tags")).isEqualTo(List.of("a", "b"));
    }

    @Test
    void invalidJsonForANestedFieldIsAUsageError() {
        Map<String, Object> props = new LinkedHashMap<>();
        props.put("tags", Map.of("type", "array"));
        CommandInfo editThing = new CommandInfo("EditThing", "EditThingInput", "Edit Thing", null,
                true, null, schema(props, List.of()));
        Harness h = harnessFor(editThing);

        int code = h.root().execute("edit-thing", "--tags", "not json");

        assertThat(code).isEqualTo(ExitCode.USAGE);
        assertThat(h.lastRequest().get()).isNull();
    }

    @Test
    void doesNotShadowABuiltInSubcommand() {
        // UpsertGoal kebabs to "upsert-goal", which is a hand-written built-in — keep the built-in.
        CommandInfo upsert = new CommandInfo("UpsertGoal", "UpsertGoalInput", "Upsert Goal", null,
                true, null, schema(new LinkedHashMap<>(), List.of()));
        Harness h = harnessFor(upsert);

        Object builtIn = h.root().getSubcommands().get("upsert-goal").getCommand();
        assertThat(builtIn).isInstanceOf(UpsertGoalCommand.class);
    }

    @Test
    void readOnlyDescriptorsAreNeverGenerated() {
        CommandInfo read = new CommandInfo("SomeQuery", "SomeQueryInput", "Some Query", null, false,
                null, schema(new LinkedHashMap<>(), List.of()));
        Harness h = harnessFor(read);

        assertThat(h.root().getSubcommands()).doesNotContainKey("some-query");
    }

    @Test
    void kebabConvertsPascalAndCamelCase() {
        assertThat(TypedCommands.kebab("EditGoal")).isEqualTo("edit-goal");
        assertThat(TypedCommands.kebab("AddGoalToGoalContainer"))
                .isEqualTo("add-goal-to-goal-container");
        assertThat(TypedCommands.kebab("projectName")).isEqualTo("project-name");
    }

    @Test
    void fetchGatingSkipsBuiltInsHelpAndEmptyButFiresForTypedCandidates() {
        // Candidate typed subcommand → fetch.
        assertThat(TypedCommands.wantsTypedSubcommand(new String[] {"edit-goal", "--project", "D"}))
                .isTrue();
        assertThat(TypedCommands.wantsTypedSubcommand(
                new String[] {"--url", "http://x", "edit-goal"})).isTrue();
        // Built-ins / help / version / empty → no fetch (built-ins only).
        assertThat(TypedCommands.wantsTypedSubcommand(new String[] {"run", "EditGoal"})).isFalse();
        assertThat(TypedCommands.wantsTypedSubcommand(new String[] {"projects"})).isFalse();
        assertThat(TypedCommands.wantsTypedSubcommand(new String[] {"--help"})).isFalse();
        assertThat(TypedCommands.wantsTypedSubcommand(new String[] {})).isFalse();
    }
}
