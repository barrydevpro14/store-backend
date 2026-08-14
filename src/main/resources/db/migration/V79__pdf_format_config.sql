CREATE TABLE pdf_format_config (
    id               UUID          PRIMARY KEY,
    code             VARCHAR(50)   NOT NULL UNIQUE,
    label            VARCHAR(100)  NOT NULL,
    format           VARCHAR(50)   NOT NULL CHECK (format IN ('A4', 'A5', 'THERMAL_80MM', 'THERMAL_58MM')),
    page_width       DECIMAL(10,2),
    page_height      DECIMAL(10,2),
    margin_left      DECIMAL(10,2) NOT NULL DEFAULT 40,
    margin_right     DECIMAL(10,2) NOT NULL DEFAULT 40,
    margin_top       DECIMAL(10,2) NOT NULL DEFAULT 40,
    margin_bottom    DECIMAL(10,2) NOT NULL DEFAULT 40,
    font_size_title  DECIMAL(5,2),
    font_size_normal DECIMAL(5,2),
    font_size_small  DECIMAL(5,2),
    enabled          BOOLEAN       NOT NULL DEFAULT TRUE,
    created_at       TIMESTAMP,
    updated_at       TIMESTAMP,
    created_by       VARCHAR(255),
    updated_by       VARCHAR(255)
);

INSERT INTO pdf_format_config (id, code, label, format, page_width, page_height, margin_left, margin_right, margin_top, margin_bottom, font_size_title, font_size_normal, font_size_small, enabled, created_at)
VALUES
    (gen_random_uuid(), 'A4',           'A4',                    'A4',           595, 842, 40, 40,  40,  120, 14, 10, 8, TRUE, NOW()),
    (gen_random_uuid(), 'A5',           'A5',                    'A5',           420, 595, 25, 25,  25,  80,  12, 9,  7, TRUE, NOW()),
    (gen_random_uuid(), 'THERMAL_80MM', 'Ticket thermique 80mm', 'THERMAL_80MM', 226, 0,   10, 10,  10,  10,  10, 8,  7, TRUE, NOW()),
    (gen_random_uuid(), 'THERMAL_58MM', 'Ticket thermique 58mm', 'THERMAL_58MM', 164, 0,   8,  8,   8,   8,   9,  7,  6, TRUE, NOW());
