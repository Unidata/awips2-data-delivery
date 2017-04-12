package com.raytheon.uf.common.datadelivery.registry;

import java.util.Comparator;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlSeeAlso;

import com.google.common.base.Preconditions;
import com.raytheon.uf.common.registry.annotations.RegistryObject;
import com.raytheon.uf.common.registry.annotations.RegistryObjectDescription;
import com.raytheon.uf.common.registry.annotations.RegistryObjectName;
import com.raytheon.uf.common.registry.annotations.RegistryObjectOwner;
import com.raytheon.uf.common.registry.annotations.RegistryObjectVersion;
import com.raytheon.uf.common.registry.annotations.SlotAttribute;
import com.raytheon.uf.common.registry.annotations.SlotAttributeConverter;
import com.raytheon.uf.common.registry.ebxml.slots.DateSlotConverter;
import com.raytheon.uf.common.serialization.annotations.DynamicSerializeElement;
import com.raytheon.uf.common.time.util.ImmutableDate;

/**
 * Abstract Meta Data object
 *
 * <pre>
 *
 * SOFTWARE HISTORY
 *
 * Date          Ticket#  Engineer  Description
 * ------------- -------- --------- --------------------------------------------
 * Feb 20, 2011  218      dhladky   Initial creation
 * May 15, 2012  455      jspinks   Added Registry annotations
 * Jul 24, 2012  955      djohnson  Add {@link #equals(Object)} and {@link
 *                                  #hashCode()}.
 * Aug 03, 2012  724      bphillip  Added more registry annotations
 * Aug 10, 2012  1022     djohnson  Requires provider name for {@link DataSet}.
 * Aug 15, 2012  743      djohnson  Add date attribute.
 * Sep 06, 2012  1102     djohnson  Implement comparable.
 * Oct 16, 2012  726      djohnson  Override {@link #toString()}.
 * Nov 19, 2012  1166     djohnson  Clean up JAXB representation of registry
 *                                  objects.
 * Sep 30, 2013  1797     dhladky   Made generic based on Time
 * Dec 20, 2013  2636     mpduff    Add a dataset availability offset
 * Jan 23, 2013  2584     dhladky   Versions.
 * Apr 14, 2013  3012     dhladky   Removed unused methods.
 * Jun 09, 2014  3113     mpduff    Version 1.1 - Added arrivalTime.
 * Apr 05, 2017  1045     tjensen   Add information to support moving dataset
 *
 * </pre>
 *
 * @author dhladky
 * @version 1.0
 */
@XmlAccessorType(XmlAccessType.NONE)
@XmlSeeAlso({ GriddedDataSetMetaData.class, OpenDapGriddedDataSetMetaData.class,
        PointDataSetMetaData.class, PDADataSetMetaData.class })
@RegistryObject({ "url" })
@RegistryObjectVersion(value = 1.1f)
public abstract class DataSetMetaData<T extends Time, C extends Coverage> {
    public static final String DATE_SLOT = "date";

    public static final String DATA_SET_NAME_SLOT = "dataSetName";

    public static final String PROVIDER_NAME_SLOT = "providerName";

    /**
     * Compares the two instances of {@link DataSetMetaData} by their applicable
     * date fields.
     */
    public static final Comparator<? super DataSetMetaData<?, ?>> DATE_COMPARATOR = new Comparator<DataSetMetaData<?, ?>>() {
        @Override
        public int compare(DataSetMetaData<?, ?> o1, DataSetMetaData<?, ?> o2) {

            Preconditions.checkNotNull(o1,
                    "Cannot compare this object with null!");
            Preconditions.checkNotNull(o2,
                    "Cannot compare this object with null!");

            if (o1.date != null && o2.date != null) {
                return o1.date.compareTo(o2.date);
            }

            return 0;
        }
    };

    @RegistryObjectDescription
    @XmlAttribute
    @DynamicSerializeElement
    protected String dataSetDescription;

    @RegistryObjectOwner
    @RegistryObjectName
    @XmlAttribute
    @DynamicSerializeElement
    protected String url;

    @XmlElement
    @DynamicSerializeElement
    @SlotAttribute
    @SlotAttributeConverter(TimeSlotConverter.class)
    protected T time;

    @XmlAttribute
    @SlotAttribute(DATA_SET_NAME_SLOT)
    @DynamicSerializeElement
    private String dataSetName;

    @XmlAttribute
    @SlotAttribute(PROVIDER_NAME_SLOT)
    @DynamicSerializeElement
    private String providerName;

    @XmlAttribute
    @SlotAttribute(DATE_SLOT)
    @SlotAttributeConverter(DateSlotConverter.class)
    @DynamicSerializeElement
    private ImmutableDate date;

    @XmlElement
    @DynamicSerializeElement
    protected int availabilityOffset;

    /**
     * Actual arrival time of the data.
     */
    @XmlElement
    @DynamicSerializeElement
    private long arrivalTime;

    /**
     * Coverage for this specific instance of the data. Used only for moving
     * datasets. Otherwise null.
     */
    @XmlElement(name = "instanceCoverage")
    @DynamicSerializeElement
    protected C instanceCoverage;

    public DataSetMetaData() {

    }

    public String getUrl() {
        return url;
    }

    public void setUrl(String url) {
        this.url = url;
    }

    public void setTime(T time) {
        this.time = time;
    }

    public T getTime() {
        return time;
    }

    public String getDataSetDescription() {
        return dataSetDescription;
    }

    public void setDataSetDescription(String dataSetDescription) {
        this.dataSetDescription = dataSetDescription;
    }

    /**
     * @return
     */
    public String getDataSetName() {
        return dataSetName;
    }

    /**
     * @param dataSetName
     *            the dataSetName to set
     */
    public void setDataSetName(String dataSetName) {
        this.dataSetName = dataSetName;
    }

    public String getProviderName() {
        return providerName;
    }

    public void setProviderName(String providerName) {
        this.providerName = providerName;
    }

    /**
     * Get the date this object starts on. In the gridded world, this would
     * correspond to the base reference time.
     *
     * @return
     */
    public ImmutableDate getDate() {
        return date;
    }

    /**
     * Set the date this object starts on. In the gridded world, this would
     * correspond to the base reference time.
     *
     * @param date
     *            the date
     */
    public void setDate(ImmutableDate date) {
        this.date = date;
    }

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

    public long getArrivalTime() {
        return arrivalTime;
    }

    public void setArrivalTime(long arrivalTime) {
        this.arrivalTime = arrivalTime;
    }

    public C getInstanceCoverage() {
        return instanceCoverage;
    }

    public void setInstanceCoverage(C instanceCoverage) {
        this.instanceCoverage = instanceCoverage;
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + ((url == null) ? 0 : url.hashCode());
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
        DataSetMetaData other = (DataSetMetaData) obj;
        if (url == null) {
            if (other.url != null) {
                return false;
            }
        } else if (!url.equals(other.url)) {
            return false;
        }
        return true;
    }

    @Override
    public String toString() {
        return url;
    }
}
