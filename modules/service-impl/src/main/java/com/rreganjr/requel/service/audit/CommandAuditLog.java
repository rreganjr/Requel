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
package com.rreganjr.requel.service.audit;

import jakarta.persistence.*;
import java.time.Instant;

/**
 * Audit record for a successfully executed command. One row per command dispatch.
 * References user by ID (not username) so the association survives username changes.
 * References project by pods.id via JPA PersistenceUnitUtil to extract the ID
 * from the managed entity without requiring a public getId() on the domain interface.
 */
@Entity
@Table(name = "command_audit_log")
public class CommandAuditLog {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Column(name = "executed_at", nullable = false)
    private Instant executedAt;

    @Column(name = "command_type", nullable = false, length = 100)
    private String commandType;

    @Column(name = "command_class", nullable = false, length = 255)
    private String commandClass;

    @Column(name = "project_id")
    private Long projectId;

    @Column(name = "request_payload", columnDefinition = "TEXT")
    private String requestPayload;

    protected CommandAuditLog() {}

    public CommandAuditLog(Long userId, Instant executedAt, String commandType,
                           String commandClass, Long projectId, String requestPayload) {
        this.userId = userId;
        this.executedAt = executedAt;
        this.commandType = commandType;
        this.commandClass = commandClass;
        this.projectId = projectId;
        this.requestPayload = requestPayload;
    }

    public Long getId() { return id; }
    public Long getUserId() { return userId; }
    public Instant getExecutedAt() { return executedAt; }
    public String getCommandType() { return commandType; }
    public String getCommandClass() { return commandClass; }
    public Long getProjectId() { return projectId; }
    public String getRequestPayload() { return requestPayload; }
}
