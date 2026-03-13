CREATE TABLE user_preferences (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    username VARCHAR(255) NOT NULL,
    sidebar_project_limit INT NOT NULL DEFAULT 10,
    sidebar_project_staleness VARCHAR(50) NOT NULL DEFAULT 'THREE_MONTHS',
    UNIQUE KEY uk_user_preferences_username (username)
);
