ALTER TABLE auth_users ADD disable_date timestamp;
ALTER TABLE auth_users ADD disable_reason varchar;
UPDATE auth_users SET email = LOWER(email);
UPDATE requesters SET email = LOWER(email);
UPDATE institutions_coordinators SET email = LOWER(email);
