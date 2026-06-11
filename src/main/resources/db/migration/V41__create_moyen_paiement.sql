-- V41 : Création de la table moyen_paiement + migration des 4 tables qui stockaient l'enum MoyenPaiement

CREATE TABLE moyen_paiement (
    id               UUID         NOT NULL DEFAULT gen_random_uuid() PRIMARY KEY,
    libelle          VARCHAR(100) NOT NULL,
    code             VARCHAR(20)  NOT NULL UNIQUE,
    actif            BOOLEAN      NOT NULL DEFAULT TRUE,
    created_at       TIMESTAMP,
    updated_at       TIMESTAMP,
    created_by       VARCHAR(255),
    updated_by       VARCHAR(255)
);

INSERT INTO moyen_paiement (libelle, code) VALUES
    ('Espèces',        'CASH'),
    ('Wave',           'WAVE'),
    ('Orange Money',   'OM'),
    ('Carte bancaire', 'CARD');

-- paiement_vente
ALTER TABLE paiement_vente ADD COLUMN moyen_id UUID REFERENCES moyen_paiement(id);
UPDATE paiement_vente pv SET moyen_id = (SELECT id FROM moyen_paiement WHERE code = pv.moyen) WHERE pv.moyen IS NOT NULL;
UPDATE paiement_vente SET moyen_id = (SELECT id FROM moyen_paiement WHERE code = 'CASH') WHERE moyen_id IS NULL;
ALTER TABLE paiement_vente ALTER COLUMN moyen_id SET NOT NULL;
ALTER TABLE paiement_vente DROP COLUMN moyen;

-- paiement_achat
ALTER TABLE paiement_achat ADD COLUMN moyen_id UUID REFERENCES moyen_paiement(id);
UPDATE paiement_achat pa SET moyen_id = (SELECT id FROM moyen_paiement WHERE code = pa.moyen) WHERE pa.moyen IS NOT NULL;
ALTER TABLE paiement_achat ALTER COLUMN moyen_id SET NOT NULL;
ALTER TABLE paiement_achat DROP COLUMN moyen;

-- paiement_abonnement (moyen peut être null — soumis avant validation)
ALTER TABLE paiement_abonnement ADD COLUMN moyen_id UUID REFERENCES moyen_paiement(id);
UPDATE paiement_abonnement pa SET moyen_id = (SELECT id FROM moyen_paiement WHERE code = pa.moyen) WHERE pa.moyen IS NOT NULL;
ALTER TABLE paiement_abonnement DROP COLUMN moyen;

-- depense
ALTER TABLE depense ADD COLUMN moyen_paiement_id UUID REFERENCES moyen_paiement(id);
UPDATE depense d SET moyen_paiement_id = (SELECT id FROM moyen_paiement WHERE code = d.mode_paiement) WHERE d.mode_paiement IS NOT NULL;
UPDATE depense SET moyen_paiement_id = (SELECT id FROM moyen_paiement WHERE code = 'CASH') WHERE moyen_paiement_id IS NULL;
ALTER TABLE depense ALTER COLUMN moyen_paiement_id SET NOT NULL;
ALTER TABLE depense DROP COLUMN mode_paiement;
