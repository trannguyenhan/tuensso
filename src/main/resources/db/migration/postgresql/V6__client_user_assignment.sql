CREATE TABLE client_user_assignment (
    client_id VARCHAR(255) NOT NULL,
    user_id   UUID         NOT NULL,
    PRIMARY KEY (client_id, user_id),
    FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE
);
