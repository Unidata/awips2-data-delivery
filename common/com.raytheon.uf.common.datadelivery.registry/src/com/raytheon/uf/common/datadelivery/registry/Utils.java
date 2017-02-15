package com.raytheon.uf.common.datadelivery.registry;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.TimeZone;
import java.util.regex.Matcher;

import com.raytheon.uf.common.status.IUFStatusHandler;
import com.raytheon.uf.common.status.UFStatus;
import com.vividsolutions.jts.geom.Coordinate;
import com.vividsolutions.jts.geom.Geometry;
import com.vividsolutions.jts.geom.GeometryFactory;
import com.vividsolutions.jts.geom.LinearRing;
import com.vividsolutions.jts.geom.Polygon;

/**
 * Collection of convenience methods.
 * 
 * <pre>
 * 
 * SOFTWARE HISTORY
 * 
 * Date          Ticket#  Engineer  Description
 * ------------- -------- --------- --------------------------------------------
 * Feb 20, 2011  218      dhladky   Initial creation.
 * Oct 01, 2012  1103     jpiatt    Added invalid subscription status.
 * Nov 20, 2012  1286     djohnson  Add UNSCHEDULED.
 * Jan 14, 2014  2459     mpduff    Change Subscription status code.
 * Jan 17, 2014  2459     mpduff    Remove unscheduled status, not just
 *                                  deactivated.
 * Nov 19, 2014  3852     dhladky   Resurrected the Unscheduled state.
 * Feb 08, 2017  6089     tjensen   Added resolveSystemProperties()
 * 
 * </pre>
 * 
 * @author dhladky
 * @version 1.0
 */

public class Utils {

    private static final java.util.regex.Pattern propertyPattern = java.util.regex.Pattern
            .compile("\\$\\{([a-zA-Z0-9.-]+)\\}");

    private static final IUFStatusHandler statusHandler = UFStatus
            .getHandler(Utils.class);

    /**
     * Get the geometry for a bounding box
     * 
     * @param upperLeft
     *            upper left corner
     * @param LowerRight
     *            lower right corner
     * 
     * @return bounding box bounding box coordinates
     */
    public static Geometry getGeometry(Coordinate upperLeft,
            Coordinate LowerRight) {

        GeometryFactory factory = new GeometryFactory();
        Coordinate[] coors = new Coordinate[5];

        coors[0] = upperLeft;
        coors[1] = new Coordinate(upperLeft.x, LowerRight.y);
        coors[2] = LowerRight;
        coors[3] = new Coordinate(LowerRight.x, upperLeft.y);
        // complete the square
        coors[4] = coors[0];

        LinearRing lr = factory.createLinearRing(coors);
        Polygon poly = factory.createPolygon(lr, null);

        return poly;
    }

    /**
     * Date conversion.
     * 
     * @param format
     *            pass in date format
     * @param dateString
     *            date in string format
     * 
     * @return Date converted date
     * @throws ParseException
     */
    public static Date convertDate(String format, String dateString)
            throws ParseException {
        SimpleDateFormat sdf = new SimpleDateFormat(format);
        sdf.setTimeZone(TimeZone.getTimeZone("GMT"));
        return (sdf.parse(dateString));
    }

    /**
     * Subscription status options.
     * 
     * <pre>
     * Expired - Do not schedule
     * Deactivated - Do not schedule
     * Active - Currently scheduled
     * Inactive - Not currently scheduled (outside of active period)
     * Invalid - Subscription does not match the available data set
     * Unscheduled - Subscription is ON, but no room to schedule.
     * </pre>
     */
    public static enum SubscriptionStatus {

        /** Active Subscription Status */
        ACTIVE("Active"),
        /** Inactive Subscription Status */
        INACTIVE("Inactive"),
        /** Expired Subscription Status */
        EXPIRED("Expired"),
        /** Deactivated Subscription Status */
        DEACTIVATED("Deactivated"),
        /** Invalid Subscription Status */
        INVALID("Invalid"),
        /** UNscheduled status */
        UNSCHEDULED("Unscheduled");

        private final String status;

        private SubscriptionStatus(String status) {
            this.status = status;
        }

        @Override
        public String toString() {
            return status;
        }
    }

    /**
     * Takes in a string and checks for system properties in the pattern
     * "${PROPERTY_NAME}". For each one it finds, it looks up the value of that
     * property and replaces it inline in the string. If property cannot be
     * found, check for environment variables of the same name. If neither
     * exist, replace the placeholder with just the property name to allow for
     * it to be skipped.
     * 
     * @param inputString
     *            String possibly containing system properties
     * @return String with the all system properties replaced
     */
    public static String resolveSystemProperties(String inputString) {
        String rval = inputString;
        if (rval != null && !"".equals(rval)) {

            StringBuffer sb = new StringBuffer();
            Matcher m = propertyPattern.matcher(rval);

            while (m.find()) {
                String propName = m.group(1);

                String sysProperty = System.getProperty(propName);
                if (sysProperty != null) {
                    m.appendReplacement(sb, sysProperty);
                } else {
                    String envVar = System.getenv(propName);
                    if (envVar != null) {
                        m.appendReplacement(sb, envVar);
                    } else {
                        statusHandler
                                .error("Unable to resolve value of system property '"
                                        + propName + "' from '" + rval + "'");
                        m.appendReplacement(sb, propName);
                    }
                }
            }
            m.appendTail(sb);
            rval = sb.toString();
        }

        return rval;
    }
}
