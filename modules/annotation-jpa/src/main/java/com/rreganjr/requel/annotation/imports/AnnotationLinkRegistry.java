/*
 * This file is part of Requel - the Collaborative Requirements
 * Elicitation System.
 *
 * Copyright 2025 Ron Regan Jr. All Rights Reserved.
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
package com.rreganjr.requel.annotation.imports;

import com.rreganjr.requel.annotation.Annotatable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Tracks which annotations should attach to which annotatable entities during import.
 */
public class AnnotationLinkRegistry {

    private final Map<String, List<Annotatable>> links = new ConcurrentHashMap<>();

    public void recordLink(String annotationId, Annotatable annotatable) {
        if (annotationId == null || annotatable == null) {
            return;
        }
        links.computeIfAbsent(annotationId, id -> new ArrayList<>()).add(annotatable);
    }

    public List<Annotatable> consumeLinks(String annotationId) {
        if (annotationId == null) {
            return List.of();
        }
        return links.containsKey(annotationId)
                ? new ArrayList<>(links.remove(annotationId))
                : List.of();
    }
}
