CREATE TABLE app_user (
    id            uuid         PRIMARY KEY,
    email         varchar(320) NOT NULL,
    password_hash varchar(100) NOT NULL,
    full_name     varchar(120) NOT NULL,
    role          varchar(16)  NOT NULL CHECK (role IN ('USER', 'ADMIN')),
    enabled       boolean      NOT NULL DEFAULT true,
    created_at    timestamptz  NOT NULL DEFAULT now()
);

CREATE UNIQUE INDEX uq_app_user_email_lower ON app_user (lower(email));
