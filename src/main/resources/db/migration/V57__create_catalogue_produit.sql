CREATE TABLE catalogue_produit
(
    id                      UUID         NOT NULL DEFAULT gen_random_uuid(),
    activite_economique_id  UUID         NOT NULL,
    reference               VARCHAR(255) NOT NULL,
    libelle                 VARCHAR(255) NOT NULL,
    categorie               VARCHAR(255),
    description             VARCHAR(500),
    created_at              TIMESTAMP,
    updated_at              TIMESTAMP,
    created_by              VARCHAR(255),
    updated_by              VARCHAR(255),
    CONSTRAINT pk_catalogue_produit PRIMARY KEY (id),
    CONSTRAINT fk_catalogue_produit_activite FOREIGN KEY (activite_economique_id) REFERENCES activite_economique (id),
    CONSTRAINT uq_catalogue_produit_ref_libelle UNIQUE (activite_economique_id, reference, libelle)
);

ALTER TABLE product
    ADD CONSTRAINT uq_product_entreprise_ref_nom UNIQUE (entreprise_id, reference, nom);
