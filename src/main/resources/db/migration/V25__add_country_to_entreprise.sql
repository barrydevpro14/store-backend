-- Add country_id as nullable first so existing rows don't fail
ALTER TABLE entreprise ADD COLUMN IF NOT EXISTS country_id UUID REFERENCES country(id);

-- Default existing rows to Senegal
UPDATE entreprise SET country_id = (SELECT id FROM country WHERE country_code = 'SN' LIMIT 1)
WHERE country_id IS NULL;

-- Enforce NOT NULL going forward
ALTER TABLE entreprise ALTER COLUMN country_id SET NOT NULL;
