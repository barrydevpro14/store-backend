ALTER TABLE commande_achat
    ADD COLUMN piece_jointe_id UUID REFERENCES piece_jointe(id);
