CREATE TABLE password_reset_token (
    id           UUID         PRIMARY KEY DEFAULT gen_random_uuid(),
    account_id   UUID         NOT NULL REFERENCES account(id) ON DELETE CASCADE,
    token        VARCHAR(255) NOT NULL UNIQUE,
    expires_at   TIMESTAMP    NOT NULL,
    used         BOOLEAN      NOT NULL DEFAULT FALSE,
    created_at   TIMESTAMP    NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_prt_token      ON password_reset_token(token);
CREATE INDEX idx_prt_account_id ON password_reset_token(account_id);
