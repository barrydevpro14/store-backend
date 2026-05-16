-- Option Minimaliste : suppression du champ commande_vente.vendeur_id (redondant avec createdBy
-- de AuditableEntity, qui contient deja l'accountId du createur via AuditorAware).
-- Le vendeur affiche dans les listings est resolu via IAccountService.findUserSummaryByAccountId(createdBy).

ALTER TABLE commande_vente
    DROP CONSTRAINT IF EXISTS fk_commande_vente_vendeur;

ALTER TABLE commande_vente
    DROP COLUMN IF EXISTS vendeur_id;
