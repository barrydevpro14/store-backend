-- Logos magasin et entreprise : relations @OneToOne vers PieceJointe.
-- Meme pattern que Product.imagePrincipal et Utilisateur.photo.
-- Nullable : un magasin ou une entreprise peut ne pas avoir de logo defini.

ALTER TABLE magasin
    ADD COLUMN logo_id UUID;

ALTER TABLE magasin
    ADD CONSTRAINT fk_magasin_logo
        FOREIGN KEY (logo_id) REFERENCES piece_jointe (id);

CREATE INDEX idx_magasin_logo ON magasin (logo_id);

ALTER TABLE entreprise
    ADD COLUMN logo_id UUID;

ALTER TABLE entreprise
    ADD CONSTRAINT fk_entreprise_logo
        FOREIGN KEY (logo_id) REFERENCES piece_jointe (id);

CREATE INDEX idx_entreprise_logo ON entreprise (logo_id);
