CREATE TABLE IF NOT EXISTS avatar_state (
    avatar_id   VARCHAR(50) PRIMARY KEY,
    x           INTEGER      NOT NULL,
    y           INTEGER      NOT NULL,
    tick        BIGINT       NOT NULL,
    updated_at  TIMESTAMPTZ  NOT NULL
);

CREATE TABLE IF NOT EXISTS position_history (
    id          BIGSERIAL    PRIMARY KEY,
    avatar_id   VARCHAR(50)  NOT NULL,
    x           INTEGER      NOT NULL,
    y           INTEGER      NOT NULL,
    tick        BIGINT       NOT NULL,
    received_at TIMESTAMPTZ  NOT NULL
);

CREATE INDEX IF NOT EXISTS idx_position_history_avatar_id ON position_history(avatar_id);
CREATE INDEX IF NOT EXISTS idx_position_history_tick       ON position_history(tick);
