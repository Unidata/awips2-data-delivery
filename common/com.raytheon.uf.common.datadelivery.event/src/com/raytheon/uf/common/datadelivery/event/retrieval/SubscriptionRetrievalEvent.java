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
package com.raytheon.uf.common.datadelivery.event.retrieval;

import java.text.SimpleDateFormat;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import com.raytheon.uf.common.datadelivery.event.INotifiableEvent;
import com.raytheon.uf.common.datadelivery.event.notification.NotificationRecord;
import com.raytheon.uf.common.serialization.annotations.DynamicSerialize;
import com.raytheon.uf.common.serialization.annotations.DynamicSerializeElement;

/**
 * Subscription Retrieval Event
 * 
 * Needs
 * 
 * <pre>
 * 
 * SOFTWARE HISTORY 
 * 
 * Date          Ticket#  Engineer  Description
 * ------------- -------- --------- --------------------------------------------
 * Aug 21, 2012           jsanchez  Made object serializable.
 * Nov 20, 2012           dhladky   More fields.
 * Dec 07, 2012  1104     djohnson  Simplify event type hierarchy.
 * Jul 22, 2014  2732     ccody     Add Date Time to SubscriptionRetrievalEvent
 *                                  message
 * May 17, 2016  5662     tjensen   Separated Data Time from rest of message
 * 
 * </pre>
 * 
 * @author jsanchez
 * @version 1.0
 */
@DynamicSerialize
public class SubscriptionRetrievalEvent extends RetrievalEvent implements
        INotifiableEvent {
    public enum Status {
        SUCCESS, PARTIAL_SUCCESS, FAILED
    }

    private static final long serialVersionUID = -1139458560029047046L;

    private static final Map<String, String> FIELD_UNIT_MAP;

    private static final String sdfString = "yyyy-MM-dd HH:mm:ss";

    private static ThreadLocal<SimpleDateFormat> subscriptionDateFormat = new ThreadLocal<SimpleDateFormat>() {
        @Override
        protected SimpleDateFormat initialValue() {
            SimpleDateFormat sdf = new SimpleDateFormat(sdfString);
            return sdf;
        }
    };

    static {
        Map<String, String> m = new HashMap<>();
        m.put("numComplete", "count");
        m.put("numfailed", "count");
        FIELD_UNIT_MAP = Collections.unmodifiableMap(m);
    }

    protected Status status;

    protected String subscriptionType;

    protected String failureMessage;

    protected Long retrievalRequestTime;

    @DynamicSerializeElement
    protected int numFailed;

    @DynamicSerializeElement
    protected int numComplete;

    @Override
    public NotificationRecord generateNotification() {
        String subname = getId();

        int priority = 3;
        StringBuffer sb = new StringBuffer();
        String retrievalRequestTimeString = null;
        if (retrievalRequestTime != null) {
            retrievalRequestTimeString = subscriptionDateFormat.get().format(
                    retrievalRequestTime);
        }

        switch (getStatus()) {
        case SUCCESS:
            sb.append("Successfully retrieved and stored data for ");
            sb.append(subname);
            if (retrievalRequestTime != null) {
                sb.append("; Data Time: " + retrievalRequestTimeString);
            }
            break;
        case PARTIAL_SUCCESS:
            priority = 1;
            sb.append("Partial-success retrieving data for ");
            sb.append(subname);
            sb.append(". Failure message: " + getFailureMessage());
            if (retrievalRequestTime != null) {
                sb.append("; Data Time: " + retrievalRequestTimeString);
            }
            break;
        case FAILED:
            priority = 1;
            sb.append("Failed data retrieval for ");
            sb.append(subname);
            sb.append(". Failure message: " + getFailureMessage());
            if (retrievalRequestTime != null) {
                sb.append("; Data Time: " + retrievalRequestTimeString);
            }
            break;
        }

        NotificationRecord record = new NotificationRecord();
        record.setDate(getDate());
        record.setCategory(subname);
        record.setUsername(getOwner());
        record.setPriority(priority);
        record.setMessage(sb.toString());

        return record;
    }

    public String getFailureMessage() {
        return failureMessage;
    }

    @Override
    protected Map<String, String> getFieldUnitMap() {
        return FIELD_UNIT_MAP;
    }

    public int getNumComplete() {
        return numComplete;
    }

    public int getNumFailed() {
        return numFailed;
    }

    public Status getStatus() {
        return status;
    }

    public String getSubscriptionType() {
        return subscriptionType;
    }

    public void setSubscriptionType(String subscriptionType) {
        this.subscriptionType = subscriptionType;
    }

    public void setFailureMessage(String failureMessage) {
        this.failureMessage = failureMessage;
    }

    public void setNumComplete(int numComplete) {
        this.numComplete = numComplete;
    }

    public void setNumFailed(int numFailed) {
        this.numFailed = numFailed;
    }

    public void setStatus(Status status) {
        this.status = status;
    }

    public Long getRetrievalRequestTime() {
        return retrievalRequestTime;
    }

    public void setRetrievalRequestTime(Long retrievalRequestTime) {
        if (retrievalRequestTime == null) {
            retrievalRequestTime = Long.valueOf(0L);
        }
        this.retrievalRequestTime = retrievalRequestTime;
    }

    @Override
    public String toString() {
        StringBuilder rval = new StringBuilder(200);
        rval.append("SubscriptionRetrieval ");
        rval.append(super.toString());
        switch (status) {
        case SUCCESS:
            rval.append(" succeeded");
            break;
        case PARTIAL_SUCCESS:
            rval.append(" partially succeeded.  Failure message: "
                    + failureMessage);
            break;
        case FAILED:
            rval.append(" failed.  Failure message: " + failureMessage);
        }
        return rval.toString();
    }

    @Override
    public void finalizeEvent() {
        // Not implemented
    }
}
