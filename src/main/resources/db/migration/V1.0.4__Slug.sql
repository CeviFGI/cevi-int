-- Neue Spalte 'slug' hinzufügen
ALTER TABLE events ADD COLUMN slug TEXT;

-- 'slug' mit der ID füllen
UPDATE events SET slug = id;

-- Neue Tabelle erstellen, um slug als NOT NULL festzulegen und UNIQUE Einschränkung hinzuzufügen
CREATE TABLE events_new (
  id INTEGER PRIMARY KEY AUTOINCREMENT,
  date TEXT NOT NULL,
  description TEXT NOT NULL,
  location TEXT NOT NULL,
  title TEXT NOT NULL,
  displayDate DATE NOT NULL,
  slug TEXT NOT NULL UNIQUE
);

-- Daten aus der alten Tabelle in die neue Tabelle kopieren
INSERT INTO events_new (id, date, description, location, title, displayDate, slug)
SELECT id, date, description, location, title, displayDate, slug FROM events;

-- Alte Tabelle löschen
DROP TABLE events;

-- Neue Tabelle umbenennen
ALTER TABLE events_new RENAME TO events;
