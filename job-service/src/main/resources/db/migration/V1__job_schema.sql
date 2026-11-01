-- Raw posting text is stored untouched; the parsed requirements are a separate, re-derivable record.
CREATE TABLE job (
    id                uuid          PRIMARY KEY,
    user_id           uuid          NOT NULL,
    title             varchar(160)  NOT NULL,
    company           varchar(160)  NOT NULL DEFAULT '',
    location          varchar(160)  NOT NULL DEFAULT '',
    url               varchar(500)  NOT NULL DEFAULT '',
    raw               text          NOT NULL,
    requirements_json text          NOT NULL,
    created_at        timestamptz   NOT NULL DEFAULT now()
);
CREATE INDEX ix_job_user ON job (user_id, created_at DESC);
