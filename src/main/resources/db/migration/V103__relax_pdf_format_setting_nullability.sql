ALTER TABLE pdf_format_setting
    ALTER COLUMN page_width DROP NOT NULL,
    ALTER COLUMN font_size_title DROP NOT NULL,
    ALTER COLUMN font_size_normal DROP NOT NULL,
    ALTER COLUMN font_size_small DROP NOT NULL;
