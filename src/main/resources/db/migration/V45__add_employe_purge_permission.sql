-- V45 : Ajout de la permission EMPLOYE_PURGE (suppression definitive d'employe par OWNER/ADMIN)

INSERT INTO permissions (id, code)
SELECT gen_random_uuid(), 'EMPLOYE_PURGE'
WHERE NOT EXISTS (SELECT 1 FROM permissions WHERE code = 'EMPLOYE_PURGE');

-- Assigner a OWNER
INSERT INTO role_permission (role_id, permission_id)
SELECT r.id, p.id
FROM role r, permissions p
WHERE r.libelle = 'OWNER'
  AND p.code    = 'EMPLOYE_PURGE'
  AND NOT EXISTS (
    SELECT 1 FROM role_permission rp WHERE rp.role_id = r.id AND rp.permission_id = p.id
  );

-- Assigner a ADMIN
INSERT INTO role_permission (role_id, permission_id)
SELECT r.id, p.id
FROM role r, permissions p
WHERE r.libelle = 'ADMIN'
  AND p.code    = 'EMPLOYE_PURGE'
  AND NOT EXISTS (
    SELECT 1 FROM role_permission rp WHERE rp.role_id = r.id AND rp.permission_id = p.id
  );
