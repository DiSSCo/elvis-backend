ALTER TABLE comments RENAME TO "__comments";
ALTER TABLE requests_attachments RENAME TO "__requests_attachments";

CREATE TABLE comments
(
    id         UUID primary key,
    thread_id  UUID      NOT NULL,
    author_id  UUID      NOT NULL,
    reply_to   UUID,
    format     varchar   NOT NULL,
    message    text      NOT NULL,
    created_at timestamp NOT NULL,
    deleted_at timestamp
);

ALTER TABLE comments ADD CONSTRAINT comments_author FOREIGN KEY (author_id) REFERENCES accounts;

CREATE TABLE IF NOT EXISTS requests_attachments
(
    id              UUID primary key,
    call_request_id UUID      NOT NULL,
    stored_file_id  UUID      NOT NULL,
    owner_id        UUID      NOT NULL,
    created_at      timestamp NOT NULL
);

ALTER TABLE requests_attachments ADD CONSTRAINT requests_attachments_owner FOREIGN KEY (owner_id) REFERENCES accounts;