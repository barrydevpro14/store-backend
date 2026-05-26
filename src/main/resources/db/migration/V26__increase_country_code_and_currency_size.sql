-- Convert CHAR → VARCHAR to match Hibernate's String→varchar mapping, and increase size to 5.
ALTER TABLE country ALTER COLUMN country_code TYPE VARCHAR(5);
ALTER TABLE country ALTER COLUMN currency TYPE VARCHAR(5);
