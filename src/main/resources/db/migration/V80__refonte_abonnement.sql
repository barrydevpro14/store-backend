-- ============================================================
-- T2 : CREATE plan_abonnement_tarif
-- ============================================================

CREATE TABLE plan_abonnement_tarif (
    id          UUID            PRIMARY KEY,
    plan_id     UUID            NOT NULL,
    periodicite VARCHAR(30)     NOT NULL,
    prix        DECIMAL(19, 2)  NOT NULL,
    actif       BOOLEAN         NOT NULL DEFAULT TRUE,
    recommande  BOOLEAN         NOT NULL DEFAULT FALSE,
    ordre       INT,
    created_at  TIMESTAMP,
    updated_at  TIMESTAMP,
    created_by  VARCHAR(255),
    updated_by  VARCHAR(255),
    CONSTRAINT fk_pat_plan          FOREIGN KEY (plan_id) REFERENCES plan_abonnement(id),
    CONSTRAINT uk_plan_tarif_periodicite UNIQUE (plan_id, periodicite)
);

-- Backfill : créer un tarif MENSUEL par plan existant (prix = plan.prix)
INSERT INTO plan_abonnement_tarif (id, plan_id, periodicite, prix, actif, recommande, created_at)
SELECT gen_random_uuid(), p.id, 'MENSUEL', p.prix, TRUE, FALSE, NOW()
FROM plan_abonnement p;


-- ============================================================
-- T5 : ALTER abonnement — periodicite + prochaine_periodicite
-- ============================================================

ALTER TABLE abonnement ADD COLUMN periodicite          VARCHAR(30);
ALTER TABLE abonnement ADD COLUMN prochaine_periodicite VARCHAR(30);

-- Backfill periodicite=MENSUEL pour les abonnements non-TRIAL
UPDATE abonnement SET periodicite = 'MENSUEL' WHERE statut <> 'TRIAL';


-- ============================================================
-- T3 : ALTER coupon
-- ============================================================

-- Rename plan_id → plan_abonnement_id
ALTER TABLE coupon RENAME COLUMN plan_id TO plan_abonnement_id;

ALTER TABLE coupon ADD COLUMN periodicite  VARCHAR(30);
ALTER TABLE coupon ADD COLUMN date_debut   DATE NOT NULL;
ALTER TABLE coupon ADD COLUMN date_fin     DATE NOT NULL;


-- ============================================================
-- T4 : ALTER paiement_abonnement
-- ============================================================

ALTER TABLE paiement_abonnement ADD COLUMN plan_abonnement_tarif_id UUID;
ALTER TABLE paiement_abonnement ADD COLUMN coupon_id                UUID;

ALTER TABLE paiement_abonnement
    ADD CONSTRAINT fk_pab_tarif
    FOREIGN KEY (plan_abonnement_tarif_id) REFERENCES plan_abonnement_tarif(id);

ALTER TABLE paiement_abonnement
    ADD CONSTRAINT fk_pab_coupon
    FOREIGN KEY (coupon_id) REFERENCES coupon(id);

-- Backfill : associer le tarif MENSUEL du plan à chaque paiement existant
UPDATE paiement_abonnement pa
SET plan_abonnement_tarif_id = pat.id
FROM abonnement a
JOIN plan_abonnement_tarif pat
    ON pat.plan_id = a.plan_abonnement_id
   AND pat.periodicite = 'MENSUEL'
WHERE pa.abonnement_id = a.id;
