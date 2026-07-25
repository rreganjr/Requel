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
package com.rreganjr.requel.gateway;

import java.lang.reflect.RecordComponent;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Derives a JSON schema for a gateway command's input DTO, so every front-end generates its command
 * surface from the same rules: MCP typed-tool schemas, the CLI's typed subcommands, and the
 * descriptor endpoint ({@code /api/gateway/commands/descriptors}) all call this one generator and
 * therefore cannot drift from one another (issues #103, #104).
 *
 * <p>Input DTOs are Java records, so each record component becomes a typed JSON property. A
 * component is marked <em>required</em> when it (or its generated accessor) carries a
 * {@code jakarta.validation} {@code @NotNull}/{@code @NotBlank} annotation — those annotations
 * encode the fields the command's applicator dereferences unconditionally (issue #104). Annotations
 * are matched by fully-qualified name so this module needs no compile-time dependency on the
 * validation API. Unknown fields are rejected ({@code additionalProperties:false}) so typos surface
 * early. A {@code null}/{@link Void} or non-record input type yields an empty object schema.
 *
 * <p>Pure JDK (reflection + collections); no Jackson or Spring, so it is safe to share from
 * {@code gateway-api}.
 */
public final class CommandInputSchema {

    private CommandInputSchema() {
    }

    /**
     * Build the JSON schema (as a plain {@code Map}) for the given command input DTO type.
     *
     * @param inputType the command's input DTO record class, or {@code null}/{@link Void} for none
     * @return an object-schema map: {@code {type, properties, required, additionalProperties:false}}
     */
    public static Map<String, Object> of(Class<?> inputType) {
        if (inputType == null || inputType == Void.class || !inputType.isRecord()) {
            return objectSchema(Map.of(), List.of());
        }
        Map<String, Object> properties = new LinkedHashMap<>();
        List<String> required = new ArrayList<>();
        for (RecordComponent component : inputType.getRecordComponents()) {
            properties.put(component.getName(), jsonType(component.getType()));
            if (isRequired(component)) {
                required.add(component.getName());
            }
        }
        return objectSchema(properties, required);
    }

    /** The input DTO's field names in declaration order (empty for {@code null}/{@link Void}/non-record). */
    public static List<String> fieldNames(Class<?> inputType) {
        if (inputType == null || inputType == Void.class || !inputType.isRecord()) {
            return List.of();
        }
        List<String> names = new ArrayList<>();
        for (RecordComponent component : inputType.getRecordComponents()) {
            names.add(component.getName());
        }
        return names;
    }

    /** Assemble an object schema node with the given properties and required-field list. */
    public static Map<String, Object> objectSchema(Map<String, Object> properties,
            List<String> required) {
        return Map.of("type", "object", "properties", properties, "required", required,
                "additionalProperties", false);
    }

    /** A {@code {"type":"string"}} schema node. */
    public static Map<String, Object> stringType() {
        return Map.of("type", "string");
    }

    /** A {@code {"type":"integer"}} schema node. */
    public static Map<String, Object> integerType() {
        return Map.of("type", "integer");
    }

    /** A {@code {"type":"boolean"}} schema node. */
    public static Map<String, Object> booleanType() {
        return Map.of("type", "boolean");
    }

    /**
     * A record component is required when it (or its generated accessor) carries a
     * {@code jakarta.validation} {@code @NotNull} or {@code @NotBlank} annotation. Matched by fully
     * qualified name so this module needs no compile-time dependency on the validation API.
     */
    private static boolean isRequired(RecordComponent component) {
        return hasRequiredAnnotation(component.getAnnotations())
                || hasRequiredAnnotation(component.getAccessor().getAnnotations());
    }

    private static boolean hasRequiredAnnotation(java.lang.annotation.Annotation[] annotations) {
        for (java.lang.annotation.Annotation a : annotations) {
            String name = a.annotationType().getName();
            if (name.equals("jakarta.validation.constraints.NotNull")
                    || name.equals("jakarta.validation.constraints.NotBlank")) {
                return true;
            }
        }
        return false;
    }

    /** Map a Java type to a JSON-schema type node. */
    private static Map<String, Object> jsonType(Class<?> type) {
        if (type == String.class || type == Character.class || type == char.class) {
            return stringType();
        }
        if (type == Boolean.class || type == boolean.class) {
            return booleanType();
        }
        if (type == Integer.class || type == int.class || type == Long.class || type == long.class
                || type == Short.class || type == short.class || type == Byte.class
                || type == byte.class) {
            return integerType();
        }
        if (type == Double.class || type == double.class || type == Float.class
                || type == float.class) {
            return Map.of("type", "number");
        }
        if (Iterable.class.isAssignableFrom(type) || type.isArray()) {
            return Map.of("type", "array");
        }
        // Enums serialize as their name; everything else is a nested object.
        return type.isEnum() ? stringType() : Map.of("type", "object");
    }
}
