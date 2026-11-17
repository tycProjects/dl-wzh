-- DroidStore: базовая схема PostgreSQL (пример, не полная)
CREATE TABLE users (
    id          BIGSERIAL PRIMARY KEY,
    email       TEXT NOT NULL UNIQUE,
    password_hash TEXT NOT NULL,
    role        TEXT NOT NULL DEFAULT 'user'
                CHECK (role IN ('user','developer','moderator','admin','owner')),
    is_blocked  BOOLEAN NOT NULL DEFAULT FALSE,
    created_at  TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE TABLE developers (
    id            BIGSERIAL PRIMARY KEY,
    user_id       BIGINT NOT NULL UNIQUE REFERENCES users(id),
    studio_name   TEXT NOT NULL,
    status        TEXT NOT NULL DEFAULT 'newcomer'
                  CHECK (status IN ('newcomer','verified_developer','verified_studio','blocked'))
);

CREATE TABLE apps (
    id            BIGSERIAL PRIMARY KEY,
    developer_id  BIGINT NOT NULL REFERENCES developers(id),
    package_name  TEXT NOT NULL UNIQUE,
    title         TEXT NOT NULL,
    status        TEXT NOT NULL DEFAULT 'draft'
                  CHECK (status IN ('draft','pending_review','security_scan','changes_requested',
                                    'approved','published','rejected','suspended'))
);

CREATE TABLE app_versions (
    id            BIGSERIAL PRIMARY KEY,
    app_id        BIGINT NOT NULL REFERENCES apps(id),
    version_code  INTEGER NOT NULL,
    version_name  TEXT NOT NULL,
    apk_path      TEXT NOT NULL,
    sha256        CHAR(64) NOT NULL,
    signer_sha256 CHAR(64),
    security_score SMALLINT CHECK (security_score BETWEEN 0 AND 100),
    created_at    TIMESTAMPTZ NOT NULL DEFAULT now(),
    UNIQUE (app_id, version_code)
);

CREATE TABLE audit_log (
    id          BIGSERIAL PRIMARY KEY,
    actor_id    BIGINT REFERENCES users(id),
    action      TEXT NOT NULL,
    target      TEXT,
    details     JSONB,
    created_at  TIMESTAMPTZ NOT NULL DEFAULT now()
);
