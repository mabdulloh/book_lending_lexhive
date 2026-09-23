CREATE TABLE books (
    id              BIGSERIAL PRIMARY KEY,
    uuid            UUID NOT NULL UNIQUE,
    title           VARCHAR(200) NOT NULL,
    author          VARCHAR(120) NOT NULL,
    isbn            VARCHAR(20) NOT NULL UNIQUE,
    total_copies    INT NOT NULL CHECK (total_copies >= 0),
    available_copies INT NOT NULL CHECK (available_copies >= 0),
    version         BIGINT,
    deleted_at      TIMESTAMP,
    created_at      TIMESTAMP NOT NULL,
    updated_at      TIMESTAMP NOT NULL
);

CREATE TABLE members (
    id          BIGSERIAL PRIMARY KEY,
    uuid        UUID NOT NULL UNIQUE,
    name        VARCHAR(120) NOT NULL,
    email       VARCHAR(180) NOT NULL UNIQUE,
    version     BIGINT,
    deleted_at  TIMESTAMP,
    created_at  TIMESTAMP NOT NULL,
    updated_at  TIMESTAMP NOT NULL
);

CREATE TABLE users (
    id           BIGSERIAL PRIMARY KEY,
    uuid         UUID NOT NULL UNIQUE,
    username     VARCHAR(120) NOT NULL UNIQUE,
    password_hash VARCHAR(180) NOT NULL,
    role         VARCHAR(20) NOT NULL CHECK (role IN ('ADMIN', 'MEMBER')),
    member_id    BIGINT REFERENCES members(id),
    created_at   TIMESTAMP NOT NULL,
    updated_at   TIMESTAMP NOT NULL,
    deleted_at   TIMESTAMP
);

CREATE TABLE loans (
    id           BIGSERIAL PRIMARY KEY,
    uuid         UUID NOT NULL UNIQUE,
    book_id      BIGINT NOT NULL REFERENCES books(id),
    member_id    BIGINT NOT NULL REFERENCES members(id),
    borrowed_at  TIMESTAMP NOT NULL,
    due_date     TIMESTAMP NOT NULL,
    returned_at  TIMESTAMP,
    version      BIGINT,
    created_at   TIMESTAMP NOT NULL,
    updated_at   TIMESTAMP NOT NULL
);

CREATE INDEX idx_books_deleted_at ON books(deleted_at);
CREATE INDEX idx_members_deleted_at ON members(deleted_at);
CREATE INDEX idx_users_username ON users(username);
CREATE INDEX idx_users_member_id ON users(member_id);
CREATE INDEX idx_users_deleted_at ON users(deleted_at);
CREATE INDEX idx_loans_member ON loans(member_id);
CREATE INDEX idx_loans_book ON loans(book_id);
CREATE INDEX idx_loans_returned_at ON loans(returned_at);
CREATE INDEX idx_loans_due_date ON loans(due_date);