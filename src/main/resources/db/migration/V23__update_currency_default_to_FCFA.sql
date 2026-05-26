-- Rename XOF → FCFA on existing rows and update the column default.
-- XOF (ISO 4217) and FCFA (Franc CFA display name) refer to the same currency.
UPDATE entreprise SET currency = 'FCFA' WHERE currency = 'XOF';
ALTER TABLE entreprise ALTER COLUMN currency SET DEFAULT 'FCFA';
