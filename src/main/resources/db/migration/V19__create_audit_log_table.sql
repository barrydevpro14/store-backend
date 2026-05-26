CREATE TABLE audit_log (
    id               UUID         PRIMARY KEY DEFAULT gen_random_uuid(),
    action           VARCHAR(100) NOT NULL,
    entity_type      VARCHAR(100) NOT NULL,
    entity_id        UUID,
    entity_label     VARCHAR(500),
    performed_by     VARCHAR(255) NOT NULL,
    performed_by_label VARCHAR(255),
    entreprise_id    UUID,
    details          TEXT,
    created_at       TIMESTAMP    NOT NULL
);

CREATE INDEX idx_audit_log_action        ON audit_log(action);
CREATE INDEX idx_audit_log_entity_type   ON audit_log(entity_type);
CREATE INDEX idx_audit_log_entreprise_id ON audit_log(entreprise_id);
CREATE INDEX idx_audit_log_performed_by  ON audit_log(performed_by);
CREATE INDEX idx_audit_log_created_at    ON audit_log(created_at DESC);
