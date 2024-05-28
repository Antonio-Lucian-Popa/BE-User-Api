CREATE EXTENSION IF NOT EXISTS "uuid-ossp";

-- Creating the users table
CREATE TABLE users (
    id UUID PRIMARY KEY,
    first_name VARCHAR(30) NOT NULL,
    last_name VARCHAR(30) NOT NULL,
    email VARCHAR(50) NOT NULL,
    password VARCHAR(100) NOT NULL,
    birthday TIMESTAMP NOT NULL,
    gender VARCHAR(30),
    profile_image VARCHAR(255),
    role VARCHAR(20) NOT NULL,
    activation_code VARCHAR(36),
    enabled BOOLEAN NOT NULL
);

-- changeset yourname:create-token-table
CREATE TABLE token (
    id UUID PRIMARY KEY,
    token VARCHAR(255) UNIQUE NOT NULL,
    token_type VARCHAR(255) NOT NULL,
    revoked BOOLEAN NOT NULL,
    expired BOOLEAN NOT NULL,
    user_id UUID,
    FOREIGN KEY (user_id) REFERENCES users(id)
);