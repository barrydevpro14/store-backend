-- =============================================================================
-- V2__add_magasin_actif.sql
-- Soft-delete flag for magasin. Default TRUE so existing rows stay active.
-- =============================================================================

ALTER TABLE magasin
    ADD COLUMN actif BOOLEAN NOT NULL DEFAULT TRUE;
