-- Persistance des infos lot (numero + date d'expiration) sur ligne_commande_achat.
-- Auparavant ces infos étaient transmises directement de LigneAchatRequest à EntreeStockCreate
-- au moment de la création atomique. Avec le refactor 2 étapes (DRAFT → VALIDATE),
-- la matérialisation stock se fait à la validation et la traçabilité doit être conservée
-- entre les 2 phases (saisie en DRAFT, lecture à VALIDATE).

ALTER TABLE ligne_commande_achat
    ADD COLUMN numero_lot      VARCHAR(100),
    ADD COLUMN date_expiration DATE;
