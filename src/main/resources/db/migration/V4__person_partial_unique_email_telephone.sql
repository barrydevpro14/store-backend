-- Person.email + Person.telephone : remplace les UNIQUE strictes par des
-- index UNIQUE partiels qui ignorent NULL **et** chaîne vide.
--
-- Problème résolu : `@Column(unique = true)` côté JPA génère un
-- `UNIQUE(email)` strict. PostgreSQL accepte plusieurs NULL sous une
-- contrainte UNIQUE, mais rejette plusieurs chaînes vides (`""`). Or
-- les Clients / Fournisseurs / Employés peuvent légitimement n'avoir
-- ni email ni téléphone — la 2e insertion avec champ vide ou null
-- bombait la contrainte.
--
-- Solution : index partiel `WHERE col IS NOT NULL AND col <> ''`. Les
-- valeurs réelles restent uniques ; tout ce qui est null/vide est
-- ignoré par l'index. L'annotation `@Column(unique = true)` côté
-- entité reste en place (metadata-only à runtime, pas de DDL
-- Hibernate ici — Flyway est autoritaire).

-- 1. Nettoyer les valeurs blank existantes en NULL pour homogénéiser.
UPDATE person SET email = NULL WHERE email IS NOT NULL AND TRIM(email) = '';
UPDATE person SET telephone = NULL WHERE telephone IS NOT NULL AND TRIM(telephone) = '';

-- 2. Drop les contraintes UNIQUE strictes auto-générées par Hibernate.
ALTER TABLE person DROP CONSTRAINT IF EXISTS person_email_key;
ALTER TABLE person DROP CONSTRAINT IF EXISTS person_telephone_key;

-- 3. Recrée des index UNIQUE partiels qui n'enforce l'unicité que sur
--    les valeurs réelles (non-null ET non-blank).
CREATE UNIQUE INDEX IF NOT EXISTS person_email_unique
    ON person(email)
    WHERE email IS NOT NULL AND email <> '';

CREATE UNIQUE INDEX IF NOT EXISTS person_telephone_unique
    ON person(telephone)
    WHERE telephone IS NOT NULL AND telephone <> '';
