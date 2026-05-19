-- Email et téléphone uniques au niveau du compte personnel (Person base table).
-- S'applique à tous les Person : Utilisateur (Proprietaire, Employe) et toute
-- future sous-classe. Permet d'éviter qu'un même email/téléphone serve à
-- plusieurs comptes (collisions à l'inscription, ambiguïté de récupération).
--
-- NOTE Postgres : un index UNIQUE traite chaque NULL comme distinct,
-- donc plusieurs lignes avec email/telephone NULL restent autorisées
-- (les @NotBlank côté DTO empêchent de toute façon ce cas en pratique).

ALTER TABLE person
    ADD CONSTRAINT uk_person_email UNIQUE (email);

ALTER TABLE person
    ADD CONSTRAINT uk_person_telephone UNIQUE (telephone);
