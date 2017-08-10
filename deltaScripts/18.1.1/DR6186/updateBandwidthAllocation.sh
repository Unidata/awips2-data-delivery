#/bin/sh
# DR6186 Drops removed columns from bandwidth_allocation 

psql -d metadata -U awipsadmin << EOF
ALTER TABLE bandwidth_allocation DROP COLUMN IF EXISTS agenttype;
EOF

