-- Neue Spalte hinzufügen
ALTER TABLE events ADD COLUMN displayDate DATE;

-- Werte in der neuen Spalte setzen
UPDATE events SET displayDate = '2023-02-19';

-- Neue Tabelle erstellen, um displayDate als NOT NULL festzulegen
CREATE TABLE events_new (
  id INTEGER PRIMARY KEY AUTOINCREMENT,
  date TEXT NOT NULL,
  description TEXT NOT NULL,
  location TEXT NOT NULL,
  title TEXT NOT NULL,
  displayDate DATE NOT NULL
);

-- Daten aus der alten Tabelle in die neue Tabelle kopieren
INSERT INTO events_new (id, date, description, location, title, displayDate)
SELECT id, date, description, location, title, displayDate FROM events;

-- Alte Tabelle löschen
DROP TABLE events;

-- Neue Tabelle umbenennen
ALTER TABLE events_new RENAME TO events;
