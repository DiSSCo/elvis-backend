CREATE TABLE IF NOT EXISTS scoring_form
(
    id             UUID   primary key,
    scorer_id      UUID   NOT NULL,
    form           jsonb  NOT NULL,
    request_id     UUID   NOT NULL,
    deleted_at     timestamp  NULL
);