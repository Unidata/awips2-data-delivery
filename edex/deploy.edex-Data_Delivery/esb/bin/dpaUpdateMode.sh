#!/bin/bash
##
# This software was developed and / or modified by Raytheon Company,
# pursuant to Contract DG133W-05-CQ-1067 with the US Government.
# 
# U.S. EXPORT CONTROLLED TECHNICAL DATA
# This software product contains export-restricted data whose
# export/transfer/disclosure is restricted by U.S. law. Dissemination
# to non-U.S. persons whether in the United States or abroad requires
# an export license or other authorization.
# 
# Contractor Name:        Raytheon Company
# Contractor Address:     6825 Pine Street, Suite 340
#                         Mail Stop B8
#                         Omaha, NE 68106
#                         402.291.0100
# 
# See the AWIPS II Master Rights File ("Master Rights File.pdf") for
# further licensing information.
##

# This script will enable or disable a dataprovideragent from sending metadata updates
# to the specified central registry. This allows two dataprovideragents to run 
# concurrently and process MADIS data, with one serving as a backup and not sending 
# the data to the central registry. This script can be ran to transition a backup 
# dataprovideragent to the main agent in the event of a failover.
SETUP_ENV=/awips2/edex/bin/setup.env
source $SETUP_ENV

if [[ -z $AW_SITE_IDENTIFIER ]]
then
    echo "AW_SITE_IDENTIFIER is not defined. Please ensure [$SETUP_ENV] is properly configured." 
    exit 1;
fi
ENABLE=0
DISABLE=0
USER=`whoami`

# Common function to print usage information.
function usage() {
    echo "Usage:   ${0} [-h|--help] [-d|-e|-u USER]"
    echo "Enable or disable sending of updates to the central registry"
    echo "Options:"
    echo "    -u USER  User with permission to update localization files (Default: ${USER})"
    echo "    -e       Enable updates to central registry"
    echo "    -d       Disable updates to central registry"
    exit 1
}

# Handler arguments
while [[ $# -gt 0 ]]
do
key="$1"

case $key in
    -h|--help)
    usage
    ;;
    -e)
    ENABLE=1
    ;;
    -d)
    DISABLE=1
    ;;
    -u)
    USER=$2
    shift
    ;;
    *)
            # unknown option
    ;;
esac
shift # past argument or value
done

if [[ $DISABLE == 0 && $ENABLE == 0 ]] || [[ $DISABLE == 1 && $ENABLE == 1 ]]
then
    echo "Must give either -e or -d to specify the desired mode."
    usage
fi

if [[ $USER == "" ]]; then
    echo "Must specify a user to update the localization files."
    usage
fi

LOCALIZATION_SERVICE=http://localhost:9589/services/localization
DPA_CONFIG_NAME=dpaConfig.properties
DPA_CONFIG_PATH=datadelivery/dpa/${DPA_CONFIG_NAME}
BASE_FILE=common_static/base/${DPA_CONFIG_PATH}
SITE_FILE=common_static/site/${AW_SITE_IDENTIFIER}/${DPA_CONFIG_PATH}

# Attempt to retrieve the site level version of the file.
curl -D headers.txt -O ${LOCALIZATION_SERVICE}/${SITE_FILE} >& /dev/null

# If site level version not found, grab base version to be updated.
if grep -q "404 Not Found" headers.txt; then
   echo "No site override exists. Creating one..."
   echo "dpa.enable.central.updates=false" > ${DPA_CONFIG_NAME}
   HEADER="If-Match: NON_EXISTENT_CHECKSUM"
else
   echo "Site override found. Updating..."
   HEADER=`grep MD5 headers.txt | sed s/Content-MD5/If-Match/g | tr -d '\r'`
fi
   

# Update the property file to enable or disable central updates.
if [[ $ENABLE == 1 ]]
then
    sed -i 's/dpa.enable.central.updates=false/dpa.enable.central.updates=true/' $DPA_CONFIG_NAME
elif [[ $DISABLE == 1 ]]
then
    sed -i 's/dpa.enable.central.updates=true/dpa.enable.central.updates=false/' $DPA_CONFIG_NAME
fi

# Send the file back to localization server
curl -u ${USER}:${USER} -H "$HEADER" ${LOCALIZATION_SERVICE}/${SITE_FILE} --upload-file $DPA_CONFIG_NAME

# Cleanup files
rm headers.txt $DPA_CONFIG_NAME