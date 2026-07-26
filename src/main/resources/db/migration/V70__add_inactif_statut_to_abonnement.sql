-- V70: add INACTIF to the abonnement.statut CHECK constraint.
-- INACTIF = admin-deactivated subscription (distinct from SUSPENDU = non-payment).

ALTER TABLE abonnement DROP CONSTRAINT IF EXISTS abonnement_statut_check;
ALTER TABLE abonnement ADD CONSTRAINT abonnement_statut_check
    CHECK (statut IN ('ACTIF', 'EXPIRE', 'SUSPENDU', 'EN_ATTENTE', 'TRIAL', 'INACTIF'));
