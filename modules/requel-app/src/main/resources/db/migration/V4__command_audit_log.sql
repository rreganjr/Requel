CREATE TABLE command_audit_log (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id BIGINT NOT NULL,
    executed_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    command_type VARCHAR(100) NOT NULL,
    command_class VARCHAR(255) NOT NULL,
    project_id BIGINT NULL,
    request_payload TEXT,
    INDEX idx_audit_project_time (project_id, executed_at),
    INDEX idx_audit_user_time (user_id, executed_at),
    CONSTRAINT fk_audit_project FOREIGN KEY (project_id) REFERENCES pods (id)
);
