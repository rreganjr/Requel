package com.rreganjr.requel.service.api.dto;

import java.util.List;

/**
 * Query response wrapping all annotations for a single annotatable entity.
 */
public record AnnotationsDto(
        List<NoteDto> notes,
        List<IssueDto> issues
) {
}
