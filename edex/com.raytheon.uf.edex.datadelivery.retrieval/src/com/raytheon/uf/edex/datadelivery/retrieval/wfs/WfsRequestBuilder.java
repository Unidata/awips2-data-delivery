package com.raytheon.uf.edex.datadelivery.retrieval.wfs;

import java.text.SimpleDateFormat;
import java.util.TimeZone;

import org.geotools.geometry.jts.ReferencedEnvelope;

import com.raytheon.uf.common.datadelivery.registry.Coverage;
import com.raytheon.uf.common.datadelivery.registry.EnvelopeUtils;
import com.raytheon.uf.common.datadelivery.registry.Provider.ServiceType;
import com.raytheon.uf.common.datadelivery.registry.Time;
import com.raytheon.uf.common.datadelivery.retrieval.util.HarvesterServiceManager;
import com.raytheon.uf.common.datadelivery.retrieval.xml.Retrieval;
import com.raytheon.uf.common.datadelivery.retrieval.xml.RetrievalAttribute;
import com.raytheon.uf.common.datadelivery.retrieval.xml.ServiceConfig;
import com.raytheon.uf.common.status.IUFStatusHandler;
import com.raytheon.uf.common.status.UFStatus;
import com.raytheon.uf.edex.datadelivery.retrieval.request.RequestBuilder;
import com.vividsolutions.jts.geom.Coordinate;

/**
 *
 * WFS Request Builder.
 *
 * <pre>
 *
 * SOFTWARE HISTORY
 *
 * Date          Ticket#  Engineer  Description
 * ------------- -------- --------- --------------------------------------------
 * May 12, 2013  753      dhladky   created.
 * May 31, 2013  2038     djohnson  Move to correct repo.
 * Jun 11, 2013  1763     dhladky   Made operational.
 * Jun 18, 2013  2120     dhladky   Added times and max feature processing
 * Aug 07, 2013  2097     dhladky   Revamped WFS to use POST (still 1.1.0 WFS)
 * Sep 11, 2013  2351     dhladky   Fixed adhoc request times
 * Sep 17, 2013  2383     bgonzale  Removed exceptional code for point start and
 *                                  end since the times will be correct now.
 * Oct 01, 2013  1797     dhladky   Generics
 * Oct 10, 2013  1797     bgonzale  Refactored registry Time objects.
 * Oct 28, 2013  2448     dhladky   Request start time incorrectly used
 *                                  subscription start time.
 * Nov 20, 2013  2554     dhladky   MaxFeatures was misplaced.
 * Sep 27, 2014  3127     dhladky   Moved some constants to super class.
 * Jul 25, 2017  6186     rjpeter   Use Retrieval
 *
 * </pre>
 *
 * @author dhladky
 */

public class WfsRequestBuilder<T extends Time, C extends Coverage>
        extends RequestBuilder<T, C> {

    private static final IUFStatusHandler statusHandler = UFStatus
            .getHandler(WfsRequestBuilder.class);

    protected static final ServiceConfig wfsServiceConfig = HarvesterServiceManager
            .getInstance().getServiceConfig(ServiceType.WFS);

    public static final String SEPERATOR = getServiceConfig()
            .getConstantValue("COMMA");

    public static final String SLASH = getServiceConfig()
            .getConstantValue("FORWARD_SLASH");

    public static final String BBOX = getServiceConfig()
            .getConstantValue("BBOX_HEADER");

    public static final String SRS = getServiceConfig()
            .getConstantValue("SRSNAME_HEADER");

    public static final String CRS = getServiceConfig()
            .getConstantValue("DEFAULT_CRS");

    public static final String TIME = getServiceConfig()
            .getConstantValue("TIME_HEADER");

    public static final String EQUALS = getServiceConfig()
            .getConstantValue("EQUALS");

    public static final String BLANK = getServiceConfig()
            .getConstantValue("BLANK");

    public static final String MAX = getServiceConfig().getConstantValue("MAX");

    public static final String VERSION = getServiceConfig()
            .getConstantValue("VERSION");

    private final String wfsURL;

    private String typeName = null;

    public WfsRequestBuilder(WfsRetrievalAdapter adapter,
            Retrieval<T, C> retrieval) {
        super(retrieval);
        // Create XML doc
        this.typeName = retrieval.getPlugin();
        StringBuilder buffer = new StringBuilder(256);
        buffer.append(createHeader());
        buffer.append(FILTER_OPEN).append(NEW_LINE);
        RetrievalAttribute<T, C> att = retrieval.getAttribute();

        if (att.getCoverage() != null && att.getTime() != null) {
            buffer.append(AND_OPEN).append(NEW_LINE);
        }

        buffer.append(processTime(att.getTime()))
                .append(processCoverage(att.getCoverage()));

        if (att.getCoverage() != null && att.getTime() != null) {
            buffer.append(AND_CLOSE).append(NEW_LINE);
        }

        buffer.append(FILTER_CLOSE).append(NEW_LINE).append(createFooter());
        this.wfsURL = buffer.toString().trim();
    }

    private static ThreadLocal<SimpleDateFormat> ogcDateFormat = new ThreadLocal<SimpleDateFormat>() {

        @Override
        protected SimpleDateFormat initialValue() {

            SimpleDateFormat sdf = new SimpleDateFormat(
                    getServiceConfig().getDateConfig().getFormats().get(0));
            sdf.setTimeZone(TimeZone.getTimeZone("GMT"));
            return sdf;
        }
    };

    /**
     * Creates the WFS XML Query header
     *
     * @return
     */
    private String createHeader() {

        StringBuilder sb = new StringBuilder(256);
        sb.append("<?xml version=\"1.0\" ?>\n");
        sb.append("<wfs:GetFeature service=\"WFS\"\n");
        sb.append("version=\"").append(VERSION).append("\"\n");
        sb.append("maxFeatures=\"").append(MAX).append("\"\n");
        sb.append("outputFormat=\"application/gml+xml; version=3.1\"\n");
        sb.append("xmlns:").append(typeName).append("=\"http://")
                .append(typeName).append(".edex.uf.raytheon.com\"\n");
        sb.append("xmlns:wfs=\"http://www.opengis.net/wfs\"\n");
        sb.append("xmlns:gml=\"http://www.opengis.net/gml\"\n");
        sb.append("xmlns:ogc=\"http://www.opengis.net/ogc\"\n");
        sb.append("xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\"\n");
        sb.append("xsi:schemaLocation=\"http://www.opengis.net/wfs../wfs/")
                .append(VERSION).append("/WFS.xsd\">\n");
        sb.append("<wfs:Query typeName=\"").append(typeName).append(":")
                .append(typeName).append("\">\n");

        return sb.toString();
    }

    /**
     * Creates the WFS XML Query Footer
     *
     * @return
     */
    private String createFooter() {
        String footer = "</wfs:Query>\n</wfs:GetFeature>\n";
        return footer;
    }

    @Override
    public String processTime(T inTime) {

        try {

            String endDateString = null;
            String startDateString = null;

            // need to grab the request start and end times
            endDateString = ogcDateFormat.get().format(inTime.getRequestEnd());
            startDateString = ogcDateFormat.get()
                    .format(inTime.getRequestStart());

            // TODO: switch to timeObs if its adhoc
            StringBuilder sb = new StringBuilder(256);
            sb.append(PROPRERTYISGREATERTHAN_OPEN).append(NEW_LINE);
            sb.append(PROPERTTY_OPEN).append(typeName).append(":insertTime")
                    .append(PROPERTTY_CLOSE).append(NEW_LINE);
            sb.append(ISLITERAL_OPEN).append(startDateString)
                    .append(ISLITERAL_CLOSE).append(NEW_LINE);
            sb.append(PROPRERTYISGREATERTHAN_CLOSE).append(NEW_LINE);

            if (endDateString != null) {
                sb.append(PROPRERTYISLESSTHAN_OPEN).append(NEW_LINE);
                sb.append(PROPERTTY_OPEN).append(typeName).append(":insertTime")
                        .append(PROPERTTY_CLOSE).append(NEW_LINE);
                sb.append(ISLITERAL_OPEN).append(endDateString)
                        .append(ISLITERAL_CLOSE).append(NEW_LINE);
                sb.append(PROPRERTYISLESSTHAN_CLOSE).append(NEW_LINE);
            }

            return sb.toString();

        } catch (Exception e) {
            statusHandler.error("Couldn't parse Time object.", e);
        }

        return BLANK;
    }

    @Override
    public String processCoverage(C coverage) {

        if (coverage != null) {
            try {
                StringBuilder sb = new StringBuilder(256);
                ReferencedEnvelope re = coverage.getRequestEnvelope();
                Coordinate ll = EnvelopeUtils.getLowerLeftLatLon(re);
                Coordinate ur = EnvelopeUtils.getUpperRightLatLon(re);
                // manage the box
                double lowerLon = ll.x;
                double lowerLat = ll.y;
                double upperLon = ur.x;
                double upperLat = ur.y;

                sb.append(WITHIN_OPEN).append(NEW_LINE);
                sb.append(PROPERTTY_OPEN).append("location/location")
                        .append(PROPERTTY_CLOSE).append(NEW_LINE);
                sb.append(ENVELOPE_OPEN).append(" srsName=\"").append(CRS)
                        .append("\">").append(NEW_LINE);
                sb.append(LOWER_CORNER_OPEN);
                sb.append(lowerLon);
                sb.append(SPACE);
                sb.append(lowerLat);
                sb.append(LOWER_CORNER_CLOSE).append(NEW_LINE);
                sb.append(UPPER_CORNER_OPEN);
                sb.append(upperLon);
                sb.append(SPACE);
                sb.append(upperLat);
                sb.append(UPPER_CORNER_CLOSE).append(NEW_LINE);
                sb.append(ENVELOPE_CLOSE).append(NEW_LINE);
                sb.append(WITHIN_CLOSE).append(NEW_LINE);

                return sb.toString();

            } catch (Exception e) {
                statusHandler.error("Couldn't parse Coverage object.", e);
            }
        }

        return BLANK;
    }

    @Override
    public String getRequest() {
        return wfsURL;
    }

    /**
     * Get the instance of the service config
     *
     * @return
     */
    private static ServiceConfig getServiceConfig() {
        return wfsServiceConfig;
    }

}
