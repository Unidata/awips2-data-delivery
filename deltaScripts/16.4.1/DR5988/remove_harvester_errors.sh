#!/bin/bash
# DR5988 Removing Data Delivery Harvester's errors dir

cd /awips2/edex/data/utility/common_static/configured/

# Loop over any site specific directories and check to see
# if they have any error directories that need removed.
for file in */ 
do 
	if [ -d "${file}/datadelivery/harvester/errors" ]; then
		echo "Removing from $file site..."
		cd ${file}/datadelivery/harvester/
		rm -rf errors
	fi
done

echo "Removal of errors directories completed."