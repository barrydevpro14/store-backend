ALTER TABLE inventaire
    ADD COLUMN type VARCHAR(20) NOT NULL DEFAULT 'PHYSIQUE';

ALTER TABLE inventaire
    ADD CONSTRAINT inventaire_type_check
        CHECK (type IN ('PHYSIQUE', 'AUTOMATIQUE'));
