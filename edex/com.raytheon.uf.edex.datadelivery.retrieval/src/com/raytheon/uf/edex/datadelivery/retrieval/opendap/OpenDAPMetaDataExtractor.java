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
package com.raytheon.uf.edex.datadelivery.retrieval.opendap;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.net.MalformedURLException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

import javax.persistence.Transient;

import com.raytheon.uf.common.datadelivery.registry.Connection;
import com.raytheon.uf.common.datadelivery.registry.Provider.ServiceType;
import com.raytheon.uf.common.datadelivery.retrieval.util.HarvesterServiceManager;
import com.raytheon.uf.edex.datadelivery.retrieval.metadata.MetaDataExtractor;

import opendap.dap.DAP2Exception;
import opendap.dap.DAS;
import opendap.dap.DASException;
import opendap.dap.DConnect;
import opendap.dap.parser.ParseException;

/**
 * Extract OpenDAP MetaData over the web.
 *
 * This class should remain package-private, all access should be limited
 * through the {@link OpenDapServiceFactory}.
 *
 * <pre>
 *
 * SOFTWARE HISTORY
 *
 * Date          Ticket#  Engineer  Description
 * ------------- -------- --------- --------------------------------------------
 * Feb 20, 2011  218      dhladky   Initial creation
 * Jun 28, 2012  819      djohnson  Use utility class for DConnect.
 * Jul 25, 2012  955      djohnson  Make package-private.
 * Aug 06, 2012  1022     djohnson  Cache a retrieved DAS instance.
 * Nov 19, 2012  1166     djohnson  Clean up JAXB representation of registry
 *                                  objects.
 * Jul 08, 2014  3120     dhladky   Fix generics
 * Apr 12, 2015  4400     dhladky   Switched over to DAP2 protocol.
 * Mar 31, 2017  6186     rjpeter   Fixed Generics.
 *
 * </pre>
 *
 * @author dhladky
 */
class OpenDAPMetaDataExtractor extends MetaDataExtractor<String, DAS> {
    private String url;

    enum DAP_TYPE {

        DAS("das"), DDS("dds"), INFO("info"), DODS("dods");

        private final String dapType;

        private DAP_TYPE(String name) {
            dapType = name;
        }

        public String getDapType() {
            return dapType;
        }
    }

    private String rootUrl;

    private SimpleDateFormat sdf = null;

    @Transient
    private transient DAS das;

    OpenDAPMetaDataExtractor(Connection conn) {
        super(conn);
        serviceConfig = HarvesterServiceManager.getInstance()
                .getServiceConfig(ServiceType.OPENDAP);
        sdf = new SimpleDateFormat();
        sdf.applyLocalizedPattern(HarvesterServiceManager.getInstance()
                .getServiceConfig(ServiceType.OPENDAP)
                .getConstantValue("DATE_COMPARE_FORMAT"));
    }

    /**
     * Checks whether or not the data is new
     */
    public boolean checkLastUpdate(Date date) {

        if (date.before(getDataDate())) {
            return true;
        }

        return false;
    }

    @Override
    public Map<String, DAS> extractMetaData(String url) throws Exception {

        try {
            setUrl(url);
            Map<String, DAS> metaData = new HashMap<>();
            // we only need DAS
            metaData.put(DAP_TYPE.DAS.getDapType(), getDASData());
            return metaData;

        } catch (Exception e) {
            logger.error("Can't extract MetaData from URL " + url, e);
            throw e;
        }
    };

    /**
     * Get an OpenDAP protocol connection
     *
     * @param extension
     * @return
     */
    private DConnect getConnection(String curl) {
        try {
            return OpenDAPConnectionUtil.getDConnectDAP2(curl);
        } catch (FileNotFoundException e) {
            logger.error("Error getting connection object for OpenDAP.", e);
        }

        return null;
    }

    private DAS getDASData() throws MalformedURLException, DASException,
            IOException, ParseException, DAP2Exception {
        if (das == null) {
            DConnect conn = getConnection(rootUrl);
            das = conn.getDAS();
        }
        return das;
    }

    /**
     * Sets the data date for comparison
     *
     * @throws Exception
     */
    @Override
    public void setDataDate() throws Exception {
        try {
            DAS das = getDASData();

            String history = das
                    .getAttributeTable(
                            serviceConfig.getConstantValue("NC_GLOBAL"))
                    .getAttribute(serviceConfig.getConstantValue("HISTORY"))
                    .getValueAt(0);
            String[] histories = history.split(":");
            String time = OpenDAPParseUtility.getInstance()
                    .trim(histories[0].trim() + histories[1].trim()
                            + histories[2].trim());

            Date dataDate = null;
            try {
                dataDate = sdf.parse(time);
            } catch (java.text.ParseException e) {
                logger.error(e.getLocalizedMessage(), e);
            }
            setDataDate(dataDate);
        } catch (Exception e) {
            logger.error(e.getLocalizedMessage(), e);
            throw e;
        }
    }

    private void setRootUrl() {
        String webUrl = getUrl();
        int index = webUrl.lastIndexOf('.');
        rootUrl = webUrl.substring(0, index);
    }

    /**
     * Set the URL
     */
    public void setUrl(String url) {
        this.url = url;
        setRootUrl();
        // If the URL changes, then the das object is also invalid
        das = null;
    }

    /**
     * Gets the URL
     *
     * @return
     */
    public String getUrl() {
        return url;
    }
}
