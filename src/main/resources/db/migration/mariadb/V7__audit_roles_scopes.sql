CREATE TABLE IF NOT EXISTS audit_log (
    id BINARY(16) NOT NULL PRIMARY KEY,
    event_type VARCHAR(50) NOT NULL,
    username VARCHAR(150),
    target_type VARCHAR(50),
    target_id VARCHAR(255),
    detail VARCHAR(500),
    ip_address VARCHAR(45),
    created_at DATETIME(6) NOT NULL,
    INDEX idx_audit_log_created (created_at DESC),
    INDEX idx_audit_log_username (username),
    INDEX idx_audit_log_event (event_type)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS roles (
    id BINARY(16) NOT NULL PRIMARY KEY,
    name VARCHAR(120) NOT NULL UNIQUE,
    description VARCHAR(255),
    created_at DATETIME(6) NOT NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS user_role_assignment (
    user_id BINARY(16) NOT NULL,
    role_id BINARY(16) NOT NULL,
    PRIMARY KEY (user_id, role_id),
    CONSTRAINT fk_role_user FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE,
    CONSTRAINT fk_role_role FOREIGN KEY (role_id) REFERENCES roles(id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS client_scopes (
    id BINARY(16) NOT NULL PRIMARY KEY,
    name VARCHAR(120) NOT NULL UNIQUE,
    description VARCHAR(255),
    claim_name VARCHAR(120),
    claim_value VARCHAR(500),
    created_at DATETIME(6) NOT NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
