ALTER TABLE notes
    ALTER COLUMN user_id SET NOT NULL;

ALTER TABLE notes
    ADD CONSTRAINT ck_notes_not_deleted_and_archived
        CHECK (NOT (deleted AND archived)),
    ADD CONSTRAINT ck_notes_pinned_only_when_active
        CHECK (NOT pinned OR (NOT deleted AND NOT archived));
