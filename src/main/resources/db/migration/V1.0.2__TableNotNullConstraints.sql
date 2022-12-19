ALTER TABLE events ALTER COLUMN date SET NOT NULL;
ALTER TABLE events ALTER COLUMN title SET NOT NULL;
ALTER TABLE events ALTER COLUMN description SET NOT NULL;
ALTER TABLE events ALTER COLUMN location SET NOT NULL;

ALTER TABLE voluntary_services ALTER COLUMN organization SET NOT NULL;
ALTER TABLE voluntary_services ALTER COLUMN organizationLink SET NOT NULL;
ALTER TABLE voluntary_services ALTER COLUMN description SET NOT NULL;
ALTER TABLE voluntary_services ALTER COLUMN location SET NOT NULL;

ALTER TABLE exchanges ALTER COLUMN organization SET NOT NULL;
ALTER TABLE exchanges ALTER COLUMN organizationLink SET NOT NULL;
ALTER TABLE exchanges ALTER COLUMN description SET NOT NULL;