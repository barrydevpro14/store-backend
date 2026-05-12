-- =============================================================================
-- V3__add_entreprise_actif.sql
-- Soft-delete flag for entreprise. Default TRUE so existing rows stay active.
-- =============================================================================

ALTER TABLE entreprise
    ADD COLUMN actif BOOLEAN NOT NULL DEFAULT TRUE;
