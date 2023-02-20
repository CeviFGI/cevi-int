ALTER TABLE events ADD COLUMN slug varchar(255);

UPDATE events set slug = id;

ALTER TABLE events MODIFY slug varchar(255) NOT NULL;

ALTER TABLE events ADD CONSTRAINT U_SLUG UNIQUE (slug);