ALTER TABLE users
    ADD COLUMN IF NOT EXISTS avatar              BYTEA,
    ADD COLUMN IF NOT EXISTS avatar_content_type TEXT;
