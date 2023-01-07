ALTER TABLE events MODIFY date varchar(255) NOT NULL;
ALTER TABLE events MODIFY description text NOT NULL;
ALTER TABLE events MODIFY location varchar(255) NOT NULL;
ALTER TABLE events MODIFY title varchar(255) NOT NULL;

ALTER TABLE exchanges MODIFY description text NOT NULL;
ALTER TABLE exchanges MODIFY organization varchar(255) NOT NULL;
ALTER TABLE exchanges MODIFY organizationLink varchar(255) NOT NULL;

ALTER TABLE voluntary_services MODIFY description text NOT NULL;
ALTER TABLE voluntary_services MODIFY location varchar(255) NOT NULL;
ALTER TABLE voluntary_services MODIFY organization varchar(255) NOT NULL;
ALTER TABLE voluntary_services MODIFY organizationLink varchar(255) NOT NULL;

ALTER TABLE contact_form_entries MODIFY message varchar(255)  NOT NULL;