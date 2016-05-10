package com.raytheon.uf.common.datadelivery.service.subscription;

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
 * Aug, 21 2013  3121      dhladky     Grid Subscription Overlap
 * 
 * </pre>
 * 
 * @author dhladky
 * @version 1.0
 */
@XmlRootElement
@XmlAccessorType(XmlAccessType.NONE)
public class PDASubscriptionOverlapConfig extends SubscriptionOverlapConfig {

    @XmlElement(required = true)
    private int maxAllowedForecastHourDuplication;

    @XmlElement(required = true)
    private int maxAllowedCycleDuplication;

    /**
     * Constructor.
     */
    public PDASubscriptionOverlapConfig() {

    }

    /**
     * Constructor.
     * 
     * @param maxAllowedParameterDuplication
     * @param maxAllowedForecastHourDuplication
     * @param maxAllowedCycleDuplication
     * @param maxAllowedSpatialDuplication
     * @param matchStrategy
     */
    public PDASubscriptionOverlapConfig(int maxAllowedParameterDuplication,
            int maxAllowedForecastHourDuplication,
            int maxAllowedCycleDuplication, int maxAllowedSpatialDuplication,
            SubscriptionOverlapMatchStrategy matchStrategy) {

        this.maxAllowedParameterDuplication = maxAllowedParameterDuplication;
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

