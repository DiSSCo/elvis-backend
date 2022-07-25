INSERT INTO settings(id, option_key, option_type, option_value)
VALUES ('ea75d761-7a6c-4c53-9d60-d3cf577d9551', 'main_page_text', 'string', 'This portal supports making application requests in Call 1 of Virtual Access (VA). Call 1 of Virtual Access will open for Proposals on 20 February. Proposers must contact relevant VA Coordinators a
s soon as possible, at the latest by 30 April. The deadline for the Call, so for completing VA requests, is Friday the 10th of July 2020 at 17:00 BST.'),
       ('c894a9c0-2d86-4da4-85ad-ca5fcb54920a', 'homepage_highlighted', 'string', 'This portal supports Transnational Access (TA) Call 3, which launches March 1 2021 and Virtual Access (VA) Call 2, which will launch later in Spring 2020. Requesters for TA must make contact with a
 Host at the organisation they would like to access. Requesters for VA must contact relevant VA Coordinator(s) prior to uploading their request.'),
       ('7fb9ccd1-3b49-4b8f-b208-358f1d07537f', 'homepage_title_1', 'string', 'Welcome'),
       ('240cce39-5edc-4ddc-91db-eba93b760bb9', 'homepage_textblock_1', 'string', 'The Virtual Access programme is a new step for the SYNTHESYS programme. It gives researchers an opportunity to propose and make the case for a collection or collection item(s) to be digitised by the holding institution for the wider benefit of the collections community.
 </br></br>
 A successful VA proposal will mean that the collection holding institutions will use SYNTHESYS+ funding to digitise the required collection items included in the proposal via their own digital workflows. There will be 2 Calls for VA across the SYNTHESYS+ programme. <a href
="https://www.synthesys.info/access/virtual-access.html" target="_blank">More information.</a>+
 </br></br> '),
       ('0e47f570-c649-4e9a-9ad8-b1be813d19bd', 'homepage_title_2', 'string', 'European loans and visits system'),
       ('6cfd701a-73c3-409b-b76d-1fcaac10aa15', 'homepage_textblock_2', 'string', 'The Virtual Access application portal is the first step (minimum viable product) towards the development of a European Loans and Visits system (ELViS), to be developed during 2020-2021. This portal
 will be further developed, first to support the peer review process for the VA call and then to transform it into ELViS that will be used as applications portal for Call 2 of Virtual Access. Besides applications for Virtual Access, ELViS will also support applications for
Transnational access (visits) and loans, and tracks their outcomes.')
ON CONFLICT (option_key) DO NOTHING;

ALTER TABLE institutions_coordinators ADD access varchar DEFAULT 'va';
UPDATE institutions_coordinators SET access = 'va';

create extension "uuid-ossp";
ALTER TABLE institutions_coordinators RENAME COLUMN id TO user_id;
ALTER TABLE institutions_coordinators ADD id uuid DEFAULT uuid_generate_v1();
UPDATE institutions_coordinators SET id = uuid_generate_v5(uuid_generate_v1(), user_id::text) WHERE true;

ALTER TABLE institutions_coordinators DROP CONSTRAINT institutions_coordinators_pk;
ALTER TABLE institutions_coordinators ADD CONSTRAINT institutions_coordinators_pk  PRIMARY KEY (id);
CREATE UNIQUE INDEX institutions_coordinators_coordinator ON institutions_coordinators (user_id, institution_id, access);
