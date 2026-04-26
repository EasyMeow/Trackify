-- Authentication identities. One row per local-account user.
-- Login and email are unique handles; password_hash stores the bcrypt digest only.

CREATE TABLE users (
    id              UUID        PRIMARY KEY DEFAULT gen_random_uuid(),
    login           TEXT        NOT NULL,
    email           TEXT        NOT NULL,
    password_hash   TEXT        NOT NULL,
    display_name    TEXT        NOT NULL,
    created_at      TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at      TIMESTAMPTZ NOT NULL DEFAULT now(),
    CONSTRAINT users_login_unique UNIQUE (login),
    CONSTRAINT users_email_unique UNIQUE (email)
);
