UPDATE pdf_format_config
SET margin_left   = 4,
    margin_right  = 4,
    margin_top    = 4,
    margin_bottom = 4
WHERE code = 'THERMAL_58MM';

UPDATE pdf_format_config
SET margin_left   = 5,
    margin_right  = 5,
    margin_top    = 5,
    margin_bottom = 5
WHERE code = 'THERMAL_80MM';
