--
-- Tabellen
--
CREATE TABLE events (
  id INTEGER PRIMARY KEY AUTOINCREMENT,
  date TEXT DEFAULT NULL,
  description TEXT,
  location TEXT DEFAULT NULL,
  title TEXT DEFAULT NULL
);

CREATE TABLE exchanges (
  id INTEGER PRIMARY KEY AUTOINCREMENT,
  description TEXT,
  organization TEXT DEFAULT NULL,
  organizationLink TEXT DEFAULT NULL
);

CREATE TABLE voluntary_services (
  id INTEGER PRIMARY KEY AUTOINCREMENT,
  description TEXT,
  location TEXT DEFAULT NULL,
  organization TEXT DEFAULT NULL,
  organizationLink TEXT DEFAULT NULL
);

CREATE TABLE contact_form_entries (
  id INTEGER PRIMARY KEY AUTOINCREMENT,
  message TEXT DEFAULT NULL
);

--
-- Indizes
--
