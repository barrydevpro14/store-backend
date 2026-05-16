-- Photo de profil utilisateur : relation @OneToOne vers PieceJointe (decalque
-- du pattern Product.imagePrincipal). Nullable : un utilisateur peut ne pas
-- avoir defini de photo. Le blob est stocke dans piece_jointe.

ALTER TABLE utilisateur
    ADD COLUMN photo_id UUID;

ALTER TABLE utilisateur
    ADD CONSTRAINT fk_utilisateur_photo
        FOREIGN KEY (photo_id) REFERENCES piece_jointe (id);

CREATE INDEX idx_utilisateur_photo ON utilisateur (photo_id);
