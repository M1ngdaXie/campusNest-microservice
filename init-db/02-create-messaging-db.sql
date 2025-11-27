-- Create messaging database
CREATE DATABASE IF NOT EXISTS campusNest_messaging CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;

-- Grant privileges to campusnest user on messaging database
GRANT ALL PRIVILEGES ON campusNest_messaging.* TO 'campusnest'@'%';
FLUSH PRIVILEGES;