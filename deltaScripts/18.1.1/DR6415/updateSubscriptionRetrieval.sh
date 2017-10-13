#! /bin/bash
# DR6415 - Update the subscription_retrieval table to add 
# a new latencyMinutes column, populate existing records 
# with a default value, and set the column as NOT NULL

psql -U awipsadmin -d metadata -f updateSubscriptionRetrieval.sql
