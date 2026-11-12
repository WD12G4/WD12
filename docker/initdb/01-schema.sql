-- ==============================================================================
-- 01-schema.sql : Initialisation des tables de l'application Materiel
-- ==============================================================================

-- 1. Table des utilisateurs
CREATE TABLE IF NOT EXISTS users (
    id BIGSERIAL PRIMARY KEY,
    version BIGINT NOT NULL DEFAULT 0,
    username VARCHAR(100) NOT NULL,
    password VARCHAR(255) NOT NULL,
    role VARCHAR(30) NOT NULL DEFAULT 'USER',
    enabled BOOLEAN NOT NULL DEFAULT TRUE,
    CONSTRAINT uk__users__username UNIQUE (username)
);

-- 2. Table des bâtiments
CREATE TABLE IF NOT EXISTS buildings (
    id BIGSERIAL PRIMARY KEY,
    version BIGINT NOT NULL DEFAULT 0,
    number INTEGER NOT NULL,
    CONSTRAINT uk__building__number UNIQUE (number)
);

-- 3. Table des salles (rattachées à un bâtiment)
CREATE TABLE IF NOT EXISTS rooms (
    id BIGSERIAL PRIMARY KEY,
    version BIGINT NOT NULL DEFAULT 0,
    building_id BIGINT NOT NULL,
    number INTEGER NOT NULL,
    CONSTRAINT uk__room__building__number UNIQUE (building_id, number),
    CONSTRAINT fk__rooms__buildings FOREIGN KEY (building_id) REFERENCES buildings(id) ON DELETE CASCADE
);

-- 4. Table des matériels
CREATE TABLE IF NOT EXISTS materials (
    id BIGSERIAL PRIMARY KEY,
    version BIGINT NOT NULL DEFAULT 0,
    serial_number VARCHAR(100) NOT NULL,
    material_type VARCHAR(30) NOT NULL,
    end_availability_date DATE NOT NULL,
    room_id BIGINT,
    archived BOOLEAN NOT NULL DEFAULT FALSE,
    CONSTRAINT uk__material__serial_number UNIQUE (serial_number),
    CONSTRAINT fk__materials__rooms FOREIGN KEY (room_id) REFERENCES rooms(id) ON DELETE SET NULL
);

-- 5. Table des refresh tokens
CREATE TABLE IF NOT EXISTS refresh_tokens (
    id BIGSERIAL PRIMARY KEY,
    version BIGINT NOT NULL DEFAULT 0,
    token_hash VARCHAR(64) NOT NULL,
    family_id VARCHAR(36) NOT NULL,
    user_id BIGINT NOT NULL,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL,
    expires_at TIMESTAMP WITH TIME ZONE NOT NULL,
    revoked_at TIMESTAMP WITH TIME ZONE,
    replaced_by_token_hash VARCHAR(64),
    created_by_ip VARCHAR(64),
    created_by_user_agent VARCHAR(512),
    CONSTRAINT uk__refresh_tokens__token_hash UNIQUE (token_hash),
    CONSTRAINT fk__refresh_tokens__users FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE
);

-- 6. Table des signalements
CREATE TABLE IF NOT EXISTS reports (
    id BIGSERIAL PRIMARY KEY,
    version BIGINT NOT NULL DEFAULT 0,
    description TEXT NOT NULL,
    report_date_time TIMESTAMP NOT NULL,
    user_id BIGINT NOT NULL,
    material_id BIGINT NOT NULL,
    handle_date_time TIMESTAMP,
    handled BOOLEAN NOT NULL DEFAULT FALSE,
    CONSTRAINT uk__report__report_date_time__user__material UNIQUE (material_id, user_id, report_date_time),
    CONSTRAINT fk__reports__users FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE,
    CONSTRAINT fk__reports__materials FOREIGN KEY (material_id) REFERENCES materials(id) ON DELETE CASCADE
);

-- 7. Table des événements d'arrêt (kill events)
CREATE TABLE IF NOT EXISTS killevents (
    id BIGSERIAL PRIMARY KEY,
    version BIGINT NOT NULL DEFAULT 0,
    process_name VARCHAR(100) NOT NULL,
    uc_id BIGINT NOT NULL,
    user_id BIGINT,
    kill_date_time TIMESTAMP NOT NULL,
    result BOOLEAN NOT NULL DEFAULT TRUE,
    CONSTRAINT uk__killevent__process_name__uc__kill_date_time UNIQUE (process_name, uc_id, kill_date_time),
    CONSTRAINT fk__killevents__materials FOREIGN KEY (uc_id) REFERENCES materials(id) ON DELETE CASCADE,
    CONSTRAINT fk__killevents__users FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE SET NULL
);

-- Index pour optimiser les requêtes fréquentes
CREATE INDEX IF NOT EXISTS idx__rooms__building_id ON rooms(building_id);
CREATE INDEX IF NOT EXISTS idx__materials__room_id ON materials(room_id);
CREATE INDEX IF NOT EXISTS idx__materials__material_type ON materials(material_type);
CREATE INDEX IF NOT EXISTS idx__refresh_tokens__user_id ON refresh_tokens(user_id);
CREATE INDEX IF NOT EXISTS idx__reports__user_id ON reports(user_id);
CREATE INDEX IF NOT EXISTS idx__reports__material_id ON reports(material_id);
CREATE INDEX IF NOT EXISTS idx__killevents__user_id ON killevents(user_id);
CREATE INDEX IF NOT EXISTS idx__killevents__uc_id ON killevents(uc_id);
