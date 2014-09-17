package com.raytheon.uf.edex.datadelivery.event.notification;

import java.io.File;
import java.util.Calendar;

import javax.xml.bind.JAXBException;

import com.raytheon.uf.common.localization.PathManagerFactory;
import com.raytheon.uf.common.serialization.SingleTypeJAXBManager;
import com.raytheon.uf.common.serialization.SerializationException;
import com.raytheon.uf.common.status.IUFStatusHandler;
import com.raytheon.uf.common.status.UFStatus;
import com.raytheon.uf.edex.database.DataAccessLayerException;
import com.raytheon.uf.edex.database.purge.PurgeRule;
import com.raytheon.uf.edex.database.purge.PurgeRuleSet;

/**
 * 
 * Performs purge services from spring files.
 * 
 * <pre>
 * 
 * SOFTWARE HISTORY
 * 
 * Date         Ticket#    Engineer    Description
 * ------------ ---------- ----------- --------------------------
 * Mar  6, 2012            jsanchez     Initial creation
 * Sep 10, 2014  3581      ccody        Remove references to SerializationUtil for JAXB operations.
 * 
 * </pre>
 * 
 * @author jsanchez
 * @version 1.0
 */
public class NotificationPurge {

    private static final transient IUFStatusHandler statusHandler = UFStatus
            .getHandler(NotificationPurge.class);

    private PurgeRuleSet purgeRules;

    /**
     * Create a new object
     */
    public NotificationPurge() {
        loadPurgeRules();
    }

    /**
     * Loads purge rules from purge/notificationPurgeRules.xml
     */
    private void loadPurgeRules() {
        File file = PathManagerFactory.getPathManager().getStaticFile(
                "purge/notificationPurgeRules.xml");
        if (file == null) {
            statusHandler
                    .error("Notifcations purge rule not defined!!  Data will not be purged for notifications!");
            return;
        }

        try {
            SingleTypeJAXBManager<PurgeRuleSet>  jaxbManager = 
                        new SingleTypeJAXBManager<PurgeRuleSet>(true,PurgeRuleSet.class);
            purgeRules = (PurgeRuleSet) jaxbManager.unmarshalFromXmlFile(file);
        } catch (JAXBException | SerializationException e) {
            statusHandler.error("Error deserializing notification purge rule from file " + file +"\n", e);
        }
    }

    public void purge() {
        if (purgeRules != null) {
            NotificationDao dao = new NotificationDao();
            Calendar expiration = Calendar.getInstance();
            for (PurgeRule rule : purgeRules.getRules()) {
                if (rule.isPeriodSpecified()) {
                    int ms = new Long(rule.getPeriodInMillis()).intValue();
                    expiration.add(Calendar.MILLISECOND, -ms);
                    try {
                        dao.purgeExpiredData(expiration);
                    } catch (DataAccessLayerException e) {
                        statusHandler.error(
                                "Error purging notification records!", e);
                    }
                }
            }
        }
    }
    
}
