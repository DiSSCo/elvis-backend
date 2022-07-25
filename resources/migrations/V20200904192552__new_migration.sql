ALTER TABLE calls RENAME COLUMN creation_date TO created_at;

alter table institution_facilities rename column institutionid to institution_id;
alter table institution_facilities rename column moderatorid to moderator_id;
alter table institution_facilities rename column delete_date to deleted_at;

alter table call_request rename to requests;
alter table requests rename column delete_date to deleted_at;
alter table requests rename column request_title to title;
alter table requests rename column request_status to status;
alter table requests rename column form_data to form;

alter table call_request_institutions rename to requests_institution_forms;
ALTER TABLE requests_institution_forms ADD deleted_at timestamp;
UPDATE requests_institution_forms set deleted_at = NOW() WHERE deleted = true;
ALTER TABLE requests_institution_forms DROP COLUMN deleted;
alter table requests_institution_forms rename column form_id to request_id;
alter table requests_institution_forms rename column form_data to form;
alter table requests_institution_forms rename column form_status to status;

alter table call_request_attachments rename to requests_attachments;
alter table requests_attachments rename column call_request_id to request_id;

alter table text_settings rename to settings;
alter table settings rename column field to option_key;
alter table settings rename column text to option_value;