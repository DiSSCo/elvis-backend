UPDATE __auth_users_dump
SET migrated_at = null,
    migrated_id = null,
    password    = '$2a$10$umAEmSRsk2lKY3HlUwMN.uAY5oay5Se2B0iCxlQZ5k8/RzwNjB11.'
WHERE id = 'f5668a85-f6b7-4ee3-a712-2a1ec6529927';

INSERT INTO __institutions_coordinators_dump (id, institution_id, first_name, last_name, email)
VALUES ('f5668a85-f6b7-4ee3-a712-2a1ec6529927',
        'grid.438154.f',
        'Tobias',
        'Schneck',
        'tobias.schneck@senckenberg.de')
ON CONFLICT DO NOTHING;

UPDATE __institutions_coordinators_dump
set id = (SELECT id FROM __auth_users_dump WHERE __auth_users_dump.email = 'synthesys@nhm-wien.ac.at')
where email = 'synthesys@nhm-wien.ac.at';
