-- DR6415

-- On any exception, except an already existing latencyMinutes column, abort the transaction.
\set ON_ERROR_STOP 1
BEGIN TRANSACTION;

-- Add latencyMinutes if it doesn't exist
DO $$
  BEGIN
    ALTER TABLE subscription_retrieval ADD COLUMN latencyMinutes integer;
  EXCEPTION
    WHEN duplicate_column THEN RAISE NOTICE 'latencyMinutes already exists. Continuing...';
  END;
$$;

-- Add a default in existing records in prep for NOT NULL constraint
UPDATE subscription_retrieval SET latencyMinutes = 120 WHERE latencyMinutes IS NULL;

-- Set NOT NULL constraint
ALTER TABLE subscription_retrieval ALTER COLUMN latencyMinutes SET NOT NULL;

COMMIT;
