CREATE TABLE facturation_pays (
    facturation_id UUID NOT NULL,
    country_id     UUID NOT NULL,
    CONSTRAINT pk_facturation_pays PRIMARY KEY (facturation_id, country_id),
    CONSTRAINT fk_facturation_pays_facturation FOREIGN KEY (facturation_id) REFERENCES facturation (id),
    CONSTRAINT fk_facturation_pays_country FOREIGN KEY (country_id) REFERENCES country (id)
);

INSERT INTO facturation_pays (facturation_id, country_id)
SELECT id, pays_id FROM facturation WHERE pays_id IS NOT NULL;

DROP INDEX IF EXISTS facturation_moyen_pays_key;
DROP INDEX IF EXISTS facturation_moyen_global_key;

ALTER TABLE facturation DROP CONSTRAINT IF EXISTS fk_facturation_pays;
ALTER TABLE facturation DROP COLUMN pays_id;
