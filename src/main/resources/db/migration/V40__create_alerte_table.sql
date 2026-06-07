-- ── V40 — alerte table for alert reporting ──────────────────────────────────
CREATE TABLE alerte (
    id              UUID        NOT NULL PRIMARY KEY,
    type            VARCHAR(60) NOT NULL,
    statut          VARCHAR(20) NOT NULL DEFAULT 'NOUVELLE',
    titre           VARCHAR(255) NOT NULL,
    message         TEXT,
    entreprise_id   UUID        REFERENCES entreprise(id) ON DELETE SET NULL,
    magasin_id      UUID        REFERENCES magasin(id)    ON DELETE SET NULL,
    entity_id       UUID,
    jours_info      INT,
    created_by      VARCHAR(255),
    updated_by      VARCHAR(255),
    created_at      TIMESTAMP   NOT NULL DEFAULT NOW(),
    updated_at      TIMESTAMP   NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_alerte_entreprise  ON alerte (entreprise_id);
CREATE INDEX idx_alerte_magasin     ON alerte (magasin_id);
CREATE INDEX idx_alerte_type        ON alerte (type);
CREATE INDEX idx_alerte_statut      ON alerte (statut);
CREATE INDEX idx_alerte_created_at  ON alerte (created_at DESC);
