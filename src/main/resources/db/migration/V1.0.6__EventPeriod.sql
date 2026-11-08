-- FR-046, UC-002 BR-012: First Day and Last Day of an event, both optional.
-- An event recorded before this revision has neither and stays valid (entity model, EVENT).
-- Stored as ISO dates (yyyy-MM-dd) like displayDate, see LocalDateConverter. That both are given
-- together and the Last Day does not lie before the First Day is enforced by the event form.
ALTER TABLE events ADD COLUMN firstDay DATE;
ALTER TABLE events ADD COLUMN lastDay DATE;
