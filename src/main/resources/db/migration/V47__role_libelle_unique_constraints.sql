-- V47 : Unicité des libellés de rôles.
--
-- 1. Rôles système (entreprise_id IS NULL) — libelle unique globalement.
-- 2. Rôles custom par entreprise (entreprise_id NOT NULL) — libelle unique par entreprise.
--
-- Les deux index sont case-insensitive (LOWER) pour être cohérents avec les
-- vérifications applicatives existantes.

CREATE UNIQUE INDEX uq_role_libelle_system
    ON role (LOWER(libelle))
    WHERE entreprise_id IS NULL;

CREATE UNIQUE INDEX uq_role_libelle_entreprise
    ON role (LOWER(libelle), entreprise_id)
    WHERE entreprise_id IS NOT NULL;
