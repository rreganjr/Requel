package com.rreganjr.requel.service.api.dto;

import com.fasterxml.jackson.annotation.JsonInclude;

/**
 * Read DTO for a report generator (document template).
 * List view omits text (XSLT) for efficiency; detail view includes it.
 *
 * @param id        report generator id
 * @param version   optimistic lock version
 * @param name      template name (unique within a project)
 * @param text      XSLT stylesheet content (detail view only, null in list view)
 * @param createdBy display name of creator
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public record ReportGeneratorDto(Long id, int version, String name, String text, String createdBy) {
}
