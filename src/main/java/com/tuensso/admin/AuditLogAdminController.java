package com.tuensso.admin;

import com.tuensso.audit.AuditLog;
import com.tuensso.audit.AuditLogRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/admin/audit")
public class AuditLogAdminController {

    private final AuditLogRepository repo;

    public AuditLogAdminController(AuditLogRepository repo) {
        this.repo = repo;
    }

    @GetMapping
    public Page<AuditLog> list(@RequestParam(defaultValue = "0") int page,
                               @RequestParam(defaultValue = "50") int size) {
        return repo.findAllByOrderByCreatedAtDesc(PageRequest.of(page, Math.min(size, 100)));
    }
}
