-- Create explicit sequences for proper JPA/Hibernate integration
CREATE SEQUENCE IF NOT EXISTS users_seq START WITH 1 INCREMENT BY 1;
CREATE SEQUENCE IF NOT EXISTS tags_seq START WITH 1 INCREMENT BY 1;
CREATE SEQUENCE IF NOT EXISTS notes_seq START WITH 1 INCREMENT BY 1;

-- Alter users table to use explicit sequence
ALTER TABLE users ALTER COLUMN id SET DEFAULT nextval('users_seq');

-- Alter tags table to use explicit sequence
ALTER TABLE tags ALTER COLUMN id SET DEFAULT nextval('tags_seq');

-- Alter notes table to use explicit sequence
ALTER TABLE notes ALTER COLUMN id SET DEFAULT nextval('notes_seq');

