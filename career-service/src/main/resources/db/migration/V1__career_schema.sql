-- One row per user. user_id is the identity-service user id; there is no cross-database
-- foreign key because each service owns its own database.
CREATE TABLE profile (
    user_id          uuid         PRIMARY KEY,
    name             varchar(120) NOT NULL DEFAULT '',
    current_title    varchar(120) NOT NULL DEFAULT '',
    experience_years double precision NOT NULL DEFAULT 0 CHECK (experience_years >= 0 AND experience_years <= 60),
    location         varchar(120) NOT NULL DEFAULT '',
    updated_at       timestamptz  NOT NULL DEFAULT now()
);

CREATE TABLE evidence (
    id         uuid          PRIMARY KEY,
    user_id    uuid          NOT NULL,
    type       varchar(16)   NOT NULL
               CHECK (type IN ('experience','project','certification','education','repository','assessment','interview','claim')),
    title      varchar(160)  NOT NULL,
    org        varchar(160),
    occurred   date,
    months     double precision CHECK (months IS NULL OR (months >= 0 AND months <= 600)),
    score      integer       CHECK (score IS NULL OR (score >= 0 AND score <= 100)),
    note       varchar(1000),
    origin     varchar(16)   NOT NULL CHECK (origin IN ('stated','imported','measured','claimed')),
    created_at timestamptz   NOT NULL DEFAULT now()
);
CREATE INDEX ix_evidence_user ON evidence (user_id, occurred DESC);

CREATE TABLE evidence_skill (
    evidence_id uuid         NOT NULL REFERENCES evidence (id) ON DELETE CASCADE,
    skill_id    varchar(40)  NOT NULL,
    weight      double precision NOT NULL CHECK (weight > 0 AND weight <= 1),
    PRIMARY KEY (evidence_id, skill_id)
);

CREATE TABLE goal (
    id         uuid        PRIMARY KEY,
    user_id    uuid        NOT NULL,
    role_id    varchar(40) NOT NULL,
    seniority  varchar(16) NOT NULL,
    active     boolean     NOT NULL DEFAULT true,
    created_at timestamptz NOT NULL DEFAULT now()
);
CREATE INDEX ix_goal_user ON goal (user_id, created_at DESC);
-- At most one active goal per user, enforced by the database rather than by hope.
CREATE UNIQUE INDEX uq_goal_one_active ON goal (user_id) WHERE active;
