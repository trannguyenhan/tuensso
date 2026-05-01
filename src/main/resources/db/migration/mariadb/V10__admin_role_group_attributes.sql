-- 1. Seed ADMIN role (system role, cannot be deleted)
INSERT INTO roles (id, name, description, created_at)
SELECT UNHEX(REPLACE(UUID(), '-', '')), 'ADMIN', 'System administrator (built-in)', NOW(6)
FROM DUAL
WHERE NOT EXISTS (SELECT 1 FROM roles WHERE name = 'ADMIN');

-- 2. Migrate users from 'admins' group to ADMIN role
INSERT IGNORE INTO user_role_assignment (user_id, role_id)
SELECT m.user_id, r.id
FROM user_group_membership m
JOIN user_groups g ON g.id = m.group_id AND g.name = 'admins'
JOIN roles r ON r.name = 'ADMIN';

-- 3. Create group_attributes table (custom attributes at group level)
CREATE TABLE IF NOT EXISTS group_attributes (
    id BINARY(16) NOT NULL PRIMARY KEY,
    group_id BINARY(16) NOT NULL,
    attr_key VARCHAR(120) NOT NULL,
    attr_value VARCHAR(1000),
    CONSTRAINT fk_gattr_group FOREIGN KEY (group_id) REFERENCES user_groups(id) ON DELETE CASCADE,
    CONSTRAINT uq_group_attr UNIQUE (group_id, attr_key)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
