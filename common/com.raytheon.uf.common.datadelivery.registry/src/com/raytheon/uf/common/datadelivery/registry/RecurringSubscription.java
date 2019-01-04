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

import java.io.Serializable;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.SortedSet;
import java.util.TreeSet;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlElements;
import javax.xml.bind.annotation.XmlSeeAlso;
import javax.xml.bind.annotation.adapters.XmlJavaTypeAdapter;

import com.google.common.collect.Sets;
import com.raytheon.uf.common.datadelivery.registry.Utils.SubscriptionStatus;
import com.raytheon.uf.common.registry.annotations.SlotAttribute;
import com.raytheon.uf.common.registry.annotations.SlotAttributeConverter;
import com.raytheon.uf.common.registry.ebxml.RegistryUtil;
import com.raytheon.uf.common.registry.ebxml.slots.SetSlotConverter;
import com.raytheon.uf.common.registry.handler.RegistryHandlerException;
import com.raytheon.uf.common.serialization.XmlGenericMapAdapter;
import com.raytheon.uf.common.serialization.annotations.DynamicSerializeElement;
import com.raytheon.uf.common.status.IUFStatusHandler;
import com.raytheon.uf.common.status.UFStatus;
import com.raytheon.uf.common.status.UFStatus.Priority;
import com.raytheon.uf.common.time.util.TimeUtil;
import com.raytheon.uf.common.util.CollectionUtil;

/**
 * Base definition of a recurring subscription.
 *
 * <pre>
 *
 * SOFTWARE HISTORY
 *
 * Date          Ticket#  Engineer  Description
 * ------------- -------- --------- --------------------------------------------
 * Mar 25, 2013  1841     djohnson  Extracted from Subscription.
 * Apr 08, 2013  1826     djohnson  Remove delivery options.
 * May 15, 2013  1040     mpduff    Changed to use Set for office id.
 * May 21, 2013  2020     mpduff    Rename UserSubscription to SiteSubscription.
 * Sept 30,2013  1797     dhladky   Generics
 * Oct 23, 2013  2484     dhladky   Unique ID for subscriptions updated.
 * Oct 30, 2013  2448     dhladky   Fixed pulling data before and after
 *                                  activePeriod starting and ending.
 * Nov 14, 2013  2548     mpduff    Add a subscription type slot.
 * Jan 08, 2014  2615     bgonzale  Implement calculate start and calculate end
 *                                  methods.
 * Jan 14, 2014  2459     mpduff    Add subscription state.
 * Jan 20, 2014  2398     dhladky   Fixed rescheduling beyond active
 *                                  period/expired window.
 * Jan 24, 2014  2709     bgonzale  Fix setting of active period end.  Change
 *                                  active period checks to check day of year.
 *                                  removed now unused active period methods.
 * Jan 28, 2014  2636     mpduff    Changed to use GMT calendar.
 * Feb 12, 2014  2636     mpduff    Return new instance of calculated start and
 *                                  end.
 * Apr 02, 2014  2810     dhladky   Priority sorting of subscriptions.
 * May 20, 2014  3113     mpduff    Add the functionality that the subscription
 *                                  itself provides the retrieval times.
 * Jul 28, 2014  2765     dhladky   No setOwner() in the setup super() method.
 * Aug 29, 2014  3446     bphillip  SubscriptionUtil is now a singleton
 * Sep 05, 2014  2131     dhladky   Added PDA data types
 * Sep 14, 2014  2131     dhladky   PDA updates
 * Nov 19, 2014  3852     dhladky   Resurrected the Unscheduled state.
 * Feb 02, 2015  4014     dhladky   More consolidated subscription time checks.
 * Mar 23, 2015  3950     dhladky   Reworked the isbeforeStart() to not have
 *                                  gaps and take into account latency and cycle
 *                                  offsets
 * May 27, 2015  4531     dhladky   Remove excessive Calendar references.
 * Aug 02, 2017  6186     rjpeter   Removed url.
 * Sep 12, 2017  6413     tjensen   Updated to support ParameterGroups
 * Dec 07, 2017  6355     nabowle   Add vertical slot.
 * Jan 03, 2019  7503     troberts  Remove subscription grouping capabilities.
 *
 * </pre>
 *
 * @author djohnson
 */
@XmlAccessorType(XmlAccessType.NONE)
@XmlSeeAlso({ PendingSiteSubscription.class, PendingSharedSubscription.class,
        AdhocSubscription.class, SiteSubscription.class,
        SharedSubscription.class })
public abstract class RecurringSubscription<T extends Time, C extends Coverage>
        implements Serializable, Subscription<T, C>,
        Comparable<Subscription<T, C>> {

    private static final long serialVersionUID = -6422673887457060034L;

    protected static final IUFStatusHandler statusHandler = UFStatus
            .getHandler(RecurringSubscription.class);

    /**
     * Constructor.
     */
    public RecurringSubscription() {

    }

    /**
     * Initialization constructor.
     *
     * @param sub
     *            Subscription object
     * @param name
     *            New subscription name
     */
    public RecurringSubscription(Subscription<T, C> sub, String name) {
        this.setActivePeriodEnd(sub.getActivePeriodEnd());
        this.setActivePeriodStart(sub.getActivePeriodStart());
        this.setCoverage(sub.getCoverage());
        this.setDataSetName(sub.getDataSetName());
        this.setDataSetSize(sub.getDataSetSize());
        this.setDescription(sub.getDescription());
        this.setFullDataSet(sub.isFullDataSet());
        this.setId(sub.getId());
        this.setName(name);
        this.setOwner(sub.getOwner());
        this.setOfficeIDs(sub.getOfficeIDs());
        this.setParameter(sub.getParameter());
        this.setParameterGroups(sub.getParameterGroups());
        this.setPriority(sub.getPriority());
        this.setProvider(sub.getProvider());
        this.setSubscriptionEnd(sub.getSubscriptionEnd());
        this.setSubscriptionId(sub.getSubscriptionId());
        this.setSubscriptionStart(sub.getSubscriptionStart());
        this.setTime(sub.getTime());
        this.setDataSetType(sub.getDataSetType());
        this.setRoute(sub.getRoute());
        this.setLatencyInMinutes(sub.getLatencyInMinutes());
        this.setEnsemble(sub.getEnsemble());
        this.setOriginatingSite(sub.getOriginatingSite());
        this.setSubscriptionType(sub.getSubscriptionType());
        this.setVertical(sub.isVertical());

        // Set the registry id
        this.setId(RegistryUtil.getRegistryObjectKey(this));
    }

    /**
     * Copy constructor.
     *
     * @param sub
     *            Subscription object
     */
    public RecurringSubscription(Subscription<T, C> sub) {
        this(sub, sub.getName());
    }

    @XmlAttribute
    @DynamicSerializeElement
    private String subscriptionId;

    @XmlAttribute
    @DynamicSerializeElement
    @SlotAttribute(NAME_SLOT)
    private String name;

    @XmlAttribute
    @DynamicSerializeElement
    @SlotAttribute(PROVIDER_NAME_SLOT)
    private String provider;

    @XmlElements({ @XmlElement(name = "officeId") })
    @DynamicSerializeElement
    @SlotAttribute
    @SlotAttributeConverter(SetSlotConverter.class)
    protected Set<String> officeIDs = Sets.newTreeSet();

    @XmlAttribute
    @DynamicSerializeElement
    private SubscriptionPriority priority = SubscriptionPriority.NORMAL;

    @XmlAttribute
    @DynamicSerializeElement
    private Date subscriptionStart;

    @XmlAttribute
    @DynamicSerializeElement
    private Date subscriptionEnd;

    @XmlAttribute
    @DynamicSerializeElement
    private Date activePeriodStart;

    @XmlAttribute
    @DynamicSerializeElement
    private Date activePeriodEnd;

    @XmlAttribute
    @DynamicSerializeElement
    @SlotAttribute
    private boolean fullDataSet;

    @XmlAttribute
    @DynamicSerializeElement
    private long dataSetSize;

    @XmlElement(name = "coverage")
    @DynamicSerializeElement
    private C coverage;

    @XmlElement
    @DynamicSerializeElement
    @SlotAttribute
    @SlotAttributeConverter(TimeSlotConverter.class)
    private T time;

    @XmlAttribute
    @DynamicSerializeElement
    private String description;

    @XmlAttribute
    @DynamicSerializeElement
    @SlotAttribute(Subscription.DATA_SET_SLOT)
    private String dataSetName;

    @XmlAttribute
    @DynamicSerializeElement
    @SlotAttribute
    private boolean valid = true;

    @XmlAttribute
    @DynamicSerializeElement
    private boolean unscheduled;

    @XmlAttribute
    @DynamicSerializeElement
    @SlotAttribute
    private DataType dataSetType;

    /**
     * Deprecated.
     *
     * Needed for compatibility with sites using versions older than 18.1.1
     */
    @XmlElements({ @XmlElement })
    @DynamicSerializeElement
    @Deprecated
    private List<Parameter> parameter;

    @DynamicSerializeElement
    @XmlJavaTypeAdapter(type = Map.class, value = XmlGenericMapAdapter.class)
    private Map<String, ParameterGroup> parameterGroups;

    @XmlElement
    @DynamicSerializeElement
    private Ensemble ensemble;

    @XmlAttribute
    @DynamicSerializeElement
    private boolean deleted;

    @XmlAttribute
    @DynamicSerializeElement
    private String id;

    @XmlAttribute
    @DynamicSerializeElement
    @SlotAttribute(Subscription.ROUTE_SLOT)
    private Network route = Network.OPSNET;

    @XmlAttribute
    @DynamicSerializeElement
    private int latencyInMinutes;

    @XmlAttribute
    @DynamicSerializeElement
    @SlotAttribute(Subscription.ORIGINATING_SITE_SLOT)
    private String originatingSite;

    @XmlAttribute
    @DynamicSerializeElement
    @SlotAttribute(Subscription.SUBSCRIPTION_TYPE_SLOT)
    private SubscriptionType subscriptionType;

    @XmlAttribute
    @DynamicSerializeElement
    @SlotAttribute(Subscription.SUBSCRIPTION_STATE_SLOT)
    private SubscriptionState subscriptionState = SubscriptionState.ON;

    @XmlAttribute
    @DynamicSerializeElement
    private boolean vertical = false;

    /*
     * Active Period starting day of the year. Calculated from
     * activePeriodStart.
     */
    private Integer startActivePeriodDayOfYear;

    /*
     * Active Period ending day of the year. Calculated from activePeriodEnd.
     */
    private Integer endActivePeriodDayOfYear;

    /** Flag stating if the object should be updated */
    private boolean shouldUpdate = false;

    /**
     * Get subscription name.
     *
     * @return subscription name
     */
    @Override
    public String getName() {
        return name;
    }

    /**
     * Set subscription name.
     *
     * @param name
     *            the name of the subscription
     */
    @Override
    public void setName(String name) {
        this.name = name;
    }

    /**
     * Set subscription provider name.
     *
     * @param provider
     *            the name of the subscription provider
     */
    @Override
    public void setProvider(String provider) {
        this.provider = provider;
    }

    /**
     * Get provider name.
     *
     * @return provider name
     */
    @Override
    public String getProvider() {
        return provider;
    }

    @Override
    public Set<String> getOfficeIDs() {
        return officeIDs;
    }

    @Override
    public void setOfficeIDs(Set<String> officeIDs) {
        this.officeIDs = new TreeSet<>(officeIDs);
    }

    /**
     * Get subscription priority for fulfillment.
     *
     * @return subscription name
     */
    @Override
    public SubscriptionPriority getPriority() {
        return priority;
    }

    /**
     * Set subscription priority.
     *
     * @param priority
     *            priority
     */
    @Override
    public void setPriority(SubscriptionPriority priority) {
        this.priority = priority;
    }

    /**
     * Get subscription start time.
     *
     * @return subscription start
     */
    @Override
    public Date getSubscriptionStart() {
        return subscriptionStart;
    }

    /**
     * Set subscription start time.
     *
     * @param subscriptionStart
     *            date time group for subscription start
     */
    @Override
    public void setSubscriptionStart(Date subscriptionStart) {
        this.subscriptionStart = subscriptionStart;
    }

    /**
     * Get subscription end time.
     *
     * @return subscription end time date time group for subscription end
     */
    @Override
    public Date getSubscriptionEnd() {
        return subscriptionEnd;
    }

    /**
     * Set subscription end time.
     *
     * @param subscriptionEnd
     *            date time group for subscription end
     */
    @Override
    public void setSubscriptionEnd(Date subscriptionEnd) {
        this.subscriptionEnd = subscriptionEnd;
    }

    /**
     * Get active period start date.
     *
     * @return activePeriodStart
     */
    @Override
    public Date getActivePeriodStart() {
        return activePeriodStart;
    }

    /**
     * Set active period start date.
     *
     * @param activePeriodStart
     *            date for subscription start
     */
    @Override
    public void setActivePeriodStart(Date activePeriodStart) {
        this.activePeriodStart = activePeriodStart;
        this.startActivePeriodDayOfYear = null;
    }

    /**
     * Get active period end date.
     *
     * @return activePeriodEnd
     */
    @Override
    public Date getActivePeriodEnd() {
        return activePeriodEnd;
    }

    /**
     * Set active period end date.
     *
     * @param activePeriodEnd
     *            date for subscription end
     */
    @Override
    public void setActivePeriodEnd(Date activePeriodEnd) {
        this.activePeriodEnd = activePeriodEnd;
        this.endActivePeriodDayOfYear = null;
    }

    private Integer getStartActivePeriodDayOfYear() {
        if (startActivePeriodDayOfYear == null && activePeriodStart != null) {
            startActivePeriodDayOfYear = TimeUtil
                    .newGmtCalendar(activePeriodStart)
                    .get(Calendar.DAY_OF_YEAR);
        }
        return startActivePeriodDayOfYear;
    }

    private Integer getEndActivePeriodDayOfYear() {
        if (endActivePeriodDayOfYear == null && activePeriodEnd != null) {
            endActivePeriodDayOfYear = TimeUtil.newGmtCalendar(activePeriodEnd)
                    .get(Calendar.DAY_OF_YEAR);
        }
        return endActivePeriodDayOfYear;
    }

    @Override
    public Date calculateStart(Date startConstraint) {
        if (subscriptionStart == null) {
            return startConstraint;
        }

        long subStartMillis = subscriptionStart.getTime();
        long constaintMillis = startConstraint.getTime();

        if (subStartMillis > constaintMillis) {
            return subscriptionStart;
        }

        return startConstraint;
    }

    @Override
    public Date calculateEnd(Date endConstraint) {
        if (subscriptionEnd == null) {
            return endConstraint;
        }

        long subEndMillis = subscriptionEnd.getTime();
        long constaintMillis = endConstraint.getTime();

        if (subEndMillis < constaintMillis) {
            return subscriptionEnd;
        }

        return endConstraint;
    }

    /**
     * isNotify flag for subscription.
     *
     * @return boolean true if full dataset
     */
    @Override
    public boolean isFullDataSet() {
        return fullDataSet;
    }

    /**
     * Set fullDataSet flag.
     *
     * @param fullDataSet
     *            true if full dataset
     */
    @Override
    public void setFullDataSet(boolean fullDataSet) {
        this.fullDataSet = fullDataSet;
    }

    /**
     * Get size of the dataset for the subscription.
     *
     * @return dataSetSize size of dataset
     */
    @Override
    public long getDataSetSize() {
        return dataSetSize;
    }

    /**
     * Set the dataset size for the subscription.
     *
     * @param dataSetSize
     *            size of dataset
     */
    @Override
    public void setDataSetSize(long dataSetSize) {
        this.dataSetSize = dataSetSize;
    }

    /**
     * Get subscription coverage area.
     *
     * @return coverage
     */
    @Override
    public C getCoverage() {
        return coverage;
    }

    /**
     * Set the coverage area for the subscription.
     *
     * @param coverage
     *            coverage area
     */
    @Override
    public void setCoverage(C coverage) {
        this.coverage = coverage;
    }

    /**
     * Get subscription submission time.
     *
     * @return subscription time
     */
    @Override
    public T getTime() {
        return time;
    }

    /**
     * Set the subscription submission time.
     *
     * @param time
     *            time stamp
     */
    @Override
    public void setTime(T time) {
        this.time = time;
    }

    /**
     * Set the subscription parameters.
     *
     * @param parameter
     *            subscription parameter list
     */
    @Override
    @Deprecated
    public void setParameter(List<Parameter> parameter) {
        this.parameter = parameter;
    }

    /**
     * Get subscription parameter list.
     *
     * @return subscription parameter list
     */
    @Override
    @Deprecated
    public List<Parameter> getParameter() {
        return parameter;
    }

    @Override
    public Map<String, ParameterGroup> getParameterGroups() {
        /*
         * Subscriptions generated pre-18.1.1 will not have parameterGroup
         * populated, so we need to generate it from the parameter list.
         *
         * TODO: After all sites are at 18.1.1 or beyond, this should be
         * removed.
         */
        if (parameterGroups == null) {
            parameterGroups = ParameterUtils
                    .generateParameterGroupsFromParameters(parameter);
        }
        return parameterGroups;
    }

    @Override
    public void setParameterGroups(
            Map<String, ParameterGroup> parameterGroups) {
        this.parameterGroups = parameterGroups;
    }

    /**
     * Add subscription id.
     *
     * @param subscriptionId
     *            a subscription id
     */
    @Override
    public void setSubscriptionId(String subscriptionId) {
        this.subscriptionId = subscriptionId;
    }

    /**
     * Get subscription id.
     *
     * @return subscription id
     */
    @Override
    public String getSubscriptionId() {
        return subscriptionId;
    }

    /**
     * Get subscription description.
     *
     * @return subscription description
     */
    @Override
    public String getDescription() {
        return description;
    }

    /**
     * Set the subscription description.
     *
     * @param description
     *            subscription description
     */
    @Override
    public void setDescription(String description) {
        this.description = description;
    }

    /**
     * Get subscription dataset name.
     *
     * @return subscription dataset name
     */
    @Override
    public String getDataSetName() {
        return dataSetName;
    }

    /**
     * Set the subscription dataSetName.
     *
     * @param dataSetName
     *            subscription dataSetName
     */
    @Override
    public void setDataSetName(String dataSetName) {
        this.dataSetName = dataSetName;
    }

    /**
     * isActive flag for subscription status.
     *
     * @return boolean true if subscription is Active
     */
    @Override
    public boolean isActive() {
        return getStatus() == SubscriptionStatus.ACTIVE;
    }

    /**
     * Set subscription valid.
     *
     * @param valid
     *            true if subscription valid
     */
    @Override
    public void setValid(boolean valid) {
        this.valid = valid;
        if (!valid) {
            subscriptionState = SubscriptionState.OFF;
        }
    }

    /**
     * Return if subscription is valid or invalid
     *
     * @return true if subscription is valid
     */
    @Override
    public boolean isValid() {
        return valid;
    }

    /**
     * Get subscription dataset type.
     *
     * @return subscription dataset type
     */
    @Override
    public DataType getDataSetType() {
        return dataSetType;
    }

    /**
     * Set the dataset type
     *
     * @param dataSetType
     *            the dataSetType to set
     */
    @Override
    public void setDataSetType(DataType dataSetType) {
        this.dataSetType = dataSetType;
    }

    /**
     * isDeleted flag.
     *
     * @return true if the subscription has been deleted
     */
    @Override
    public boolean isDeleted() {
        return deleted;
    }

    /**
     * Set the deleted flag.
     *
     * @param deleted
     *            set subscription to deleted
     */
    @Override
    public void setDeleted(boolean deleted) {
        this.deleted = deleted;
    }

    /**
     * @return the unscheduled
     */
    @Override
    public boolean isUnscheduled() {
        return unscheduled;
    }

    /**
     * @param unscheduled
     *            the unscheduled to set
     */
    @Override
    public void setUnscheduled(boolean unscheduled) {
        this.unscheduled = unscheduled;
    }

    /**
     * Get subscription id.
     *
     * @return subscription id
     */
    @Override
    public String getId() {
        return id;
    }

    /**
     * Set the subscription id.
     *
     * @param id
     *            set subscription id
     */
    @Override
    public void setId(String id) {
        this.id = id;
    }

    @Override
    public boolean isVertical() {
        return vertical;
    }

    @Override
    public void setVertical(boolean isVertical) {
        this.vertical = isVertical;
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

        RecurringSubscription other = (RecurringSubscription) obj;
        if (dataSetName == null) {
            if (other.dataSetName != null) {
                return false;
            }
        } else if (!dataSetName.equals(other.dataSetName)) {
            return false;
        }
        if (name == null) {
            if (other.name != null) {
                return false;
            }
        } else if (!name.equals(other.name)) {
            return false;
        }
        if (originatingSite == null) {
            if (other.originatingSite != null) {
                return false;
            }
        } else if (!originatingSite.equals(other.originatingSite)) {
            return false;
        }
        if (provider == null) {
            if (other.provider != null) {
                return false;
            }
        } else if (!provider.equals(other.provider)) {
            return false;
        }

        if (getOwner() == null) {
            if (other.getOwner() != null) {
                return false;
            }
        } else if (!getOwner().equals(other.getOwner())) {
            return false;
        }

        return true;
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result
                + ((dataSetName == null) ? 0 : dataSetName.hashCode());
        result = prime * result + ((name == null) ? 0 : name.hashCode());
        result = prime * result
                + ((originatingSite == null) ? 0 : originatingSite.hashCode());
        result = prime * result
                + ((provider == null) ? 0 : provider.hashCode());
        result = prime * result
                + ((getOwner() == null) ? 0 : getOwner().hashCode());
        return result;
    }

    @Override
    public String toString() {
        SubscriptionType subType = getSubscriptionType();
        StringBuilder sb = new StringBuilder(getName());
        sb.append("::");
        sb.append(getProvider());
        sb.append("::");
        sb.append(getDataSetName());
        sb.append("::");
        sb.append(getOwner());
        sb.append("::");
        sb.append(getOriginatingSite());
        sb.append("::");
        sb.append(subType == null ? "null" : subType.name());
        return sb.toString();
    }

    /**
     * Determine if subscription status is expired and set subscription to off
     * if it is expired.
     *
     * @return true if status is expired
     */
    private boolean checkAndSetExpiration() {
        Calendar cal = TimeUtil.newGmtCalendar();
        boolean expired = false;
        if (subscriptionEnd != null && cal.getTime().after(subscriptionEnd)) {
            expired = true;
            this.subscriptionState = SubscriptionState.OFF;
            this.shouldUpdate = true;
        }

        return expired;
    }

    /**
     * Check for expiration on date
     *
     * @param date
     * @return
     */
    private boolean isExpired(Date date) {
        boolean expired = false;
        if (subscriptionEnd != null && date.after(subscriptionEnd)) {
            expired = true;
        }
        return expired;
    }

    /**
     * Check for before start date
     *
     * @param date
     * @return
     */
    private boolean isBeforeStart(Date date) {
        boolean before = false;

        long latency = this.getLatencyInMinutes() * TimeUtil.MILLIS_PER_MINUTE;
        if (getSubscriptionStart() == null) {
            /*
             * If subscription has no registered start time, It can't be before
             * checked time.
             */
            return before;
        }
        long startTime = getSubscriptionStart().getTime();

        long offset = 0l;

        switch (dataSetType) {

        // special case of grids
        case GRID:
            // get the step time in milliseconds.
            offset = ((GriddedTime) getTime()).findForecastStepUnit()
                    * TimeUtil.MILLIS_PER_SECOND;
            break;

        case POINT:
            // point uses an interval (in Minutes)
            offset = ((PointTime) getTime()).getInterval()
                    * TimeUtil.MILLIS_PER_MINUTE;
            break;

        default:
            // no offset value set
            break;
        }

        if ((startTime - offset - latency) > date.getTime()) {
            before = true;
        }

        return before;
    }

    /**
     * Get the current subscription status.
     *
     * @return SUBSCRIPTION_STATUS
     */
    @Override
    public SubscriptionStatus getStatus() {
        if (!isValid()) {
            return SubscriptionStatus.INVALID;
        } else if (checkAndSetExpiration()) {
            return SubscriptionStatus.EXPIRED;
        } else if (subscriptionState == SubscriptionState.OFF) {
            return SubscriptionStatus.DEACTIVATED;
        } else if (isUnscheduled()) {
            return SubscriptionStatus.UNSCHEDULED;
        }

        // At this point the subscription is in the ON state
        if (inActivePeriodWindow(TimeUtil.newDate())) {
            return SubscriptionStatus.ACTIVE;
        }

        return SubscriptionStatus.INACTIVE;
    }

    /**
     * Return true if this subscription should be scheduled. Scheduling is based
     * on the status of the subscription. Returns false if the subscription is
     * expired or deactivated.
     *
     * @return true if this subscription should be scheduled
     */
    public boolean shouldSchedule() {
        return subscriptionState == SubscriptionState.ON
                && !checkAndSetExpiration() && isActive();
    }

    /**
     * Should this be scheduled for this time.
     *
     * @param checkDate
     * @return
     */
    @Override
    public boolean shouldScheduleForTime(Date checkCal) {
        if (!isExpired(checkCal) && !isBeforeStart(checkCal)
                && inActivePeriodWindow(checkCal)) {
            return true;
        }

        return false;
    }

    @Override
    public boolean inActivePeriodWindow(Date checkDate) {
        if (activePeriodStart == null && activePeriodEnd == null) {
            // no active period set
            return true;
        } else {
            Calendar cal = TimeUtil.newGmtCalendar(checkDate);
            Integer startDay = getStartActivePeriodDayOfYear();
            Integer endDay = getEndActivePeriodDayOfYear();
            int checkDay = cal.get(Calendar.DAY_OF_YEAR);

            boolean isAfterPeriodStart = startDay <= checkDay;
            boolean isBeforePeriodEnd = checkDay < endDay;
            boolean periodCrossesYearBoundary = endDay < startDay;

            if (periodCrossesYearBoundary) {
                return isAfterPeriodStart || isBeforePeriodEnd;
            } else {
                return isAfterPeriodStart && isBeforePeriodEnd;
            }
        }
    }

    @Override
    public Network getRoute() {
        return this.route;
    }

    @Override
    public void setRoute(Network route) {
        this.route = route;
    }

    /**
     * Set the latency in minutes.
     *
     * @param latencyInMinutes
     *            the latency, in minutes
     *
     */
    @Override
    public void setLatencyInMinutes(int latencyInMinutes) {
        this.latencyInMinutes = latencyInMinutes;
    }

    /**
     * Get the latency, in minutes.
     *
     * @return the latency in minutes
     */
    @Override
    public int getLatencyInMinutes() {
        return latencyInMinutes;
    }

    @Override
    public Ensemble getEnsemble() {
        return ensemble;
    }

    @Override
    public void setEnsemble(Ensemble ensemble) {
        this.ensemble = ensemble;
    }

    @Override
    public void setOriginatingSite(String originatingSite) {
        this.originatingSite = originatingSite;
    }

    @Override
    public String getOriginatingSite() {
        return originatingSite;
    }

    /**
     * @return the subscriptionType
     */
    @Override
    public SubscriptionType getSubscriptionType() {
        return subscriptionType;
    }

    /**
     * @param subscriptionType
     *            the subscriptionType to set
     */
    @Override
    public void setSubscriptionType(SubscriptionType subscriptionType) {
        this.subscriptionType = subscriptionType;
    }

    /**
     * @return the subscriptionState
     */
    @Override
    public SubscriptionState getSubscriptionState() {
        return subscriptionState;
    }

    /**
     * @param subscriptionState
     *            the subscriptionState to set
     */
    @Override
    public void setSubscriptionState(SubscriptionState subscriptionState) {
        this.subscriptionState = subscriptionState;
    }

    @Override
    public void activate() {
        if (valid && !checkAndSetExpiration()) {
            this.setSubscriptionState(SubscriptionState.ON);
        }
    }

    @Override
    public void deactivate() {
        this.setSubscriptionState(SubscriptionState.OFF);
    }

    /**
     * @return the shouldUpdate
     */
    public boolean shouldUpdate() {
        return shouldUpdate;
    }

    @Override
    public int compareTo(Subscription<T, C> o) {

        SubscriptionPriority oPriority = o.getPriority();
        SubscriptionPriority myPriority = this.getPriority();

        return myPriority.compareTo(oPriority);
    }

    @SuppressWarnings("rawtypes")
    @Override
    public SortedSet<Date> getRetrievalTimes(Date planStart, Date planEnd,
            List<DataSetMetaData> dsmdList, SubscriptionUtil subUtil) {
        SortedSet<Date> retrievalTimes = null;
        switch (dataSetType) {
        case GRID:
            List<Integer> cycles = ((GriddedTime) getTime()).getCycleTimes();
            final boolean subscribedToCycles = !CollectionUtil
                    .isNullOrEmpty(cycles);

            if (subscribedToCycles) {
                retrievalTimes = getTimes(Sets.newTreeSet(cycles), planStart,
                        planEnd);
            } else {
                int interval = subUtil.calculateInterval(dsmdList);
                Date lastArrivalTime = subUtil.getLatestArrivalTime(dsmdList);
                Calendar lastArrival = TimeUtil.newGmtCalendar(lastArrivalTime);
                if (lastArrival == null) {
                    return new TreeSet<>();
                }
                while (lastArrival.before(planStart)) {
                    lastArrival.add(Calendar.MINUTE, interval);
                }
                retrievalTimes = getTimes(interval, lastArrival.getTime(),
                        planEnd);
            }
            break;
        case POINT:
            int interval = this.getLatencyInMinutes();
            retrievalTimes = getTimes(interval, planStart, planEnd);
            break;
        case PDA:
            int pdainterval = subUtil.calculateInterval(dsmdList);
            Date lastArrivalTime = subUtil.getLatestArrivalTime(dsmdList);
            if (lastArrivalTime == null) {
                return new TreeSet<>();
            }
            retrievalTimes = getTimes(pdainterval, lastArrivalTime, planEnd);
            break;
        default:
            throw new IllegalArgumentException(
                    "The BandwidthManager doesn't know how to treat subscriptions with data type ["
                            + dataSetType + "]!");
        }

        return retrievalTimes;
    }

    /**
     * Get the times for the specified time range and interval
     *
     * @param interval
     *            The interval
     * @param planStart
     *            The start
     * @param planEnd
     *            The end
     * @return sorted set of calendar objects
     */
    private SortedSet<Date> getTimes(int interval, Date planStart,
            Date planEnd) {
        SortedSet<Date> subscriptionTimes = new TreeSet<>();

        if (interval == SubscriptionUtil.MISSING || interval <= 0) {
            return subscriptionTimes;
        }
        /*
         * starting time when subscription is first valid for scheduling based
         * on plan start and subscription start.
         */
        Calendar subscriptionCalculatedStart = TimeUtil
                .newGmtCalendar(calculateStart(planStart));

        /*
         * end time when when subscription is last valid for scheduling based on
         * plan end and subscription end.
         */
        Calendar subscriptionCalculatedEnd = TimeUtil
                .newGmtCalendar(calculateEnd(planEnd));

        subscriptionCalculatedStart = TimeUtil.minCalendarFields(
                subscriptionCalculatedStart, Calendar.MINUTE, Calendar.SECOND,
                Calendar.MILLISECOND);

        Calendar start = TimeUtil
                .newGmtCalendar(subscriptionCalculatedStart.getTime());
        start.add(Calendar.MINUTE, interval * -1);
        while (!start.after(subscriptionCalculatedEnd)) {
            Date baseRefTime = start.getTime();
            baseRefTime.setTime(start.getTimeInMillis());
            if (baseRefTime.after(planStart) && baseRefTime.before(planEnd)) {
                /*
                 * Fine grain check by hour and minute, for
                 * subscription(start/end), activePeriod(start/end)
                 */

                if (shouldScheduleForTime(baseRefTime)) {
                    subscriptionTimes.add(baseRefTime);
                }
            }
            start.add(Calendar.MINUTE, interval);
        }

        return subscriptionTimes;
    }

    /**
     * Get the times for the specified time range and cycles
     *
     * @param cycles
     *            Cycles to consider
     * @param planStart
     *            start time
     * @param planEnd
     *            end time
     * @return sorted set of calendar objects
     */
    private SortedSet<Date> getTimes(TreeSet<Integer> cycles, Date planStart,
            Date planEnd) {

        SortedSet<Date> subscriptionTimes = new TreeSet<>();
        /* calendar used in these calcs with grid */
        Calendar cplanStart = TimeUtil.newGmtCalendar(planStart);
        Calendar cplanEnd = TimeUtil.newGmtCalendar(planEnd);
        /*
         * starting time when subscription is first valid for scheduling based
         * on plan start and subscription start.
         */
        Calendar subscriptionCalculatedStart = TimeUtil
                .newGmtCalendar(calculateStart(planStart));

        /*
         * end time when when subscription is last valid for scheduling based on
         * plan end and subscription end.
         */
        Calendar subscriptionCalculatedEnd = TimeUtil
                .newGmtCalendar(calculateEnd(planEnd));

        subscriptionCalculatedStart = TimeUtil.minCalendarFields(
                subscriptionCalculatedStart, Calendar.MINUTE, Calendar.SECOND,
                Calendar.MILLISECOND);

        // drop the start time by 6 hours to account for 4 cycle/day models
        subscriptionCalculatedStart.add(Calendar.HOUR_OF_DAY, -6);
        Calendar start = subscriptionCalculatedStart;

        while (!start.after(subscriptionCalculatedEnd)) {
            for (Integer cycle : cycles) {
                start.set(Calendar.HOUR_OF_DAY, cycle);

                // calculate the offset, every hour
                int availabilityOffset = 0;
                try {
                    availabilityOffset = SubscriptionUtil.getInstance()
                            .getDataSetAvailablityOffset(this, start.getTime());
                } catch (RegistryHandlerException e) {
                    // Error occurred querying the registry. Log and continue on
                    statusHandler.handle(Priority.PROBLEM,
                            "Unable to retrieve data availability offset, using 0 for the offset.",
                            e);
                }

                Date baseRefTime = start.getTime();
                // add the offset and check if it falls within window
                Calendar offsetBaseRefTime = TimeUtil
                        .newGmtCalendar(baseRefTime);
                offsetBaseRefTime.add(Calendar.MINUTE, availabilityOffset);

                if (offsetBaseRefTime.after(cplanStart)
                        && offsetBaseRefTime.before(cplanEnd)) {
                    /*
                     * Fine grain check by hour and minute, for
                     * subscription(start/end), activePeriod(start/end)
                     */

                    if (shouldScheduleForTime(baseRefTime)) {
                        subscriptionTimes.add(baseRefTime);
                    }
                }
            }

            // Start the next day..
            start.add(Calendar.DAY_OF_YEAR, 1);
            start.set(Calendar.HOUR_OF_DAY, 0);
        }

        return subscriptionTimes;
    }
}