-- Create sequences
CREATE SEQUENCE IF NOT EXISTS users_seq START WITH 1 INCREMENT BY 1;
CREATE SEQUENCE IF NOT EXISTS notes_seq START WITH 1 INCREMENT BY 1;
CREATE SEQUENCE IF NOT EXISTS tags_seq START WITH 1 INCREMENT BY 1;

-- Create users table
CREATE TABLE IF NOT EXISTS users (
    id BIGINT PRIMARY KEY DEFAULT nextval('users_seq'),
    username VARCHAR(255),
    email VARCHAR(255),
    password VARCHAR(255),
    role VARCHAR(255) DEFAULT 'ROLE_USER'
);

-- Create tags table
CREATE TABLE IF NOT EXISTS tags (
    id BIGINT PRIMARY KEY DEFAULT nextval('tags_seq'),
    tag VARCHAR(255)
);

-- Create notes table
CREATE TABLE IF NOT EXISTS notes (
    id BIGINT PRIMARY KEY DEFAULT nextval('notes_seq'),
    title VARCHAR(255),
    content TEXT,
    created TIMESTAMP NOT NULL,
    updated TIMESTAMP,
    user_id BIGINT,
    deleted BOOLEAN NOT NULL DEFAULT FALSE,
    CONSTRAINT fk_notes_user FOREIGN KEY (user_id) REFERENCES users (id) ON DELETE CASCADE
);

-- Create note_tag join table
CREATE TABLE IF NOT EXISTS note_tag (
    note_id BIGINT NOT NULL,
    tag_id BIGINT NOT NULL,
    PRIMARY KEY (note_id, tag_id),
    CONSTRAINT fk_note_tag_note FOREIGN KEY (note_id) REFERENCES notes (id) ON DELETE CASCADE,
    CONSTRAINT fk_note_tag_tag FOREIGN KEY (tag_id) REFERENCES tags (id) ON DELETE CASCADE
);

-- Create indexes for better query performance
CREATE INDEX IF NOT EXISTS idx_notes_user_id ON notes (user_id);
CREATE INDEX IF NOT EXISTS idx_notes_deleted ON notes (deleted);
CREATE INDEX IF NOT EXISTS idx_note_tag_tag_id ON note_tag (tag_id);

