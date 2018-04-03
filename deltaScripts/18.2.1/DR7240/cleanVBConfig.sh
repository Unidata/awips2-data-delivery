#!/bin/bash
# Cleans any existing volume browser configuration files for data delivery. 
# Configuration files for existing subscriptions can be recreated using the 
# model storage methods by deactivating and reactivating the subscription.

psql=/awips2/psql/bin/psql
sql="select v.stringvalue from ebxml.registryobject o, ebxml.slot s, ebxml.value v where o.objecttype like '%Subscription' and s.parent_id = o.id and s.name='name' and s.value_id = v.id;"
while read -r subName; do
	echo "Removing VB config files for subscription '${subName}'..."
	for commonPath in /awips2/edex/data/utility/common_static/configured/*/; do 
		vbdir="${commonPath}/menus/datadelivery/volumebrowser"

			if [[ -d "$vbdir" ]]; then
			rm -rf ${vbdir}/${subName}*
		fi

		lmdir="${commonPath}/volumebrowser/levelMappings"
		if [[ -d "$lmdir" ]]; then
			rm -rf ${lmdir}/${subName}*
		fi
	done

	for cavePath in /awips2/edex/data/utility/cave_static/configured/*/; do 
		vbsdir="${cavePath}/volumebrowser/VbSources"

		if [[ -d "$vbsdir" ]]; then
			rm -rf ${vbsdir}/${subName}*
		fi
	done
done  < <(${psql} -U awipsadmin -d metadata -Atq -c "${sql}")