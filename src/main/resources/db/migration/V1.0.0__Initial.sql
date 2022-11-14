--
-- Tables
--
CREATE TABLE events (
  id bigint NOT NULL AUTO_INCREMENT,
  date varchar(255) DEFAULT NULL,
  description text,
  location varchar(255) DEFAULT NULL,
  title varchar(255) DEFAULT NULL,
  PRIMARY KEY(id)
);

CREATE TABLE exchanges (
  id bigint NOT NULL AUTO_INCREMENT,
  description text,
  organization varchar(255) DEFAULT NULL,
  organizationLink varchar(255) DEFAULT NULL,
  PRIMARY KEY(id)
);

CREATE TABLE voluntary_services (
  id bigint NOT NULL AUTO_INCREMENT,
  description text,
  location varchar(255) DEFAULT NULL,
  organization varchar(255) DEFAULT NULL,
  organizationLink varchar(255) DEFAULT NULL,
  PRIMARY KEY(id)
);

CREATE TABLE contact_form_entries (
  id bigint NOT NULL AUTO_INCREMENT,
  message varchar(255) DEFAULT NULL,
  PRIMARY KEY(id)
);

--
-- Indexes
--
