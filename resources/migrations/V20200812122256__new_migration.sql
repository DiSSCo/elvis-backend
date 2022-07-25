ALTER TABLE calls
ADD COLUMN IF NOT EXISTS deleted bool;
UPDATE calls SET deleted = false WHERE deleted = null;