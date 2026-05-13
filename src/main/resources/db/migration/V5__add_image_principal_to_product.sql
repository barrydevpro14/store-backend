-- Image principale du produit : relation @OneToOne vers PieceJointe (independante de la galerie product.images).

ALTER TABLE product
    ADD COLUMN image_principal_id UUID;

ALTER TABLE product
    ADD CONSTRAINT fk_product_image_principal
        FOREIGN KEY (image_principal_id) REFERENCES piece_jointe (id);

CREATE INDEX idx_product_image_principal ON product (image_principal_id);
