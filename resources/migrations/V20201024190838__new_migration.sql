ALTER TABLE accounts ADD synchronized_at timestamp;
ALTER TABLE accounts DROP COLUMN attributes;
ALTER TABLE accounts ADD orc_id varchar;
ALTER TABLE accounts ADD institution_id varchar;
CREATE INDEX accounts_institution_id ON accounts (institution_id);
CREATE INDEX accounts_orc_id ON accounts (orc_id);
