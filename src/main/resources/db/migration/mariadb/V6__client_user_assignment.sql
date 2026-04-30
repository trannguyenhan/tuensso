CREATE TABLE IF NOT EXISTS client_user_assignment (
    client_id VARCHAR(255) NOT NULL,
    user_id   BINARY(16)     NOT NULL,
    PRIMARY KEY (client_id, user_id),
    FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
