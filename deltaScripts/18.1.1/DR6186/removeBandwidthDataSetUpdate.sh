#/bin/sh
# DR6186 Drops the bandwidth dataset update table

psql -d metadata -U awipsadmin << EOF
DROP TABLE IF EXISTS bandwidth_datasetupdate;
EOF

