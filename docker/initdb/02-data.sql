-- ==============================================================================
-- 02-data.sql : Données de test / démo pour l'application Materiel
-- ==============================================================================

-- 1. Utilisateurs de test
-- La connexion passe par Keycloak (docker/keycloak/gestion-parc-realm.json) : les comptes y
-- portent les mêmes identifiants et mots de passe. Ici, l'identifiant relie le compte Keycloak
-- au rôle de l'application ; le mot de passe stocké n'est plus vérifié à la connexion.
-- Mots de passe chiffrés en BCrypt :
-- 'admin'   -> mot de passe : 'admin123'
-- 'manager' -> mot de passe : 'manager123'
-- 'user'    -> mot de passe : 'user123'
INSERT INTO users (username, password, role, enabled) VALUES
    ('admin', '$2a$10$3PpORdX6N7emD6AAuV5HHOCVpg19GSNQ8woz6oIZ3fjmd8M5ftu4m', 'ADMIN', true),
    ('manager', '$2a$10$FwLRxTvC5jC1luaEfRHScemmx4s1d6zmbbLVriMiSkJXLVoiXkhzm', 'MANAGER', true),
    ('user', '$2a$10$a8wmFP3IddEoUJvVXs3QNugQeOe1N6jXrkgRZyS9IemD7J1Vi3BbK', 'USER', true)
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
