-- Assistant runs, findings, and usage rows backing the Phase 2 dispatcher.
-- See doc/assistant-spi-plan.md (Data Model and Applicator Contract) for the
-- field-level rationale. Soft FKs (no constraint) are used for cross-module
-- references (users, pods, annotations) so the assistant tables can be added
-- without coupling their schema to user-jpa / project-jpa / annotation-jpa
-- migration ordering.

CREATE TABLE assistant_runs (
    id VARCHAR(36) NOT NULL PRIMARY KEY,
    assistant_id VARCHAR(200) NOT NULL,
    assistant_user_id BIGINT NULL,
    triggered_by_user_id BIGINT NULL,
    project_id BIGINT NULL,
    target_type VARCHAR(80) NULL,
    target_id BIGINT NULL,
    task_type VARCHAR(80) NULL,
    provider VARCHAR(80) NULL,
    model VARCHAR(120) NULL,
    template_id VARCHAR(120) NULL,
    template_version VARCHAR(40) NULL,
    template_source VARCHAR(80) NULL,
    status VARCHAR(20) NOT NULL,
    started_at TIMESTAMP NULL,
    completed_at TIMESTAMP NULL,
    latency_ms BIGINT NULL,
    error_kind VARCHAR(80) NULL,
    error_summary VARCHAR(1000) NULL,
    findings_count INT NOT NULL DEFAULT 0,
    body_capture_reason VARCHAR(40) NULL,
    body_retained_until TIMESTAMP NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    INDEX idx_assistant_runs_project (project_id),
    INDEX idx_assistant_runs_status (status),
    INDEX idx_assistant_runs_assistant (assistant_id),
    INDEX idx_assistant_runs_target (target_type, target_id)
);

CREATE TABLE assistant_findings (
    id VARCHAR(36) NOT NULL PRIMARY KEY,
    idempotency_key VARCHAR(255) NOT NULL,
    assistant_id VARCHAR(200) NOT NULL,
    project_id BIGINT NULL,
    target_type VARCHAR(80) NOT NULL,
    target_id BIGINT NOT NULL,
    finding_type VARCHAR(120) NOT NULL,
    severity VARCHAR(20) NULL,
    confidence DECIMAL(4,3) NULL,
    summary VARCHAR(500) NULL,
    evidence_json TEXT NULL,
    applied_annotation_id BIGINT NULL,
    state VARCHAR(20) NOT NULL,
    created_run_id VARCHAR(36) NOT NULL,
    last_seen_run_id VARCHAR(36) NOT NULL,
    superseded_by_run_id VARCHAR(36) NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    last_seen_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    closed_at TIMESTAMP NULL,
    CONSTRAINT uq_assistant_findings_key UNIQUE (idempotency_key),
    INDEX idx_assistant_findings_target (target_type, target_id),
    INDEX idx_assistant_findings_state (state),
    INDEX idx_assistant_findings_project (project_id),
    INDEX idx_assistant_findings_applied (applied_annotation_id)
);

-- AssistantUsage stores provider-call telemetry. request_body and response_body
-- are intentionally LONGTEXT (encrypted ciphertext, base64-encoded by the
-- application). They are nullable and populated only when AssistantRun.
-- body_capture_reason is non-null (failure / project_capture_window /
-- user_opt_in / sampled) and the TTL has not elapsed.
CREATE TABLE assistant_usages (
    id VARCHAR(36) NOT NULL PRIMARY KEY,
    run_id VARCHAR(36) NOT NULL,
    provider VARCHAR(80) NULL,
    model VARCHAR(120) NULL,
    input_tokens INT NULL,
    output_tokens INT NULL,
    cached_input_tokens INT NULL,
    cost_estimate DECIMAL(12,6) NULL,
    latency_ms BIGINT NULL,
    request_body LONGTEXT NULL,
    response_body LONGTEXT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    INDEX idx_assistant_usages_run (run_id),
    CONSTRAINT FK_assistant_usages_run FOREIGN KEY (run_id) REFERENCES assistant_runs (id)
);

-- Annotation-level attribution. assistant_idempotency_key duplicates the value
-- on the matching assistant_findings row for fast reverse lookup; the source
-- column carries "HUMAN" for non-assistant writes or "ASSISTANT:<assistant_id>"
-- so source labeling does not require a join against assistant_findings.
ALTER TABLE annotations
    ADD COLUMN assistant_idempotency_key VARCHAR(255) NULL,
    ADD COLUMN source VARCHAR(100) NULL,
    ADD INDEX idx_annotations_assistant_idempotency_key (assistant_idempotency_key);
