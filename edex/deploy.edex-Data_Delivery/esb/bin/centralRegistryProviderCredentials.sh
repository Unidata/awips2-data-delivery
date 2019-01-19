#!/bin/bash
SETUP_ENV=/awips2/edex/bin/setup.env
source $SETUP_ENV
JAVA_BIN=/awips2/java/jre/bin/java

if [[ -z $EDEX_HOME ]]
then 
	EDEX_HOME=/awips2/edex
fi
	

if [[ -z $CLUSTER_ID ]] 
then
	echo "CLUSTER_ID undefined. Determining from hostname..."
	HOST=$(hostname -s)
	CLUSTER_ID=${HOST:$(expr index "$HOST" -)} | tr '[:lower:]' '[:upper:]'
fi

if [[ -z $CLUSTER_ID ]] 
then	
	echo "CLUSTER_ID could not be determined from hostname. Using site [$AW_SITE_IDENTIFIER] as CLUSTER_ID"
	CLUSTER_ID=$AW_SITE_IDENTIFIER
fi

if [[ -z $JAR_LIB ]] 
then	
	JAR_LIB="/awips2/edex/lib"
fi	

FIND_JAR_COMMAND="find $JAR_LIB -name *.jar"
JAR_FOLDERS=`$FIND_JAR_COMMAND`

#Recursively search all library directories for jar files and add them to the local classpath
addSep=false
for i in $JAR_FOLDERS;
do
	if [[ "$addSep" == true ]]; 
	then
		LOCAL_CLASSPATH=$LOCAL_CLASSPATH":"$i
	else
		LOCAL_CLASSPATH=$i
		addSep=true
	fi
done

$JAVA_BIN -cp "$LOCAL_CLASSPATH" -Daw.site.identifier=$AW_SITE_IDENTIFIER -Dedex.home=$EDEX_HOME com.raytheon.uf.edex.datadelivery.service.services.SetDataProviderDataCLI "$@"
