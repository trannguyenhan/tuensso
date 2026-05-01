ALTER TABLE roles ADD COLUMN IF NOT EXISTS permissions VARCHAR(500);

-- ADMIN gets all permissions
UPDATE roles SET permissions = 'dashboard,apps,users,groups,roles,sessions,audit,integration'
WHERE name = 'ADMIN';
