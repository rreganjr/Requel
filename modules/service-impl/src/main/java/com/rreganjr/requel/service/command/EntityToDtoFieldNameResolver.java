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
package com.rreganjr.requel.service.command;

import com.rreganjr.requel.service.api.FromEntityProperty;
import org.springframework.stereotype.Component;

import java.lang.reflect.RecordComponent;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Translates a JPA entity property name to the input-DTO field name for a command, from
 * {@link FromEntityProperty} annotations on the command's input-DTO record components (issue #176).
 *
 * <p>Entity-level bean validation runs at flush and reports entity property names; Angular forms are
 * built from input DTOs. {@code CommandController} calls this to convert the former to the latter
 * before building {@code FieldViolation.field}, so a violation routes to the right control without a
 * per-editor map. A name with no annotation (the common case, where DTO and entity agree) passes
 * through unchanged. Results are cached per input-DTO class.
 */
@Component
public class EntityToDtoFieldNameResolver {

    private final Map<Class<?>, Map<String, String>> cache = new ConcurrentHashMap<>();

    /**
     * @param inputClass the command's input-DTO class, or {@code null} if unknown
     * @param entityPropertyName the entity property name from the violation
     * @return the input-DTO field name if a {@link FromEntityProperty} maps it, else the input unchanged
     */
    public String toDtoField(Class<?> inputClass, String entityPropertyName) {
        if (inputClass == null || entityPropertyName == null) {
            return entityPropertyName;
        }
        return cache.computeIfAbsent(inputClass, EntityToDtoFieldNameResolver::build)
                .getOrDefault(entityPropertyName, entityPropertyName);
    }

    private static Map<String, String> build(Class<?> inputClass) {
        Map<String, String> mapping = new HashMap<>();
        if (inputClass.isRecord()) {
            for (RecordComponent component : inputClass.getRecordComponents()) {
                FromEntityProperty annotation = component.getAnnotation(FromEntityProperty.class);
                if (annotation != null) {
                    mapping.put(annotation.value(), component.getName());
                }
            }
        }
        return mapping;
    }
}
