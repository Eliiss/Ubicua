-- 01_schema.sql

USE ubicua_db;

-- Tabla para Estaciones Meteorológicas (basado en pág. 2 del PDF)
CREATE TABLE IF NOT EXISTS weather_stats (
    id INT AUTO_INCREMENT PRIMARY KEY,
    sensor_id VARCHAR(50),
    timestamp_sensor DATETIME,
    temperature FLOAT,
    humidity FLOAT,
    air_quality INT,
    wind_speed FLOAT,
    wind_direction INT,
    pressure FLOAT,
    uv_index INT,
    recorded_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- Tabla para Contadores de Tráfico (basado en pág. 3 del PDF)
CREATE TABLE IF NOT EXISTS traffic_stats (
    id INT AUTO_INCREMENT PRIMARY KEY,
    sensor_id VARCHAR(50),
    timestamp_sensor DATETIME,
    vehicle_count INT,
    pedestrian_count INT,
    bicycle_count INT,
    average_speed FLOAT,
    occupancy_percentage INT,
    recorded_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- Tabla para Semáforos (basado en pág. 4 del PDF)
CREATE TABLE IF NOT EXISTS traffic_lights (
    id INT AUTO_INCREMENT PRIMARY KEY,
    sensor_id VARCHAR(50),
    current_state VARCHAR(20), -- green, red...
    time_remaining INT,
    pedestrian_waiting BOOLEAN,
    cycle_count INT,
    last_change DATETIME,
    recorded_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- Tabla para Pantallas de Información (basado en pág. 5 del PDF)
CREATE TABLE IF NOT EXISTS info_displays (
    id INT AUTO_INCREMENT PRIMARY KEY,
    sensor_id VARCHAR(50),
    display_status VARCHAR(20),
    current_message TEXT,
    brightness INT,
    temperature FLOAT,
    energy_watts FLOAT,
    recorded_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);
