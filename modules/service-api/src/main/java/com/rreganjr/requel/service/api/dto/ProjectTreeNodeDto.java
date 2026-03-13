package com.rreganjr.requel.service.api.dto;

import com.fasterxml.jackson.annotation.JsonInclude;

import java.util.List;

/**
 * A node in the project content tree for sidebar navigation.
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public record ProjectTreeNodeDto(
        Long id,
        String type,
        String name,
        List<ProjectTreeNodeDto> children
) {
    /** Leaf node constructor — entity with id, no children. */
    public ProjectTreeNodeDto(Long id, String type, String name) {
        this(id, type, name, null);
    }

    /** Group node constructor — no id, has children. */
    public ProjectTreeNodeDto(String type, String name, List<ProjectTreeNodeDto> children) {
        this(null, type, name, children);
    }
}
