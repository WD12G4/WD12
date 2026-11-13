-- ==============================================================================
-- 02-data.sql : Données de test / démo pour l'application Materiel
-- ==============================================================================

-- 1. Utilisateurs de test
-- L'authentification est gérée par Keycloak (docker/keycloak/gestion-parc-realm.json).
-- Les comptes portent les mêmes identifiants ('admin', 'manager', 'user').
-- L'identifiant relie le compte Keycloak au rôle de l'application.
INSERT INTO users (username, role, enabled) VALUES
    ('admin', 'ADMIN', true),
    ('manager', 'MANAGER', true),
    ('user', 'USER', true)
ON CONFLICT (username) DO NOTHING;

-- 2. Bâtiments
INSERT INTO buildings (id, number) VALUES
    (1, 101),
    (2, 102),
    (3, 201)
ON CONFLICT (number) DO NOTHING;
SELECT setval('buildings_id_seq', (SELECT MAX(id) FROM buildings));

-- 3. Salles
INSERT INTO rooms (id, building_id, number) VALUES
    (1, 1, 10),
    (2, 1, 11),
    (3, 2, 20),
    (4, 3, 1)
ON CONFLICT (building_id, number) DO NOTHING;
SELECT setval('rooms_id_seq', (SELECT MAX(id) FROM rooms));

-- 4. Matériels de test
INSERT INTO materials (serial_number, material_type, end_availability_date, room_id, archived) VALUES
    ('UC-DELL-001', 'UC', '2028-12-31', 1, false),
    ('UC-DELL-002', 'UC', '2028-12-31', 1, false),
    ('SCR-LG-001', 'SCREEN', '2027-06-30', 1, false),
    ('SCR-LG-002', 'SCREEN', '2027-06-30', 2, false),
    ('PRJ-EPSON-001', 'PROJECTOR', '2029-01-15', 3, false),
    ('PER-MOUSE-001', 'PERIPHERAL', '2026-11-30', 1, false),
    ('UC-HP-OLD-099', 'UC', '2024-01-01', NULL, true)
ON CONFLICT (serial_number) DO NOTHING;
SELECT setval('materials_id_seq', (SELECT COALESCE(MAX(id), 1) FROM materials));
