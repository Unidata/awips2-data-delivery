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

import java.util.Set;

import com.raytheon.uf.common.datadelivery.registry.GroupDefinition;
import com.raytheon.uf.common.datadelivery.registry.Subscription;

/**
 * Data needed to validate a group definition
 *
 * <pre>
 *
 * SOFTWARE HISTORY
 *
 * Date         Ticket#    Engineer    Description
 * ------------ ---------- ----------- --------------------------
 * Nov 17, 2017 6343       tgurney     Initial creation
 *
 * </pre>
 *
 * @author tgurney
 */

public interface IGroupValidationData {

    /**
     * @return List of subscriptions in the original group. Will be empty if
     *         creating a new group rather than editing an existing group.
     */
    public Set<Subscription> getOldSubscriptions();

    /**
     * @return List of subscriptions in the new group.
     */
    public Set<Subscription> getNewSubscriptions();

    /**
     * @return The original group definition, before any changes are made. Will
     *         be null if creating a new group rather than editing an existing
     *         one.
     */
    public GroupDefinition getOldGroupDefinition();

    /**
     * @return The new group definition
     */
    public GroupDefinition getNewGroupDefinition();
}
