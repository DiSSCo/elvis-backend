CREATE TABLE IF NOT EXISTS calls
(
    id         UUID primary key,
    call_type varchar NOT NULL,
    description  varchar NOT NULL,
    title  varchar NOT NULL,
    additional_info  varchar,
    creation_date     timestamp NOT NULL,
    start_date     timestamp NOT NULL,
    end_date     timestamp
);

INSERT INTO calls (id, call_type, description, title, additional_info, creation_date, start_date, end_date) VALUES (
   '7d4cc4af-ac72-406e-b128-0edb23e48659',
   'va',
   '19 SYNTHESYS+ partners are preparing to offer Virtual Access to their collections. Researching a collection often requires being physically present in the collection itself. Virtual Access aims to remove the reliance on physical access by piloting digitisation workflows across organisations engaged in the SYNTHESYS+ Access programme, to trial a ''Digitisation on Demand'' (DoD) model.<br/>Successful Virtual Access proposals will provide funding for digitisation of one or more collections with the resulting data being made freely available for use by the scientific "community". <br/>Virtual Access is a new approach to accessing collections and Virtual Access workflows will vary from organisation to organisation. The Virtual Access process for each participating institution will be managed by a VA (Virtual Access) coordinator who can advise on feasibility,workflows, and costs. The input of the digitising institution(s) is key to the proposals. It is the responsibility of the proposer to contact the VA coordinator(s) of the institution(s) they would like to work with on their proposal.<br/><br/>Use the Create New Request button on this page to create your proposal for a Digitisation on Demand request.<br/>',
   'Digitisation on Demand',
   'https://www.synthesys.info/access/virtual-access.html',
   '2020-01-01 00:00:00.000000',
   '2020-02-01 00:00:00.000000',
   null
);
