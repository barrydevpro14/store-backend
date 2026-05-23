-- Adds the standard AuditableEntity columns (created_at / updated_at / created_by /
-- updated_by) to category_product and quality, matching the rest of the schema.
-- Required by backend rule 40: every CRUD list query carries the createdAt filter
-- + ORDER BY entity.createdAt DESC. The two entities (CategoryProduct, Quality)
-- previously extended BaseEntity (no audit fields) — bringing them in line.

ALTER TABLE category_product
    ADD COLUMN created_at timestamp(6) without time zone,
    ADD COLUMN updated_at timestamp(6) without time zone,
    ADD COLUMN created_by character varying(255),
    ADD COLUMN updated_by character varying(255);

ALTER TABLE quality
    ADD COLUMN created_at timestamp(6) without time zone,
    ADD COLUMN updated_at timestamp(6) without time zone,
    ADD COLUMN created_by character varying(255),
    ADD COLUMN updated_by character varying(255);
