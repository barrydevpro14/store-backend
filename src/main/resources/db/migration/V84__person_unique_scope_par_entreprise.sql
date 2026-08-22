-- Supprime les index UNIQUE globaux sur person.email et person.telephone.
-- L'unicité est désormais gérée au niveau applicatif, scopée par entreprise
-- et par type (Client, Fournisseur, Employe vérifient chacun dans leur propre
-- table). Cela permet à un même individu d'être Client chez une entreprise
-- ET Employe/Fournisseur chez une autre sans conflit.
DROP INDEX IF EXISTS person_email_unique;
DROP INDEX IF EXISTS person_telephone_unique;
