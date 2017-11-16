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

import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

import com.raytheon.uf.common.datadelivery.registry.GroupDefinition;
import com.raytheon.uf.common.datadelivery.registry.Subscription;

/**
 * Performs validation on a group containing MADIS subscriptions
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

public class MadisGroupValidator implements IGroupValidator {

    @Override
    public String validate(IGroupValidationData data) {
        GroupDefinition oldGroup = data.getOldGroupDefinition();
        if (oldGroup != null) {
            Set<Subscription> madisSubs = data.getOldSubscriptions().stream()
                    .filter(sub -> "MADIS".equals(sub.getProvider()))
                    .collect(Collectors.toSet());
            if (madisSubs.size() > 1) {
                GroupDefinition newGroup = data.getNewGroupDefinition();
                if (!Objects.equals(newGroup.getEnvelope(),
                        oldGroup.getEnvelope())) {
                    return "You may not change the area of a group "
                            + "with multiple MADIS subscriptions.";
                }
            }
        }
        return null;
    }
}
