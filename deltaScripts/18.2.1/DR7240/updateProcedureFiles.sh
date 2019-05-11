#!/bin/bash
# Loops over all procedure files in caveData directories and checks each one 
# for subscription names used for datasetIds. If any are found, replace them 
# with the dataset name prepended with "DD_" to match changes in RODO #7240.
#
# Should be ran on dx1.

psql=/awips2/psql/bin/psql
declare -gA subMap

# create map of subscriptions to dataset name ahead of time so we aren't constantly hitting psql
sql="select v1.stringvalue, v2.stringvalue from ebxml.registryobject o, ebxml.slot s1, ebxml.slot s2, ebxml.value v1, ebxml.value v2 where o.objecttype like '%Subscription' and s1.parent_id = o.id and s2.parent_id = o.id and s1.name='name' and s2.name='dataSetName' and s1.value_id = v1.id and s2.value_id = v2.id;"
while read -r queryResult; do
	sub=`echo $queryResult | awk -F"|" '{print $1}'`
	ds=`echo $queryResult | awk -F"|" '{print $2}'`
	if [[ $sub != "" ]]; then
		subMap[${sub}]=${ds}
	fi
done < <(${psql} -U awipsadmin -d metadata -Atq -c "${sql}")

# loop over all procedure files
for procFile in /home/*/caveData/etc/user/*/procedures/*.xml; do
echo "Processing file: {$procFile}..."

if [[ -f "$procFile" ]]; then
	# loop over all subscriptions in the file
	grep -A 1 "info.datasetId" $procFile | grep "constraintValue" | cut -d '"' -f 4 | sort -u | while read -r subName; do
		# get the dataset name for that subscription name
		dsName=${subMap[${subName}]}
		if [[ $dsName != "" ]]; then
			ddName="DD_${dsName}"
			echo "Replacing subscription '${subName}' with '${ddName}'"

			# replace all instances subscription name with "DD_" + dataSetName
			sed -i "s/${subName}/${ddName}/g" $procFile
		fi
	done
else
	echo "Invalid arguments: Must give a procedure file to process."
fi
done