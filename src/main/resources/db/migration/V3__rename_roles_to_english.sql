-- Rename role libellés FR → EN to match the new RBAC YAML codes.
--   PROPRIETAIRE → OWNER   (entreprise owner)
--   VENDEUR      → SELLER  (counter / cashier in a store)
-- Also renames the matching permission code so @PreAuthorize stays aligned.
--   PROPRIETAIRE_ACCESS → OWNER_ACCESS
--
-- The YAML sync (additive only — never deletes) cannot perform a rename:
-- it would create new OWNER/SELLER rows alongside the existing French ones,
-- leaving orphans. This migration does the actual relabel in-place so
-- existing user-role assignments + audit data stay attached.

UPDATE role
SET libelle = 'OWNER'
WHERE libelle = 'PROPRIETAIRE';

UPDATE role
SET libelle = 'SELLER'
WHERE libelle = 'VENDEUR';

UPDATE permissions
SET code = 'OWNER_ACCESS'
WHERE code = 'PROPRIETAIRE_ACCESS';
