DROP TABLE IF EXISTS password_reset_tokens;

ALTER TABLE auth_users rename to __auth_users_dump;
ALTER TABLE moderators rename to __moderators_dump;
ALTER TABLE requesters rename to __requesters_dump;


CREATE TABLE IF NOT EXISTS accounts
(
	id uuid constraint users_pk primary key,
	email varchar not null,
	first_name varchar,
	last_name varchar,
	group_list varchar[] not null,
	permission_list varchar[] not null,
	attributes jsonb not null,
	realm_id varchar not null
);

CREATE UNIQUE index users_realm on accounts (id, realm_id);

ALTER TABLE institutions_coordinators RENAME TO __institutions_coordinators_dump;

CREATE TABLE IF NOT EXISTS institutions_coordinators(
    id uuid constraint institutions_coordinators_pk primary key,
    institution_id varchar NOT NULL
);

DROP TABLE IF EXISTS settings;
CREATE TABLE settings
(
	id uuid constraint settings_pk primary key,
	option_key varchar not null,
	option_type varchar not null,
	option_value text not null
);
CREATE UNIQUE index settings_option_key_uindex on settings (option_key);
INSERT INTO settings (id, option_key, option_value, option_type) VALUES
(
'ea75d761-7a6c-4c53-9d60-d3cf577d9551',
'main_page_text',
'This portal supports making application requests in Call 1 of Virtual Access (VA). Call 1 of Virtual Access will open for Proposals on 20 February. Proposers must contact relevant VA Coordinators as soon as possible, at the latest by 30 April. The deadline for the Call, so for completing VA requests, is Friday the 10th of July 2020 at 17:00 BST.',
'string'
);

ALTER TABLE calls add deleted_at timestamp default null;
UPDATE calls SET deleted_at = NOW() where deleted = true;
ALTER TABLE calls DROP COLUMN deleted;


ALTER TABLE institution_facilities ADD CONSTRAINT institution_facilities_user FOREIGN KEY (moderator_id) REFERENCES accounts;
ALTER TABLE institution_moderators ADD CONSTRAINT institution_moderators_user FOREIGN KEY (moderator_id) REFERENCES accounts;
ALTER TABLE institutions_coordinators ADD CONSTRAINT institutions_coordinators_user FOREIGN KEY (id) REFERENCES accounts;