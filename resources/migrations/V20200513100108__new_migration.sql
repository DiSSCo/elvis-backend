ALTER TABLE call_request_attachments ADD institution_id varchar NULL;
CREATE INDEX IF NOT EXISTS call_request_attachments_c ON call_request_attachments (call_request_id, institution_id);
