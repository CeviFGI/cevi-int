-- FR-048, UC-013 BR-004: the moment an event or offer was created and its Last Change, both set by
-- the system (entity model, EVENT and VOLUNTARY_SERVICE). Stored as UTC text of fixed width
-- (yyyy-MM-ddTHH:mm:ss.SSSZ, see InstantConverter), so that ordering the text orders the moments.
--
-- SQLite only adds a NOT NULL column with a constant default, so the column starts out empty and
-- every existing row then receives the moment of this migration in both columns: entries that
-- existed before the News Feed appear in it once as new. The application always sets both values
-- itself; the empty default is never stored by it.
ALTER TABLE events ADD COLUMN created TEXT NOT NULL DEFAULT '';
ALTER TABLE events ADD COLUMN lastChange TEXT NOT NULL DEFAULT '';
UPDATE events SET created = strftime('%Y-%m-%dT%H:%M:%fZ', 'now');
UPDATE events SET lastChange = created;

ALTER TABLE voluntary_services ADD COLUMN created TEXT NOT NULL DEFAULT '';
ALTER TABLE voluntary_services ADD COLUMN lastChange TEXT NOT NULL DEFAULT '';
UPDATE voluntary_services SET created = strftime('%Y-%m-%dT%H:%M:%fZ', 'now');
UPDATE voluntary_services SET lastChange = created;
