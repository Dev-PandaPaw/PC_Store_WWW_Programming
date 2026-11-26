-- Match ProductSpec's Integer fields to legacy text capacity columns.
-- Blank capacities mean unspecified; reject other nonnumeric values rather
-- than silently dropping or guessing their contents. Flyway runs this atomically.
-- Capacities are optional in ProductSpec (e.g. a case has no RAM capacity).
ALTER TABLE product_specs
    ALTER COLUMN ram_gb DROP NOT NULL,
    ALTER COLUMN ram_gb DROP DEFAULT,
    ALTER COLUMN storage_gb DROP NOT NULL,
    ALTER COLUMN storage_gb DROP DEFAULT;

ALTER TABLE product_specs
    ALTER COLUMN ram_gb TYPE INTEGER
        USING NULLIF(btrim(ram_gb::text), '')::integer,
    ALTER COLUMN storage_gb TYPE INTEGER
        USING NULLIF(btrim(storage_gb::text), '')::integer;
