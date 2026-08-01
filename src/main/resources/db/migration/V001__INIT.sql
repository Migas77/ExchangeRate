CREATE TYPE user_role as ENUM ('FREE_TIER', 'PREMIUM_TIER', 'MAX_TIER');

CREATE TABLE users
(
    id       UUID PRIMARY KEY,
    email    VARCHAR(255) NOT NULL UNIQUE,
    password VARCHAR(255) NOT NULL,
    role     user_role    NOT NULL
);
