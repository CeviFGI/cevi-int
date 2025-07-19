-- Die Tabelle events ändern
CREATE TABLE events_new (
  id INTEGER PRIMARY KEY AUTOINCREMENT,
  date TEXT NOT NULL,
  description TEXT NOT NULL,
  location TEXT NOT NULL,
  title TEXT NOT NULL
);

INSERT INTO events_new (id, date, description, location, title)
SELECT id, date, description, location, title FROM events;

DROP TABLE events;
ALTER TABLE events_new RENAME TO events;

-- Die Tabelle exchanges ändern
CREATE TABLE exchanges_new (
  id INTEGER PRIMARY KEY AUTOINCREMENT,
  description TEXT NOT NULL,
  organization TEXT NOT NULL,
  organizationLink TEXT NOT NULL
);

INSERT INTO exchanges_new (id, description, organization, organizationLink)
SELECT id, description, organization, organizationLink FROM exchanges;

DROP TABLE exchanges;
ALTER TABLE exchanges_new RENAME TO exchanges;

-- Die Tabelle voluntary_services ändern
CREATE TABLE voluntary_services_new (
  id INTEGER PRIMARY KEY AUTOINCREMENT,
  description TEXT NOT NULL,
  location TEXT NOT NULL,
  organization TEXT NOT NULL,
  organizationLink TEXT NOT NULL
);

INSERT INTO voluntary_services_new (id, description, location, organization, organizationLink)
SELECT id, description, location, organization, organizationLink FROM voluntary_services;

DROP TABLE voluntary_services;
ALTER TABLE voluntary_services_new RENAME TO voluntary_services;

-- Die Tabelle contact_form_entries ändern
CREATE TABLE contact_form_entries_new (
  id INTEGER PRIMARY KEY AUTOINCREMENT,
  message TEXT NOT NULL
);

INSERT INTO contact_form_entries_new (id, message)
SELECT id, message FROM contact_form_entries;

DROP TABLE contact_form_entries;
ALTER TABLE contact_form_entries_new RENAME TO contact_form_entries;
