--
-- Tables
--
CREATE TABLE events (
  id bigint NOT NULL,
  date varchar(255) DEFAULT NULL,
  description text,
  location varchar(255) DEFAULT NULL,
  title varchar(255) DEFAULT NULL
);

CREATE TABLE exchanges (
  id bigint NOT NULL,
  description text,
  organization varchar(255) DEFAULT NULL,
  organizationLink varchar(255) DEFAULT NULL
);

CREATE TABLE voluntary_services (
  id bigint NOT NULL,
  description text,
  location varchar(255) DEFAULT NULL,
  organization varchar(255) DEFAULT NULL,
  organizationLink varchar(255) DEFAULT NULL
);

CREATE TABLE contact_form_entries (
  id bigint NOT NULL,
  message varchar(255) DEFAULT NULL
);

--
-- Sequences
--
CREATE SEQUENCE hibernate_sequence;

--
-- Indexes
--
ALTER TABLE events
  ADD PRIMARY KEY (id);

ALTER TABLE exchanges
  ADD PRIMARY KEY (id);

ALTER TABLE voluntary_services
  ADD PRIMARY KEY (id);

ALTER TABLE contact_form_entries
  ADD PRIMARY KEY (id);