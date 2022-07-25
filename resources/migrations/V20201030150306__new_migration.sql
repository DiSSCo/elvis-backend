ALTER TABLE __auth_users_dump ADD migrated_at timestamp;
ALTER TABLE __auth_users_dump ADD migrated_id uuid;
ALTER TABLE institution_moderators DROP COLUMN moderator_id;
