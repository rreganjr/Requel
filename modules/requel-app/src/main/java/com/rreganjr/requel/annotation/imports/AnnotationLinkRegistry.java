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
