-- V6 : renommer les codes de permissions SUBSCRIPTION_TYPE_* → TYPE_PLAN_*
--
-- Le RBAC sync est purement additif (IRolesPermissionsSyncService ne
-- supprime jamais). Les UPDATE in-place préservent les références
-- existantes dans role_permission, donc aucune réassignation manuelle
-- ensuite.

UPDATE permissions SET code = 'TYPE_PLAN_CREATE' WHERE code = 'SUBSCRIPTION_TYPE_CREATE';
UPDATE permissions SET code = 'TYPE_PLAN_READ'   WHERE code = 'SUBSCRIPTION_TYPE_READ';
UPDATE permissions SET code = 'TYPE_PLAN_UPDATE' WHERE code = 'SUBSCRIPTION_TYPE_UPDATE';
UPDATE permissions SET code = 'TYPE_PLAN_DELETE' WHERE code = 'SUBSCRIPTION_TYPE_DELETE';
