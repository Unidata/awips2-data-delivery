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

##
# Development only mode used to simulate the SBN for shared subscription
# development and testing without an actual SBN data feed.
##

export MAX_MEM=1536 # in Meg

if [ $HIGH_MEM == "on" ]; then
    export MAX_MEM=2560
fi

export EDEX_DEBUG_PORT=5013

export METADATA_POOL_MAX=20
export METADATA_POOL_TIMEOUT=60

export SOFT_REF_LRU_POLICY_MS_PER_MB=50