ALTER TABLE requests ADD resource_id uuid DEFAULT null;
ALTER TABLE institutions  ADD resource_id uuid DEFAULT null;

DELETE FROM __auth_users_dump WHERE id = '056d125c-99a6-4b8d-9ec6-119bede7c076';
DELETE FROM requests WHERE requester_id = '056d125c-99a6-4b8d-9ec6-119bede7c076';
INSERT INTO institution_moderators SELECT * FROM institutions_coordinators ON CONFLICT DO NOTHING;

DROP TABLE moderator_invitation_request;
DROP TABLE __moderators_dump;

ALTER TABLE __auth_users_dump ADD replaced_at timestamp DEFAULT null;
ALTER TABLE __comments ADD migrated_at timestamp DEFAULT null;
ALTER TABLE __requests_attachments ADD migrated_at timestamp DEFAULT null;

ALTER TABLE requests_attachments ADD institution_id varchar;
CREATE INDEX requests_attachments_institution ON requests_attachments (institution_id);