CREATE TABLE document_sequence (
    id                  UUID         NOT NULL DEFAULT gen_random_uuid() PRIMARY KEY,
    magasin_id          UUID         NOT NULL,
    type_document       VARCHAR(30)  NOT NULL,
    prefixe             VARCHAR(20)  NOT NULL,
    prochaine_sequence  BIGINT       NOT NULL DEFAULT 1,
    longueur_sequence   INTEGER      NOT NULL DEFAULT 6,
    created_at          TIMESTAMP,
    updated_at          TIMESTAMP,
    created_by          VARCHAR(255),
    updated_by          VARCHAR(255),
    CONSTRAINT uk_magasin_type_document UNIQUE (magasin_id, type_document)
);
