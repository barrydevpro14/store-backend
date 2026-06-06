-- Drops the currency column added by V22, which has been removed from the codebase.
ALTER TABLE entreprise DROP COLUMN IF EXISTS currency;
