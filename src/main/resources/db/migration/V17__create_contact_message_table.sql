CREATE TABLE contact_message (
    id            UUID         NOT NULL DEFAULT gen_random_uuid() PRIMARY KEY,
    nom           VARCHAR(100) NOT NULL,
    email         VARCHAR(150) NOT NULL,
    sujet         VARCHAR(200) NOT NULL,
    message       TEXT         NOT NULL,
    statut        VARCHAR(20)  NOT NULL DEFAULT 'NOUVEAU',
    reponse       TEXT,
    created_at    TIMESTAMP,
    updated_at    TIMESTAMP,
    created_by    VARCHAR(255),
    updated_by    VARCHAR(255)
);
