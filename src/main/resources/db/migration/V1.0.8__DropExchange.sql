-- FR-029 was rejected (OQ-005): exchange offers are not built, so the table that no code ever read
-- or wrote is dropped together with its display fragment. The entity model no longer has EXCHANGE.
DROP TABLE IF EXISTS exchanges;
