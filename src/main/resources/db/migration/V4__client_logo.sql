ALTER TABLE oauth2_registered_client
    ADD COLUMN IF NOT EXISTS logo_uri VARCHAR(500);