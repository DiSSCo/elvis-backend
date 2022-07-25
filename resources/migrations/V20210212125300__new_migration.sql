CREATE TABLE IF NOT EXISTS institutions_taf_administrators
(
    id uuid constraint institutions_taf_administrators_pk primary key,
    user_id uuid not null,
    institution_id varchar
);

ALTER TABLE institutions_taf_administrators ADD CONSTRAINT institutions_taf_administrators_i FOREIGN KEY (institution_id) REFERENCES institutions;
ALTER TABLE institutions_taf_administrators ADD CONSTRAINT institutions_taf_administrators_u FOREIGN KEY (user_id) REFERENCES accounts;

CREATE UNIQUE INDEX institutions_taf_administrators_unique on institutions_taf_administrators (user_id, institution_id);

CREATE TABLE IF NOT EXISTS institutions_scorers
(
    id uuid constraint institutions_scorers_pk primary key,
    user_id uuid not null,
    institution_id varchar
);

ALTER TABLE institutions_scorers ADD CONSTRAINT institutions_scorers_pk_i FOREIGN KEY (institution_id) REFERENCES institutions;
ALTER TABLE institutions_scorers ADD CONSTRAINT institutions_scorers_pk_u FOREIGN KEY (user_id) REFERENCES accounts;

CREATE UNIQUE INDEX institutions_scorers_unique on institutions_scorers (user_id, institution_id);
