-- Baseline migration. Enables extensions needed by later domain migrations.
-- Subsequent migrations (V2__auth_and_user.sql onward) own table DDL.

CREATE EXTENSION IF NOT EXISTS pgcrypto;
