-- V68: Abonnement refonte — ajoute plan_abonnement_id manquant
--
-- L'entité Abonnement mappe désormais directement plan_abonnement_id (refonte V65),
-- mais la colonne n'a jamais été ajoutée au schéma (V7 l'avait supprimée,
-- V65 n'a pas recréé). Résout : Schema validation: missing column [plan_abonnement_id].
--
-- Nullable : les lignes existantes ne peuvent pas être backfillées
-- (type_plan_abonnement_id + la table type_plan_abonnement ont été supprimés par V65).

ALTER TABLE abonnement ADD COLUMN IF NOT EXISTS plan_abonnement_id uuid;

ALTER TABLE abonnement
    ADD CONSTRAINT fk_abonnement_plan_abonnement
        FOREIGN KEY (plan_abonnement_id) REFERENCES plan_abonnement(id);

CREATE INDEX idx_abonnement_plan_abonnement_id ON abonnement(plan_abonnement_id);
