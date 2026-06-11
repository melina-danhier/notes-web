-- Add archived column to notes table
ALTER TABLE notes ADD COLUMN archived BOOLEAN NOT NULL DEFAULT FALSE;

-- Create index for filtering archived notes
CREATE INDEX idx_notes_archived ON notes (archived);
