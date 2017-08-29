/**
 * This software was developed and / or modified by Raytheon Company,
 * pursuant to Contract DG133W-05-CQ-1067 with the US Government.
 *
 * U.S. EXPORT CONTROLLED TECHNICAL DATA
 * This software product contains export-restricted data whose
 * export/transfer/disclosure is restricted by U.S. law. Dissemination
 * to non-U.S. persons whether in the United States or abroad requires
 * an export license or other authorization.
 *
 * Contractor Name:        Raytheon Company
 * Contractor Address:     6825 Pine Street, Suite 340
 *                         Mail Stop B8
 *                         Omaha, NE 68106
 *                         402.291.0100
 *
 * See the AWIPS II Master Rights File ("Master Rights File.pdf") for
 * further licensing information.
 **/
package com.raytheon.uf.edex.datadelivery.harvester.purge;

import java.math.BigInteger;
import java.util.List;

import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import com.raytheon.uf.edex.registry.ebxml.dao.RegistryObjectTypeDao;

import oasis.names.tc.ebxml.regrep.xsd.rim.v4.RegistryObjectType;

/**
 *
 * Data Access object for interactions with DataSetMetaData
 *
 * <pre>
 *
 * SOFTWARE HISTORY
 *
 * Date          Ticket#  Engineer  Description
 * ------------- -------- --------- --------------------------------------------
 * Sep 13, 2016  5846     tjensen   Initial creation
 * Aug 29, 2017  6186     rjpeter   Fix purge query to not be a cartesian product.
 *
 * </pre>
 *
 * @author tjensen
 */
public class DataSetMetaDataDao
        extends RegistryObjectTypeDao<RegistryObjectType> {

    private static String GET_PROVIDERS = "SELECT distinct v.stringValue FROM SlotType s inner join s.slotValue v WHERE s.name = :providerSlotName and s.parent_id in "
            + "(SELECT regObj.id FROM RegistryObjectType regObj WHERE regObj.objectType=:objectType)";

    private static String GET_IDS_BEYOND_RETENTION = "SELECT o.id FROM RegistryObjectType o inner join o.slot s1 inner join s1.slotValue v1 "
            + "WHERE o.objectType=:objectType and s1.name = :dateSlotName and v1.integerValue < :retentionTime and "
            + "o.id in (select s2.parent_id FROM SlotType s2 inner join s2.slotValue v2 WHERE s2.name = :providerSlotName and v2.stringValue = :provider) "
            + "order by v2.integerValue";

    @Transactional(propagation = Propagation.REQUIRED, readOnly = true)
    public List<String> getProviderNames(String objectType,
            String providerSlotName) {
        return this.executeHQLQuery(GET_PROVIDERS, "providerSlotName",
                providerSlotName, "objectType", objectType);
    }

    @Transactional(propagation = Propagation.REQUIRED, readOnly = true)
    public List<String> getIdsBeyondRetention(String objectType,
            String providerSlotName, String provider, String dateSlotName,
            BigInteger retentionTime, int purgeBatchSize) {
        return this.executeHQLQuery(GET_IDS_BEYOND_RETENTION, purgeBatchSize,
                "objectType", objectType, "providerSlotName", providerSlotName,
                "provider", provider, "dateSlotName", dateSlotName,
                "retentionTime", retentionTime);
    }

}
