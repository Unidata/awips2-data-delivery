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
package com.raytheon.uf.viz.datadelivery.subscription;

/**
 * Action to update the activate/deactivate buttons.
 * 
 * <pre>
 * 
 * SOFTWARE HISTORY
 * 
 * Date         Ticket#    Engineer    Description
 * ------------ ---------- ----------- --------------------------
 * Jun 15, 2012   687      lvenable    Initial creation
 * May 23, 2013  2020      mpduff      Added updateControls method.
 * 
 * </pre>
 * 
 * @author lvenable
 * @version 1.0
 */

public interface ISubscriptionAction {
    /**
     * Update the Activate button's text.
     * 
     * @param text
     *            The text to display on the button
     */
    public void activateButtonUpdate(String text);

    /**
     * Update the controls.
     */
    public void updateControls();
}
