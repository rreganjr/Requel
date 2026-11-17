/*
 * This file is part of Requel - the Collaborative Requirements
 * Elicitation System.
 *
 * Copyright 2008, 2009, 2025 Ron Regan Jr. All Rights Reserved.
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
package com.rreganjr.requel.project.impl;

import com.rreganjr.platform.User;
import com.rreganjr.requel.project.*;
import jakarta.persistence.*;

import java.time.Instant;
import java.util.*;

@Entity
@Table(name = "idea", indexes = @Index(name = "idx_idea_project", columnList = "project_id"))
public class IdeaImpl implements Idea {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id", unique = true, nullable = false)
    private Long id;

    @Column(name = "external_id", length = 100)
    private String externalId;

    @Lob
    @Column(name = "source_text", nullable = false)
    private String sourceText;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    private Status status = Status.SUBMITTED;

    @ManyToOne(optional = false)
    @JoinColumn(name = "project_id", nullable = false)
    private ProjectImpl project;

    @ManyToOne(optional = false)
    @JoinColumn(name = "created_by_id", nullable = false)
    private User createdBy;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @OneToMany(mappedBy = "idea", cascade = CascadeType.ALL, orphanRemoval = true)
    private Set<RequirementImpl> derivedRequirements = new HashSet<>();

    @Version
    private Long version;

    protected IdeaImpl() {
        // for hibernate
    }

    public IdeaImpl(String sourceText, ProjectImpl project, User createdBy) {
        this.sourceText = sourceText;
        this.project = project;
        this.createdBy = createdBy;
        this.createdAt = Instant.now();
    }

    @Override
    public Long getId() {
        return id;
    }

    protected void setId(Long id) {
        this.id = id;
    }

    @Override
    public String getExternalId() {
        return externalId;
    }

    public void setExternalId(String externalId) {
        this.externalId = externalId;
    }

    @Override
    public String getSourceText() {
        return sourceText;
    }

    protected void setSourceText(String sourceText) {
        this.sourceText = sourceText;
    }

    @Override
    public Status getStatus() {
        return status;
    }

    public void setStatus(Status status) {
        this.status = status;
    }

    @Override
    public Set<Requirement> getDerivedRequirements() {
        return new HashSet<>(derivedRequirements);
    }

    @Override
    public String getDescription() {
        return sourceText;
    }

    @Override
    public String getName() {
        return "Idea " + (externalId != null ? externalId : id);
    }

    @Override
    public User getCreatedBy() {
        return createdBy;
    }

    protected void setCreatedBy(User createdBy) {
        this.createdBy = createdBy;
    }

    @Override
    public Instant getCreatedAt() {
        return createdAt;
    }

    protected void setCreatedAt(Instant createdAt) {
        this.createdAt = createdAt;
    }

    @Override
    public Long getVersion() {
        return version;
    }

    @Override
    public Project getProject() {
        return project;
    }

    protected void setProject(ProjectImpl project) {
        this.project = project;
    }

    @Override
    public Set<Annotation> getAnnotations() {
        return new HashSet<>();
    }

    @Override
    public int compareTo(ProjectOrDomainEntity o) {
        return Long.compare(this.id, o.getId());
    }

    // JPA helper
    Set<RequirementImpl> getDerivedRequirementsInternal() {
        return derivedRequirements;
    }

    void addDerivedRequirement(RequirementImpl requirement) {
        derivedRequirements.add(requirement);
    }
}
