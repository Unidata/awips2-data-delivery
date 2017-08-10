#/bin/sh
# DR6186 Drops and recreates the subscription_retrieval table

psql -d metadata -U awipsadmin << EOF
drop index if exists subscription_retrieval_nextRetrieval_idx;
drop table if exists subscription_retrieval;
CREATE TABLE subscription_retrieval
(
  id integer NOT NULL,
  datasetname character varying(255) NOT NULL,
  dsmdurl character varying(255) NOT NULL,
  inserttime timestamp without time zone NOT NULL,
  latencyexpiretime timestamp without time zone NOT NULL,
  owner character varying(255) NOT NULL,
  priority integer NOT NULL,
  provider character varying(255) NOT NULL,
  retrieval bytea NOT NULL,
  state character varying(255) NOT NULL,
  subscriptionname character varying(255) NOT NULL,
  CONSTRAINT subscription_retrieval_pkey PRIMARY KEY (id)
);
CREATE INDEX "subscription_retrieval_nextRetrieval_idx"
  ON subscription_retrieval
  USING btree
  (priority, state COLLATE pg_catalog."default", id);
CREATE SEQUENCE IF NOT EXISTS subscription_retrieval_seq OWNED BY subscription_retrieval.id;
EOF

