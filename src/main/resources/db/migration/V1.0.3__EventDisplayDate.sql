ALTER TABLE events ADD COLUMN displayDate DATE;

UPDATE events set displayDate = '2023-02-19';

ALTER TABLE events MODIFY displayDate DATE NOT NULL;