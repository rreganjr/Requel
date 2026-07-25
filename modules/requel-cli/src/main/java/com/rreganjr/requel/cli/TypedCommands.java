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
import com.rreganjr.requel.gateway.CommandGateway;
import com.rreganjr.requel.gateway.GatewayException;
import com.rreganjr.requel.gateway.GatewayRequest;
import com.rreganjr.requel.gateway.GatewayResult;
import com.rreganjr.requel.gateway.rest.BearerTokenSource;
import com.rreganjr.requel.gateway.rest.CommandInfo;
import com.rreganjr.requel.gateway.rest.RestCommandGateway;
import com.rreganjr.requel.gateway.rest.RestGatewayCatalog;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.Callable;
import java.util.function.Function;
import picocli.CommandLine;
import picocli.CommandLine.Model.CommandSpec;
import picocli.CommandLine.Model.IGetter;
import picocli.CommandLine.Model.ISetter;
import picocli.CommandLine.Model.OptionSpec;
import picocli.CommandLine.ParseResult;

/**
 * Registers a typed picocli subcommand per gateway <em>write</em> command, generated at runtime from
 * the server's descriptor catalog ({@code /api/gateway/commands/descriptors}) so the CLI exposes
 * exactly what the connected server permits and stays in lockstep with MCP and the gateway policy
 * (issue #103). Each command's input JSON schema — the same schema MCP typed tools use — becomes a
 * set of per-field {@code --flags}: scalar fields map to typed options (string/integer/number/
 * boolean), and nested object/array fields take a raw-JSON string. Required fields (from the
 * schema's {@code required} array) are required flags.
 *
 * <p>Discovery is best-effort and runtime-primary: the generic {@code requel run <Type> --input}
 * stays the always-available fallback. When the invocation is a built-in subcommand, {@code --help}/
 * {@code --version}, or the server is unreachable, no typed subcommands are registered and only the
 * built-ins show — the offline story from the #70 plan.
 */
final class TypedCommands {

    /** Default dispatch: a REST gateway built from the parent's URL + token source. */
    static final Function<RequelCli, CommandGateway> REST_GATEWAY =
            parent -> new RestCommandGateway(parent.url, parent.tokenSource());

    private static final ObjectMapper MAPPER = new ObjectMapper();

    private TypedCommands() {
    }

    /**
     * Best-effort: if this invocation looks like it wants a typed subcommand and a server is
     * reachable, fetch the catalog and register the typed subcommands. Any failure (offline, auth,
     * writes disabled → empty catalog) leaves only the built-ins, by design.
     */
    static void registerFromServer(CommandLine root, RequelCli parent, String[] args) {
        ProbedInvocation probe = probe(args);
        if (!probe.wantsTypedSubcommand()) {
            return;
        }
        List<CommandInfo> descriptors;
        try {
            descriptors = new RestGatewayCatalog(probe.url(), probe.tokenSource()).descriptors();
        } catch (RuntimeException e) {
            // Offline / unauthorized / other transport failure: built-ins only.
            return;
        }
        register(root, parent, descriptors, REST_GATEWAY);
    }

    /**
     * Add a typed subcommand for each write descriptor. Pure and side-effect-free apart from
     * mutating {@code root}'s subcommand set — the seam the tests drive with a stub gateway.
     */
    static void register(CommandLine root, RequelCli parent, List<CommandInfo> descriptors,
            Function<RequelCli, CommandGateway> gatewayFactory) {
        for (CommandInfo d : descriptors) {
            if (!d.write()) {
                continue; // the catalog is write-only, but never generate a mutating verb for a read
            }
            String name = kebab(d.commandType());
            if (root.getSubcommands().containsKey(name)) {
                continue; // never shadow a hand-written built-in (e.g. upsert-goal)
            }
            root.addSubcommand(name, buildSubcommand(d, parent, gatewayFactory));
        }
    }

    private static CommandLine buildSubcommand(CommandInfo d, RequelCli parent,
            Function<RequelCli, CommandGateway> gatewayFactory) {
        Map<String, Object> values = new LinkedHashMap<>();
        Set<String> rawJsonFields = new HashSet<>();

        // The command's behavior is a Callable over the captured value map; wrapWithoutInspection
        // makes it the spec's user object without scanning it for annotations, so picocli's default
        // execution strategy invokes it when this subcommand is matched.
        Callable<Integer> action = () ->
                dispatch(parent, gatewayFactory, d.commandType(), values, rawJsonFields);
        CommandSpec spec = CommandSpec.wrapWithoutInspection(action);
        spec.name(kebab(d.commandType()));
        spec.mixinStandardHelpOptions(true);
        spec.usageMessage().description(describe(d));

        Map<String, Object> properties = asMap(schema(d).get("properties"));
        Set<String> required = Set.copyOf(asStringList(schema(d).get("required")));
        for (Map.Entry<String, Object> property : properties.entrySet()) {
            String field = property.getKey();
            String jsonType = typeOf(property.getValue());
            boolean rawJson = jsonType.equals("array") || jsonType.equals("object");
            if (rawJson) {
                rawJsonFields.add(field);
            }
            MapBinding binding = new MapBinding(values, field);
            spec.addOption(OptionSpec.builder("--" + kebab(field))
                    .type(rawJson ? String.class : javaType(jsonType))
                    .required(required.contains(field))
                    .paramLabel(field.toUpperCase(Locale.ROOT))
                    .description(flagDescription(field, jsonType, rawJson, required.contains(field)))
                    .getter(binding)
                    .setter(binding)
                    .build());
        }

        return new CommandLine(spec);
    }

    private static int dispatch(RequelCli parent, Function<RequelCli, CommandGateway> gatewayFactory,
            String commandType, Map<String, Object> values, Set<String> rawJsonFields) {
        Map<String, Object> input = new LinkedHashMap<>();
        for (Map.Entry<String, Object> e : values.entrySet()) {
            Object value = e.getValue();
            if (rawJsonFields.contains(e.getKey()) && value instanceof String raw) {
                try {
                    value = MAPPER.readValue(raw, Object.class);
                } catch (JsonProcessingException ex) {
                    System.err.println("Invalid JSON for --" + kebab(e.getKey()) + ": "
                            + ex.getOriginalMessage());
                    return ExitCode.USAGE;
                }
            }
            input.put(e.getKey(), value);
        }
        CommandGateway gateway = gatewayFactory.apply(parent);
        try {
            GatewayResult result = gateway.execute(new GatewayRequest(commandType, input));
            parent.printResult(result);
            return ExitCode.SUCCESS;
        } catch (GatewayException e) {
            parent.printError(e);
            return ExitCode.forKind(e.getKind());
        }
    }

    // ---- invocation probing (decide whether to fetch, and resolve the connection) ---------------

    /**
     * Parse the args against a throwaway root (unmatched allowed, nothing executed) to decide whether
     * this invocation wants a typed subcommand, and to resolve the URL/token the same way the real
     * run will (picocli applies {@code --url}/{@code --token} flags and their env defaults).
     */
    /**
     * Whether this invocation looks like it wants a runtime typed subcommand (i.e. not a built-in,
     * {@code --help}/{@code --version}, or empty). Package-visible for tests of the fetch gating.
     */
    static boolean wantsTypedSubcommand(String[] args) {
        return probe(args).wantsTypedSubcommand();
    }

    private static ProbedInvocation probe(String[] args) {
        RequelCli probeRoot = new RequelCli();
        CommandLine probe = new CommandLine(probeRoot)
                .setUnmatchedArgumentsAllowed(true)
                .setExpandAtFiles(false)
                .setCaseInsensitiveEnumValuesAllowed(true);
        boolean wants;
        try {
            ParseResult pr = probe.parseArgs(args);
            // A recognized built-in subcommand → let it run offline; no fetch needed.
            // Otherwise a non-option unmatched token is a candidate typed subcommand → fetch.
            wants = !pr.hasSubcommand()
                    && pr.unmatched().stream().anyMatch(t -> !t.startsWith("-"));
        } catch (RuntimeException e) {
            wants = false; // malformed → let the real parse report it; built-ins only
        }
        String url = probeRoot.url != null && !probeRoot.url.isBlank()
                ? probeRoot.url : "http://localhost:8080";
        return new ProbedInvocation(wants, url, probeRoot.tokenSource());
    }

    private record ProbedInvocation(boolean wantsTypedSubcommand, String url,
            BearerTokenSource tokenSource) {
    }

    // ---- schema → picocli mapping helpers -------------------------------------------------------

    /** picocli getter+setter backed by an entry in the per-subcommand values map. */
    private static final class MapBinding implements IGetter, ISetter {
        private final Map<String, Object> values;
        private final String key;

        MapBinding(Map<String, Object> values, String key) {
            this.values = values;
            this.key = key;
        }

        @Override
        @SuppressWarnings("unchecked")
        public <T> T get() {
            return (T) values.get(key);
        }

        @Override
        @SuppressWarnings("unchecked")
        public <T> T set(T value) {
            return (T) values.put(key, value);
        }
    }

    private static Class<?> javaType(String jsonType) {
        return switch (jsonType) {
            case "integer" -> Long.class;   // headroom for ids; Jackson narrows to the DTO field
            case "number" -> Double.class;
            case "boolean" -> Boolean.class;
            default -> String.class;        // string, and anything unrecognized
        };
    }

    private static String flagDescription(String field, String jsonType, boolean rawJson,
            boolean required) {
        String prefix = required ? "(required) " : "";
        String typeHint = rawJson ? " (" + jsonType + " as raw JSON)" : "";
        return prefix + field + typeHint;
    }

    private static String[] describe(CommandInfo d) {
        List<String> lines = new ArrayList<>();
        lines.add(d.title() != null ? d.title() : d.commandType());
        if (d.description() != null && !d.description().isBlank()) {
            lines.add(d.description());
        }
        return lines.toArray(new String[0]);
    }

    private static Map<String, Object> schema(CommandInfo d) {
        return d.schema() != null ? d.schema() : Map.of();
    }

    @SuppressWarnings("unchecked")
    private static Map<String, Object> asMap(Object node) {
        return node instanceof Map ? (Map<String, Object>) node : Map.of();
    }

    private static List<String> asStringList(Object node) {
        if (!(node instanceof List<?> list)) {
            return List.of();
        }
        List<String> out = new ArrayList<>();
        for (Object o : list) {
            out.add(String.valueOf(o));
        }
        return out;
    }

    /** The {@code "type"} of a JSON-schema property node, defaulting to {@code "string"}. */
    private static String typeOf(Object propertyNode) {
        Object type = asMap(propertyNode).get("type");
        return type instanceof String s ? s : "string";
    }

    /** {@code EditGoal} → {@code edit-goal}, {@code projectName} → {@code project-name}. */
    static String kebab(String name) {
        StringBuilder sb = new StringBuilder(name.length() + 4);
        for (int i = 0; i < name.length(); i++) {
            char c = name.charAt(i);
            if (Character.isUpperCase(c)) {
                if (i > 0) {
                    sb.append('-');
                }
                sb.append(Character.toLowerCase(c));
            } else {
                sb.append(c);
            }
        }
        return sb.toString();
    }
}
