CREATE TABLE IF NOT EXISTS requesters
(
    id         UUID primary key,
    first_name varchar not null,
    last_name  varchar not null,
    email      varchar not null,
    orc_id     varchar
);

CREATE UNIQUE INDEX IF NOT EXISTS requesters_email ON requesters (email);

CREATE TABLE IF NOT EXISTS auth_users
(
    id        UUID primary key,
    email     varchar   not null,
    password  varchar   not null,
    roles     varchar[] not null,
    user_data jsonb     not null
);

CREATE UNIQUE INDEX IF NOT EXISTS auth_users_email ON auth_users (email);

CREATE TABLE IF NOT EXISTS password_reset_tokens
(
    secret_token varchar primary key,
    user_id      UUID      not null,
    email        varchar   not null,
    valid_till   timestamp not null
);

CREATE TABLE IF NOT EXISTS institutions
(
    id       varchar primary key,
    cetaf    varchar NOT NULL,
    title    varchar NOT NULL,
    address  varchar NOT NULL,
    city     varchar NOT NULL,
    country  varchar NOT NULL,
    zip_code varchar NOT NULL
);

CREATE TABLE IF NOT EXISTS institutions_coordinators
(
    id             UUID primary key,
    institution_id varchar NOT NULL,
    first_name     varchar,
    last_name      varchar,
    email          varchar NOT NULL
);

CREATE TABLE IF NOT EXISTS moderators
(
    id         UUID primary key,
    first_name varchar not null,
    last_name  varchar not null,
    email      varchar not null
);

CREATE UNIQUE INDEX IF NOT EXISTS moderators_email ON moderators (email);

CREATE TABLE IF NOT EXISTS institution_moderators
(
    id             UUID primary key,
    institution_id varchar NOT NULL,
    moderator_id   UUID    NOT NULL
);

CREATE UNIQUE INDEX IF NOT EXISTS institution_moderators_im ON institution_moderators (institution_id, moderator_id);

CREATE TABLE IF NOT EXISTS moderator_invitation_request
(
    id             UUID primary key,
    email          varchar   NOT NULL,
    token          varchar   NOT NULL,
    institution_id varchar   NOT NULL,
    created_at     timestamp NOT NULL
);

CREATE UNIQUE INDEX IF NOT EXISTS moderator_invitation_request_t ON moderator_invitation_request (token);
CREATE UNIQUE INDEX IF NOT EXISTS moderator_invitation_request_e ON moderator_invitation_request (email);

CREATE TABLE IF NOT EXISTS call_request
(
    id             UUID primary key,
    call_id        UUID NOT NULL,
    created_at     timestamp,
    form_data      jsonb,
    request_title  varchar,
    request_status varchar,
    requester_id   UUID NOT NULL
);

CREATE INDEX IF NOT EXISTS call_request_r ON call_request (requester_id);

CREATE TABLE IF NOT EXISTS call_request_institutions
(
    id                UUID primary key,
    form_id           UUID    NOT NULL,
    institution_id    varchar,
    va_coordinator_id UUID,
    created_at        timestamp,
    form_data         jsonb,
    form_status       varchar NOT NULL,
    deleted           boolean NOT NULL

);

CREATE INDEX IF NOT EXISTS call_request_institutions_c ON call_request_institutions (va_coordinator_id);
CREATE INDEX IF NOT EXISTS call_request_institutions_ic ON call_request_institutions (institution_id, va_coordinator_id);
CREATE UNIQUE INDEX IF NOT EXISTS call_request_institutions_r ON call_request_institutions (form_id, institution_id);

CREATE TABLE IF NOT EXISTS security
(
    id           UUID primary key,
    auth_user_id UUID    NOT NULL,
    permission   varchar NOT NULL,
    access       varchar NOT NULL
);

CREATE UNIQUE INDEX IF NOT EXISTS security_t ON security (auth_user_id, permission, access);

CREATE TABLE IF NOT EXISTS comments_thread
(
    id         UUID primary key,
    created_at timestamp NOT NULL,
    deleted_at timestamp
);

CREATE INDEX IF NOT EXISTS comments_thread_d ON comments_thread (created_at, deleted_at);

CREATE TABLE IF NOT EXISTS comments
(
    id          UUID primary key,
    thread_id   UUID      NOT NULL,
    author_data jsonb     NOT NULL,
    reply_to    UUID,
    format      varchar   NOT NULL,
    message     text      NOT NULL,
    created_at  timestamp NOT NULL,
    deleted_at  timestamp
);

CREATE INDEX IF NOT EXISTS comments_d ON comments (created_at, deleted_at);

CREATE TABLE IF NOT EXISTS attachments
(
    id            UUID primary key,
    file_name     varchar   NOT NULL,
    extension     varchar   NOT NULL,
    mime_type     varchar   NOT NULL,
    mime_sub_type varchar   NOT NULL,
    added_at      timestamp NOT NULL
);

CREATE TABLE IF NOT EXISTS call_request_attachments
(
    id              UUID primary key,
    call_request_id UUID      NOT NULL,
    stored_file_id  UUID      NOT NULL,
    author_data     jsonb     NOT NULL,
    created_at      timestamp NOT NULL
);

CREATE UNIQUE INDEX IF NOT EXISTS call_request_attachments_a ON call_request_attachments (call_request_id, stored_file_id);
CREATE UNIQUE INDEX IF NOT EXISTS password_reset_tokens_u ON password_reset_tokens (user_id);


DO
$$
    BEGIN
        BEGIN
            ALTER TABLE institutions_coordinators
                ADD CONSTRAINT institutions_coordinators_i FOREIGN KEY (institution_id) REFERENCES institutions;
        EXCEPTION
            WHEN duplicate_object THEN RAISE NOTICE 'constraint already exists';
        END;

    END
$$;

DO
$$
    BEGIN
        BEGIN
            ALTER TABLE moderator_invitation_request
                ADD CONSTRAINT moderator_invitation_request_i FOREIGN KEY (institution_id) REFERENCES institutions;
        EXCEPTION
            WHEN duplicate_object THEN RAISE NOTICE 'constraint already exists';
        END;

    END
$$;

DO
$$
    BEGIN

        BEGIN
            ALTER TABLE call_request_institutions
                ADD CONSTRAINT call_request_institutions_fk FOREIGN KEY (form_id) references call_request ON DELETE CASCADE;
        EXCEPTION
            WHEN duplicate_object THEN RAISE NOTICE 'constraint already exists';
        END;

    END
$$;

DO
$$
    BEGIN

        BEGIN
            ALTER TABLE comments
                ADD CONSTRAINT comment_t FOREIGN KEY (thread_id) references comments_thread ON DELETE CASCADE;
        EXCEPTION
            WHEN duplicate_object THEN RAISE NOTICE 'constraint already exists';
        END;

    END
$$;

DO
$$
    BEGIN

        BEGIN
            ALTER TABLE call_request_attachments
                ADD CONSTRAINT call_request_attachments_fk FOREIGN KEY (stored_file_id) references attachments ON DELETE CASCADE;
        EXCEPTION
            WHEN duplicate_object THEN RAISE NOTICE 'constraint already exists';
        END;

    END
$$;

DO
$$
    BEGIN

        BEGIN
            ALTER TABLE call_request_attachments
                ADD CONSTRAINT call_request_fk FOREIGN KEY (call_request_id) references call_request ON DELETE CASCADE;
        EXCEPTION
            WHEN duplicate_object THEN RAISE NOTICE 'constraint already exists';
        END;

    END
$$;
