package com.tuensso.audit;

import org.springframework.stereotype.Service;

@Service
public class AuditService {

    private final AuditLogRepository repo;

    public AuditService(AuditLogRepository repo) {
        this.repo = repo;
    }

    public void log(String eventType, String username, String targetType, String targetId, String detail, String ip) {
        AuditLog entry = new AuditLog();
        entry.setEventType(eventType);
        entry.setUsername(username);
        entry.setTargetType(targetType);
        entry.setTargetId(targetId);
        entry.setDetail(detail != null && detail.length() > 500 ? detail.substring(0, 500) : detail);
        entry.setIpAddress(ip);
        repo.save(entry);
    }
}
