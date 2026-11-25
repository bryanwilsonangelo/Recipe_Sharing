CREATE DATABASE TubesPBW;

CREATE TABLE users (
    idusr SERIAL PRIMARY KEY,
    username VARCHAR(50) NOT NULL UNIQUE,
    full_name VARCHAR(50) NOT NULL,
    password VARCHAR(60) NOT NULL,
    role VARCHAR(20) NOT NULL DEFAULT 'USER'
);

CREATE TABLE categories (
    idcat SERIAL PRIMARY KEY,
    name VARCHAR(50) NOT NULL UNIQUE
);

CREATE TABLE recipes (
    idrec SERIAL PRIMARY KEY,
    title VARCHAR(50) NOT NULL,
    short_description VARCHAR(100),
    instruction TEXT NOT NULL,
    photo_path VARCHAR(255),
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    idusr INT NOT NULL,
    CONSTRAINT fk_recipes_user
        FOREIGN KEY (idusr) REFERENCES users(idusr) ON DELETE CASCADE
);

CREATE TABLE recipes_categories (
    idrec INT NOT NULL,
    idcat INT NOT NULL,
    PRIMARY KEY (idrec, idcat),
    CONSTRAINT fk_rc_recipe
        FOREIGN KEY (idrec) REFERENCES recipes(idrec) ON DELETE CASCADE,
    CONSTRAINT fk_rc_category
        FOREIGN KEY (idcat) REFERENCES categories(idcat) ON DELETE CASCADE
);