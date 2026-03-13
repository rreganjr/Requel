package com.rreganjr.requel.service.audit;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface CommandAuditLogRepository extends JpaRepository<CommandAuditLog, Long> {
}
