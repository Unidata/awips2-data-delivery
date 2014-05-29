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


/**
 * Initial Pending Subscription Object.
 * 
 * <pre>
 * 
 * SOFTWARE HISTORY
 * 
 * Date         Ticket#    Engineer    Description
 * ------------ ---------- ----------- --------------------------
 * Apr 02, 2013 1841       djohsnon    Converted to interface
 * Sept 30, 2013 1797      dhladky     Some generics
 * 
 * </pre>
 * 
 * @author djohnson
 * @version 1.0
 */

public interface InitialPendingSubscription<T extends Time, C extends Coverage> extends Subscription<T, C> {

    String CHANGE_REQUEST_ID_SLOT = "changeReqId";

    /**
     * Set the change request id
     * 
     * @param changeReqId
     *            the changeReqId to set
     */
    void setChangeReqId(String changeReqId);

    /**
     * Retrieve the change request id
     * 
     * @return the changeReqId
     */
    String getChangeReqId();

    /**
     * Get the change reason.
     * 
     * @return the changeReason
     */
    String getChangeReason();

    /**
     * Set the change reason.
     * 
     * @param changeReason
     *            the changeReason to set
     */
    void setChangeReason(String changeReason);

    /**
     * Create the subscription this pending subscription represents.
     * 
     * @return the subscription
     */
    Subscription<T, C> subscription();
}
