package com.raytheon.uf.common.datadelivery.service.subscription;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;

/**
 * Configuration for the {@link ISubscriptionOverlapService}.
 * 
 * <pre>
 * 
 * SOFTWARE HISTORY
 * 
 * Date         Ticket#    Engineer    Description
 * ------------ ---------- ----------- --------------------------
 * Sep 24, 2013   2386     dhladky     Grid Subscription Overlap
 * Oct 21, 2013   2292     mpduff      Change overlap rules
 * Nov 10, 2015   4644     dhladky     Added level overlap rules
 * 
 * </pre>
 * 
 * @author dhladky
 * @version 1.0
 */
@XmlRootElement
@XmlAccessorType(XmlAccessType.NONE)
public class GridSubscriptionOverlapConfig extends SubscriptionOverlapConfig {

    @XmlElement(required = true)
    private int maxAllowedForecastHourDuplication;

    @XmlElement(required = true)
    private int maxAllowedCycleDuplication;
    
    @XmlElement(required = true)
    private int maxAllowedLevelDuplication;

    /**
     * Constructor.
     */
    public GridSubscriptionOverlapConfig() {

    }

    /**
     * Constructor.
     * 
     * @param maxAllowedParameterDuplication
     * @param maxAllowedLevelDuplication
     * @param maxAllowedForecastHourDuplication
     * @param maxAllowedCycleDuplication
     * @param maxAllowedSpatialDuplication
     * @param matchStrategy
     */
    public GridSubscriptionOverlapConfig(int maxAllowedParameterDuplication,
            int maxAllowedLevelDuplication,
            int maxAllowedForecastHourDuplication,
            int maxAllowedCycleDuplication, int maxAllowedSpatialDuplication,
            SubscriptionOverlapMatchStrategy matchStrategy) {

        this.maxAllowedParameterDuplication = maxAllowedParameterDuplication;
        this.maxAllowedLevelDuplication = maxAllowedLevelDuplication;
        this.maxAllowedForecastHourDuplication = maxAllowedForecastHourDuplication;
        this.maxAllowedCycleDuplication = maxAllowedCycleDuplication;
        this.maxAllowedSpatialDuplication = maxAllowedSpatialDuplication;
        this.matchStrategy = matchStrategy;
    }

    /**
     * @return the maxAllowedForecastHourDuplication
     */
    public int getMaxAllowedForecastHourDuplication() {
        return maxAllowedForecastHourDuplication;
    }

    /**
     * @param maxAllowedForecastHourDuplication
     *            the maxAllowedForecastHourDuplication to set
     */
    public void setMaxAllowedForecastHourDuplication(
            int maxAllowedForecastHourDuplication) {
        this.maxAllowedForecastHourDuplication = maxAllowedForecastHourDuplication;
    }

    /**
     * @return the maxAllowedCycleDuplication
     */
    public int getMaxAllowedCycleDuplication() {
        return maxAllowedCycleDuplication;
    }

    /**
     * @param maxAllowedCycleDuplication
     *            the maxAllowedCycleDuplication to set
     */
    public void setMaxAllowedCycleDuplication(int maxAllowedCycleDuplication) {
        this.maxAllowedCycleDuplication = maxAllowedCycleDuplication;
    }
    

    public int getMaxAllowedLevelDuplication() {
        return maxAllowedLevelDuplication;
    }

    /**
     * max allowed level duplication percentage
     * @param maxAllowedLevelDuplication
     */
    public void setMaxAllowedLevelDuplication(int maxAllowedLevelDuplication) {
        this.maxAllowedLevelDuplication = maxAllowedLevelDuplication;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public SubscriptionOverlapConfig getNeverOverlaps() {
        return new GridSubscriptionOverlapConfig(
                SubscriptionOverlapConfig.ONE_HUNDRED_PERCENT,
                SubscriptionOverlapConfig.ONE_HUNDRED_PERCENT,
                SubscriptionOverlapConfig.ONE_HUNDRED_PERCENT,
                SubscriptionOverlapConfig.ONE_HUNDRED_PERCENT,
                SubscriptionOverlapConfig.ONE_HUNDRED_PERCENT,
                SubscriptionOverlapMatchStrategy.MATCH_ALL);
    }

}
