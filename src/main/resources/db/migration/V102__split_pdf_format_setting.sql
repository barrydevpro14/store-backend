CREATE TABLE pdf_format_setting (
    id                    UUID          PRIMARY KEY,
    pdf_format_config_id  UUID          NOT NULL REFERENCES pdf_format_config(id),
    magasin_id            UUID          REFERENCES magasin(id),
    page_width            DECIMAL(10,2) NOT NULL,
    page_height           DECIMAL(10,2),
    margin_left           DECIMAL(10,2) NOT NULL,
    margin_right          DECIMAL(10,2) NOT NULL,
    margin_top            DECIMAL(10,2) NOT NULL,
    margin_bottom         DECIMAL(10,2) NOT NULL,
    font_size_title       DECIMAL(5,2)  NOT NULL,
    font_size_normal      DECIMAL(5,2)  NOT NULL,
    font_size_small       DECIMAL(5,2)  NOT NULL,
    created_at            TIMESTAMP,
    updated_at            TIMESTAMP,
    created_by            VARCHAR(255),
    updated_by            VARCHAR(255),
    CONSTRAINT ux_pdf_format_setting_magasin UNIQUE (pdf_format_config_id, magasin_id)
);

CREATE UNIQUE INDEX ux_pdf_format_setting_global ON pdf_format_setting (pdf_format_config_id) WHERE magasin_id IS NULL;

INSERT INTO pdf_format_setting (id, pdf_format_config_id, magasin_id, page_width, page_height, margin_left, margin_right, margin_top, margin_bottom, font_size_title, font_size_normal, font_size_small, created_at)
SELECT gen_random_uuid(), id, NULL, page_width, page_height, margin_left, margin_right, margin_top, margin_bottom, font_size_title, font_size_normal, font_size_small, NOW()
FROM pdf_format_config;

ALTER TABLE pdf_format_config
    DROP COLUMN page_width,
    DROP COLUMN page_height,
    DROP COLUMN margin_left,
    DROP COLUMN margin_right,
    DROP COLUMN margin_top,
    DROP COLUMN margin_bottom,
    DROP COLUMN font_size_title,
    DROP COLUMN font_size_normal,
    DROP COLUMN font_size_small;
