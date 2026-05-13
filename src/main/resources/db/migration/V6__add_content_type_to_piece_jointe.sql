-- Stockage explicite du content-type des pieces jointes (remplace la detection magic bytes au runtime).

ALTER TABLE piece_jointe
    ADD COLUMN content_type VARCHAR(100) NOT NULL;
