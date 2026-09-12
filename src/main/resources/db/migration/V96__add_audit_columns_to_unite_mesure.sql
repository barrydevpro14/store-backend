-- Adds the standard AuditableEntity columns (created_at / updated_at / created_by /
-- updated_by) to unites_mesure, matching the rest of the schema.
-- Required by backend rule 40: every CRUD list query carries the createdAt filter
-- + ORDER BY entity.createdAt DESC. UniteMesure previously extended BaseEntity
-- (no audit fields) — bringing it in line (see V12 for the same treatment on
-- category_product / quality).

ALTER TABLE unites_mesure
    ADD COLUMN created_at timestamp(6) without time zone,
    ADD COLUMN updated_at timestamp(6) without time zone,
    ADD COLUMN created_by character varying(255),
    ADD COLUMN updated_by character varying(255);
