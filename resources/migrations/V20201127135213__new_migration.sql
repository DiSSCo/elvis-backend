ALTER TABLE institution_facilities
ALTER COLUMN deleted_at TYPE TIMESTAMP USING deleted_at::timestamp;