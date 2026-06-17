-- Add pinned column to 'notes' table
ALTER TABLE notes ADD COLUMN pinned BOOLEAN NOT NULL DEFAULT FALSE;

-- Create index for better query performance when sorting by pinned
CREATE INDEX idx_notes_pinned ON notes (pinned);

