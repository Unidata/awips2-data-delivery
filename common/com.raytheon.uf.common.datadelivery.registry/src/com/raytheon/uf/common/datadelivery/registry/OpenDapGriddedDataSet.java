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
package com.raytheon.uf.common.datadelivery.registry;

import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.Map;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.bind.annotation.adapters.XmlJavaTypeAdapter;

import com.raytheon.uf.common.datadelivery.registry.Provider.ServiceType;
import com.raytheon.uf.common.registry.annotations.RegistryObjectVersion;
import com.raytheon.uf.common.serialization.XmlGenericMapAdapter;
import com.raytheon.uf.common.serialization.annotations.DynamicSerialize;
import com.raytheon.uf.common.serialization.annotations.DynamicSerializeElement;

/**
 * The {@DataSet} for OpenDAP gridded products.
 *
 * <pre>
 *
 * SOFTWARE HISTORY
 *
 * Date          Ticket#  Engineer  Description
 * ------------- -------- --------- --------------------------------------------
 * Jul 31, 2012  1022     djohnson  Initial creation
 * Nov 19, 2012  1166     djohnson  Clean up JAXB representation of registry
 *                                  objects.
 * Sep 30, 2013  1797     dhladky   Generics
 * Aug 02, 2017  6186     rjpeter   Deprecated fields/methods.
 *
 * </pre>
 *
 * @author djohnson
 */
@XmlRootElement
@XmlAccessorType(XmlAccessType.NONE)
@RegistryObjectVersion(value = 1.0f)
@DynamicSerialize
public class OpenDapGriddedDataSet extends GriddedDataSet {

    public OpenDapGriddedDataSet() {

    }

    /* Remove post 18.1.1 */
    @DynamicSerializeElement
    @XmlJavaTypeAdapter(type = Map.class, value = XmlGenericMapAdapter.class)
    @Deprecated
    private Map<Integer, String> cyclesToUrls = new HashMap<>();

    /* Remove post 18.1.1 */
    @DynamicSerializeElement
    @XmlElement
    @Deprecated
    private LinkedList<Integer> cycleUpdate = new LinkedList<>();

    /**
     * @return the cyclesToUrls
     */
    @Deprecated
    public Map<Integer, String> getCyclesToUrls() {
        return cyclesToUrls;
    }

    /**
     * @param cyclesToUrls
     *            the cyclesToUrls to set
     */
    @Deprecated
    public void setCyclesToUrls(Map<Integer, String> cyclesToUrls) {
        this.cyclesToUrls = cyclesToUrls;
    }

    @Deprecated
    public void cycleUpdated(int cycle) {
        Integer asObject = Integer.valueOf(cycle);
        // Remove all occurences
        while (cycleUpdate.contains(asObject)) {
            cycleUpdate.remove(asObject);
        }
        cycleUpdate.push(asObject);
    }

    /**
     * Added only to comply with Thrift. These SHOULD NOT be called by anyone
     * except for thrift.
     *
     * @return the cycle update
     */
    @Deprecated
    public LinkedList<Integer> getCycleUpdate() {
        return cycleUpdate;
    }

    /**
     * Added only to comply with Thrift. These SHOULD NOT be called by anyone
     * except for thrift.
     *
     * @param cycleUpdate
     *            the cycleUpdate to set
     */
    @Deprecated
    public void setCycleUpdate(LinkedList<Integer> cycleUpdate) {
        // If the instance variable is not empty, then it's not thrift calling
        if (!this.cycleUpdate.isEmpty()) {
            throw new UnsupportedOperationException(
                    "Only added to comply with Thrift.  Do not call this method.");
        }

        this.cycleUpdate = cycleUpdate;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void combine(DataSet<GriddedTime, GriddedCoverage> toCombine) {
        super.combine(toCombine);

        if (toCombine instanceof OpenDapGriddedDataSet) {
            OpenDapGriddedDataSet other = (OpenDapGriddedDataSet) toCombine;

            Map<Integer, String> oldCyclesToUrls = other.getCyclesToUrls();
            Map<Integer, String> newCyclesToUrls = this.getCyclesToUrls();
            Iterator<Integer> updatedCycles = this.cycleUpdate
                    .descendingIterator();
            while (updatedCycles.hasNext()) {
                Integer cycle = updatedCycles.next();
                other.cycleUpdated(cycle);
                oldCyclesToUrls.put(cycle, newCyclesToUrls.get(cycle));
            }

            this.cycleUpdate = other.cycleUpdate;
            this.cyclesToUrls = other.cyclesToUrls;
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public ServiceType getServiceType() {
        return ServiceType.OPENDAP;
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = super.hashCode();
        result = prime * result
                + ((cycleUpdate == null) ? 0 : cycleUpdate.hashCode());
        result = prime * result
                + ((cyclesToUrls == null) ? 0 : cyclesToUrls.hashCode());
        return result;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (!super.equals(obj)) {
            return false;
        }
        if (getClass() != obj.getClass()) {
            return false;
        }
        OpenDapGriddedDataSet other = (OpenDapGriddedDataSet) obj;
        if (cycleUpdate == null) {
            if (other.cycleUpdate != null) {
                return false;
            }
        } else if (!cycleUpdate.equals(other.cycleUpdate)) {
            return false;
        }
        if (cyclesToUrls == null) {
            if (other.cyclesToUrls != null) {
                return false;
            }
        } else if (!cyclesToUrls.equals(other.cyclesToUrls)) {
            return false;
        }
        return true;
    }
}
