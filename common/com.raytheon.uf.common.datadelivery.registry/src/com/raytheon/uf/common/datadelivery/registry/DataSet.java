package com.raytheon.uf.common.datadelivery.registry;

import java.util.HashMap;
import java.util.Map;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.adapters.XmlJavaTypeAdapter;

import com.raytheon.uf.common.datadelivery.registry.Provider.ServiceType;
import com.raytheon.uf.common.registry.annotations.RegistryObject;
import com.raytheon.uf.common.registry.annotations.RegistryObjectAssociation;
import com.raytheon.uf.common.registry.annotations.RegistryObjectDescription;
import com.raytheon.uf.common.registry.annotations.RegistryObjectName;
import com.raytheon.uf.common.registry.annotations.RegistryObjectOwner;
import com.raytheon.uf.common.registry.annotations.RegistryObjectVersion;
import com.raytheon.uf.common.registry.annotations.SlotAttribute;
import com.raytheon.uf.common.registry.annotations.SlotAttributeConverter;
import com.raytheon.uf.common.registry.ebxml.MapValuesResolver;
import com.raytheon.uf.common.registry.ebxml.slots.KeySetSlotConverter;
import com.raytheon.uf.common.serialization.XmlGenericMapAdapter;
import com.raytheon.uf.common.serialization.annotations.DynamicSerialize;
import com.raytheon.uf.common.serialization.annotations.DynamicSerializeElement;

/**
 * DataSet, wraps up the DataSetMetaData objects.
 *
 * <pre>
 *
 * SOFTWARE HISTORY
 *
 * Date          Ticket#  Engineer  Description
 * ------------- -------- --------- --------------------------------------------
 * Feb 28, 2011  218      dhladky   Initial creation
 * Aug 02, 2012  955      djohnson  Renamed to DataSet.
 * Aug 10, 2012  1022     djohnson  Move grid specific code to {@link
 *                                  GriddedDataSet}.
 * Aug 22, 2012  743      djohnson  Store data type as an enum.
 * Sep 07, 2012  1102     djohnson  Remove invalid {@code @XmlRootElement}.
 * Nov 19, 2012  1166     djohnson  Clean up JAXB representation of registry
 *                                  objects.
 * Dec 18, 2013  2636     mpduff    Add a data availability delay for the
 *                                  dataset.
 * Jan 23, 2013  2584     dhladky   Versions.
 * Jun 09, 2014  3113     mpduff    Version 1.1 - Add arrivalTime.
 * Nov 16, 2016  5988     tjensen   Added Parameters to equals comparison
 * Apr 05, 2017  1045     tjensen   Add information to support moving datasets
 *
 * </pre>
 *
 * @author dhladky
 * @version 1.0
 */
@XmlAccessorType(XmlAccessType.NONE)
@DynamicSerialize
@RegistryObject({ "providerName", "collectionName", "dataSetName" })
@RegistryObjectVersion(value = 1.1f)
public abstract class DataSet<T extends Time, C extends Coverage> {

    @RegistryObjectOwner
    @XmlAttribute
    @DynamicSerializeElement
    @SlotAttribute
    protected String providerName;

    @XmlAttribute
    @DynamicSerializeElement
    protected String collectionName;

    @RegistryObjectDescription
    @RegistryObjectName
    @XmlAttribute
    @DynamicSerializeElement
    @SlotAttribute
    protected String dataSetName;

    /**
     * Map of parameters and their descriptions.
     */
    @DynamicSerializeElement
    @RegistryObjectAssociation(MapValuesResolver.class)
    @SlotAttribute
    @SlotAttributeConverter(KeySetSlotConverter.class)
    @XmlJavaTypeAdapter(type = Map.class, value = XmlGenericMapAdapter.class)
    protected Map<String, Parameter> parameters = new HashMap<>();

    @XmlElement(name = "coverage")
    @DynamicSerializeElement
    protected C coverage;

    @XmlAttribute
    @DynamicSerializeElement
    @SlotAttribute
    protected DataType dataSetType;

    @XmlElement
    @DynamicSerializeElement
    @SlotAttribute
    @SlotAttributeConverter(TimeSlotConverter.class)
    protected T time;

    @XmlElement
    @DynamicSerializeElement
    protected int availabilityOffset;

    /**
     * Actual arrival time of the last data.
     */
    @XmlElement
    @DynamicSerializeElement
    protected long arrivalTime;

    @XmlElement
    @DynamicSerializeElement
    protected long estimatedSize;

    @XmlElement
    @DynamicSerializeElement
    protected boolean moving;

    public Map<String, Parameter> getParameters() {
        return parameters;
    }

    public void setParameters(Map<String, Parameter> parameters) {
        this.parameters = parameters;
    }

    public String getCollectionName() {
        return collectionName;
    }

    public void setCollectionName(String collectionName) {
        this.collectionName = collectionName;
    }

    public String getDataSetName() {
        return dataSetName;
    }

    public void setDataSetName(String dataSetName) {
        this.dataSetName = dataSetName;
    }

    public void setCoverage(C coverage) {
        this.coverage = coverage;
    }

    public C getCoverage() {
        return coverage;
    }

    public DataType getDataSetType() {
        return dataSetType;
    }

    public void setDataSetType(DataType dataSetType) {
        this.dataSetType = dataSetType;
    }

    public String getProviderName() {
        return providerName;
    }

    public void setProviderName(String providerName) {
        this.providerName = providerName;
    }

    public void setTime(T time) {
        this.time = time;
    }

    public T getTime() {
        return time;
    }

    public long getEstimatedSize() {
        return estimatedSize;
    }

    public void setEstimatedSize(long estimatedSize) {
        this.estimatedSize = estimatedSize;
    }

    /**
     * Retrieve the service type for this instance of DataSet.
     *
     * @return the serviceType
     */
    public abstract ServiceType getServiceType();

    /**
     * @return the availabilityOffset
     */
    public int getAvailabilityOffset() {
        return availabilityOffset;
    }

    /**
     * @param availabilityOffset
     *            the availabilityOffset to set
     */
    public void setAvailabilityOffset(int availabilityOffset) {
        this.availabilityOffset = availabilityOffset;
    }

    /**
     * @return the arrivalTime
     */
    public long getArrivalTime() {
        return arrivalTime;
    }

    /**
     * @param arrivalTime
     *            the arrivalTime to set
     */
    public void setArrivalTime(long arrivalTime) {
        this.arrivalTime = arrivalTime;
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result
                + ((collectionName == null) ? 0 : collectionName.hashCode());
        result = prime * result
                + ((dataSetName == null) ? 0 : dataSetName.hashCode());
        result = prime * result
                + ((dataSetType == null) ? 0 : dataSetType.hashCode());
        result = prime * result + (moving ? 1231 : 1237);
        result = prime * result
                + ((parameters == null) ? 0 : parameters.hashCode());
        result = prime * result
                + ((providerName == null) ? 0 : providerName.hashCode());
        return result;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj == null) {
            return false;
        }
        if (getClass() != obj.getClass()) {
            return false;
        }
        DataSet other = (DataSet) obj;
        if (collectionName == null) {
            if (other.collectionName != null) {
                return false;
            }
        } else if (!collectionName.equals(other.collectionName)) {
            return false;
        }
        if (dataSetName == null) {
            if (other.dataSetName != null) {
                return false;
            }
        } else if (!dataSetName.equals(other.dataSetName)) {
            return false;
        }
        if (dataSetType != other.dataSetType) {
            return false;
        }
        if (moving != other.moving) {
            return false;
        }
        if (parameters == null) {
            if (other.parameters != null) {
                return false;
            }
        } else if (!parameters.equals(other.parameters)) {
            return false;
        }
        if (providerName == null) {
            if (other.providerName != null) {
                return false;
            }
        } else if (!providerName.equals(other.providerName)) {
            return false;
        }
        return true;
    }

    /**
     * Combine the important information from another dataset into this one.
     * Used when a data set update is occuring.
     *
     * @param result
     *            the combined dataset
     */
    public void combine(DataSet<T, C> toCombine) {
        this.getParameters().putAll(toCombine.getParameters());
    }

    public boolean isMoving() {
        return moving;
    }

    public void setMoving(boolean isMoving) {
        this.moving = isMoving;
    }

    /**
     * Apply information pulled from config files relevant ot this Data Set
     * 
     * @param isMoving
     *            True if this is a moving data set
     * @param parentCov
     *            Outer bounds of where this product can be found. Only
     *            applicable to moving data.
     * @param estSizeInKB
     *            Estimated Size of per parameter of the data in KB. Only used
     *            for moving data sets.
     */
    public void applyInfoFromConfig(boolean isMoving, C parentCov,
            long estSizeInKB) {
        setMoving(isMoving);
        if (isMoving) {
            setCoverage(parentCov);
            setEstimatedSize(estSizeInKB * 1024);
        }

    }
}
