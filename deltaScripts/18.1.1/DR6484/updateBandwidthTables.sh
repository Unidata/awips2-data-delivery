#!/bin/sh
# DR 6484 - Refactor Bandwidth Alloction tables

PSQL=/awips2/psql/bin/psql
USER=awipsadmin

# Run on the dx1
echo "Clean up replication tables and drop the ones that have changed to force them to be recreated.";
   $PSQL -d metadata -U $USER << EOF

BEGIN;
DROP TABLE IF EXISTS bandwidth_allocation CASCADE;
DROP TABLE IF EXISTS bandwidth_subscription CASCADE;
DROP TABLE IF EXISTS bandwidth_subscription_retrieval_attributes CASCADE;
CREATE TABLE bandwidth_allocation
(
  dtype character varying(31) NOT NULL,
  identifier bigint NOT NULL,
  bandwidthbucket bigint,
  basereferencetime timestamp without time zone NOT NULL,
  datasetavailablitydelay integer,
  endtime timestamp without time zone NOT NULL,
  estimatedsize bigint NOT NULL,
  network character varying(255) NOT NULL,
  priority character varying(255) NOT NULL,
  subscriptionid character varying(255) NOT NULL,
  starttime timestamp without time zone NOT NULL,
  status character varying(255),
  subname character varying(255),
  subscriptionlatency integer,
  CONSTRAINT bandwidth_allocation_pkey PRIMARY KEY (identifier)
)
WITH (
  OIDS=FALSE
);
ALTER TABLE bandwidth_allocation
  OWNER TO awipsadmin;
GRANT ALL ON TABLE bandwidth_allocation TO awipsadmin;
GRANT SELECT, UPDATE, INSERT, TRUNCATE, DELETE, TRIGGER ON TABLE bandwidth_allocation TO awips;
GRANT SELECT, UPDATE, INSERT, TRUNCATE, DELETE, TRIGGER ON TABLE bandwidth_allocation TO pguser;
COMMIT;
EOF
