INSERT INTO country (name, country_code, currency)
VALUES ('Guinée Conakry', 'GN', 'GNF')
ON CONFLICT (country_code) DO UPDATE SET name = 'Guinée Conakry';
