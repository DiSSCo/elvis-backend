ALTER TABLE accounts ADD gender varchar;
ALTER TABLE accounts ADD related_institution_id varchar;
UPDATE accounts SET gender = 'undefined' WHERE gender is null;
ALTER TABLE accounts ADD birth_date date;