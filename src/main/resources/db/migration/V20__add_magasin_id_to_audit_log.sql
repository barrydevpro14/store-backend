ALTER TABLE audit_log ADD COLUMN IF NOT EXISTS magasin_id UUID;
CREATE INDEX idx_audit_log_magasin_id ON audit_log(magasin_id);
