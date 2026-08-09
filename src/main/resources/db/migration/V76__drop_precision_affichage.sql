-- V76 — Suppression de la colonne precision_affichage dans unites_mesure
--        (champ retiré de l'entité UniteMesure — la précision d'affichage
--         est désormais fixée à 2 décimales pour toutes les quantités)
ALTER TABLE unites_mesure DROP COLUMN precision_affichage;
