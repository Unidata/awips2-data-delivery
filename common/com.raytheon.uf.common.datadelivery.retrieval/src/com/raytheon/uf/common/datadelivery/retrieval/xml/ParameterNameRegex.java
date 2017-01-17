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

package com.raytheon.uf.common.datadelivery.retrieval.xml;

import java.util.regex.Pattern;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlAttribute;

import com.raytheon.uf.common.serialization.annotations.DynamicSerialize;
import com.raytheon.uf.common.serialization.annotations.DynamicSerializeElement;

/**
 * 
 * Parameter Name Regex XML object
 * 
 * <pre>
 *
 * SOFTWARE HISTORY
 *
 * Date         Ticket#    Engineer    Description
 * ------------ ---------- ----------- --------------------------
 * Dec 2, 2016  5988       tjensen     Initial creation
 *
 * </pre>
 *
 * @author tjensen
 */

@XmlAccessorType(XmlAccessType.NONE)
@DynamicSerialize
public class ParameterNameRegex implements Comparable {

    @XmlAttribute(name = "AWIPS")
    @DynamicSerializeElement
    private String awips;

    @XmlAttribute(name = "pattern")
    @DynamicSerializeElement
    private String regex;

    @XmlAttribute(name = "id")
    @DynamicSerializeElement
    private String id;

    @XmlAttribute(name = "order")
    @DynamicSerializeElement
    private String order;

    public String getOrder() {
        return order;
    }

    public void setOrder(String order) {
        this.order = order;
    }

    private Pattern pattern;

    public String getAwips() {
        return awips;
    }

    public void setAwips(String awips) {
        this.awips = awips;
    }

    public String getRegex() {
        return regex;
    }

    public void setRegex(String regex) {
        this.regex = regex;
    }

    public Pattern getPattern() {
        if (pattern == null) {
            pattern = Pattern.compile("^" + getRegex());
        }
        return pattern;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public int compareTo(Object o) {
        if (o instanceof ParameterNameRegex) {
            ParameterNameRegex other = (ParameterNameRegex) o;
            int orderDiff = order.compareTo(other.getOrder());
            if (orderDiff != 0) {
                return orderDiff;
            }
            return id.compareTo(other.getId());
        }
        return 1;
    }
}
