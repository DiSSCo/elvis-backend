CREATE TABLE IF NOT EXISTS institution_facilities
(
    id            UUID primary key,
    institutionId varchar   NOT NULL,
    moderatorId   uuid      NOT NULL,
    data          jsonb,
    created_at    timestamp NOT NULL,
    delete_date   varchar
);