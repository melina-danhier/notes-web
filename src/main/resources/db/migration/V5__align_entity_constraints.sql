UPDATE notes
SET title = 'Notiz'
WHERE title IS NULL OR btrim(title) = '';

UPDATE notes
SET content = ''
WHERE content IS NULL;

UPDATE users
SET username = split_part(email, '@', 1)
WHERE username IS NULL OR btrim(username) = '';

UPDATE users
SET role = 'ROLE_USER'
WHERE role IS NULL OR btrim(role) = '';

UPDATE tags
SET tag = lower(btrim(tag))
WHERE tag IS NOT NULL;

WITH normalized_duplicates AS (
    SELECT id,
           MIN(id) OVER (PARTITION BY tag) AS keep_id
    FROM tags
    WHERE tag IS NOT NULL
      AND btrim(tag) <> ''
)
INSERT INTO note_tag (note_id, tag_id)
SELECT DISTINCT nt.note_id, nd.keep_id
FROM note_tag nt
JOIN normalized_duplicates nd ON nd.id = nt.tag_id
WHERE nd.id <> nd.keep_id
ON CONFLICT DO NOTHING;

WITH normalized_duplicates AS (
    SELECT id,
           MIN(id) OVER (PARTITION BY tag) AS keep_id
    FROM tags
    WHERE tag IS NOT NULL
      AND btrim(tag) <> ''
)
DELETE FROM note_tag nt
USING normalized_duplicates nd
WHERE nt.tag_id = nd.id
  AND nd.id <> nd.keep_id;

DELETE FROM note_tag nt
USING tags t
WHERE nt.tag_id = t.id
  AND (t.tag IS NULL OR btrim(t.tag) = '');

WITH normalized_duplicates AS (
    SELECT id,
           MIN(id) OVER (PARTITION BY tag) AS keep_id
    FROM tags
    WHERE tag IS NOT NULL
      AND btrim(tag) <> ''
)
DELETE FROM tags t
USING normalized_duplicates nd
WHERE t.id = nd.id
  AND nd.id <> nd.keep_id;

DELETE FROM tags
WHERE tag IS NULL OR btrim(tag) = '';

ALTER TABLE notes
    ALTER COLUMN title SET NOT NULL;

ALTER TABLE notes
    ALTER COLUMN content SET NOT NULL;

ALTER TABLE users
    ALTER COLUMN username SET NOT NULL;

ALTER TABLE users
    ALTER COLUMN role SET NOT NULL;

ALTER TABLE tags
    ALTER COLUMN tag SET NOT NULL;

ALTER TABLE tags
    ADD CONSTRAINT uq_tags_tag UNIQUE (tag);
