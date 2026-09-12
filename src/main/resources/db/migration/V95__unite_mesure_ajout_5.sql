-- V95 — ajout de 5 unites de mesure

INSERT INTO unites_mesure (id, code, libelle, symbole)
VALUES
    (gen_random_uuid(), 'METRE_CUBE',  'Mètre cube',  'm³'),
    (gen_random_uuid(), 'SACHET',      'Sachet',      'sachet'),
    (gen_random_uuid(), 'CENTIMETRE',  'Centimètre',  'cm'),
    (gen_random_uuid(), 'MILLIMETRE',  'Millimètre',  'mm'),
    (gen_random_uuid(), 'GRAMME',      'Gramme',      'g')
