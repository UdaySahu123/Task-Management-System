-- =============================================================================
-- V1: initial schema for the Task Management System
--
-- Managed by Flyway. Never edit an applied migration; add V2__*.sql instead.
-- Hibernate is configured with ddl-auto=validate, so these tables are the
-- single source of truth for the database structure.
-- =============================================================================

CREATE TABLE users (
    id            BIGINT        NOT NULL AUTO_INCREMENT,
    name          VARCHAR(100)  NOT NULL,
    email         VARCHAR(150)  NOT NULL,
    password_hash VARCHAR(100)  NOT NULL,
    created_at    DATETIME(6)   NOT NULL,
    updated_at    DATETIME(6)   NOT NULL,
    PRIMARY KEY (id),
    CONSTRAINT uk_users_email UNIQUE (email)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4
  COLLATE = utf8mb4_unicode_ci;

CREATE TABLE tasks (
    id          BIGINT         NOT NULL AUTO_INCREMENT,
    title       VARCHAR(200)   NOT NULL,
    description VARCHAR(2000)  NULL,
    status      VARCHAR(20)    NOT NULL,
    priority    VARCHAR(20)    NOT NULL,
    due_date    DATE           NOT NULL,
    created_at  DATETIME(6)    NOT NULL,
    updated_at  DATETIME(6)    NOT NULL,
    user_id     BIGINT         NOT NULL,
    PRIMARY KEY (id),
    CONSTRAINT fk_tasks_user FOREIGN KEY (user_id) REFERENCES users (id) ON DELETE CASCADE
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4
  COLLATE = utf8mb4_unicode_ci;

-- Indexes that support the dashboard and the task list filters,
-- all of which are always scoped by user_id.
CREATE INDEX idx_tasks_user_status     ON tasks (user_id, status);
CREATE INDEX idx_tasks_user_priority   ON tasks (user_id, priority);
CREATE INDEX idx_tasks_user_due_date   ON tasks (user_id, due_date);
CREATE INDEX idx_tasks_user_created_at ON tasks (user_id, created_at);
