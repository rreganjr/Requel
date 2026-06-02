-- Audit log for MCP (Model Context Protocol) JSON-RPC calls. One row per call.
-- Records the triggering (human) user resolved from the security context, the
-- method/tool invoked, and the outcome. assistant_user_id and run_id are reserved
-- for the internal AI runtime's dual-identity session token (Phase 5+) and are
-- nullable until that path mints them.
CREATE TABLE mcp_calls (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    triggering_user_id BIGINT NULL,
    assistant_user_id BIGINT NULL,
    run_id VARCHAR(36) NULL,
    method VARCHAR(120) NOT NULL,
    tool_name VARCHAR(200) NULL,
    status VARCHAR(20) NOT NULL,
    error_code INT NULL,
    error_summary VARCHAR(1000) NULL,
    duration_ms BIGINT NULL,
    called_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    INDEX idx_mcp_user_time (triggering_user_id, called_at),
    INDEX idx_mcp_run (run_id)
);
