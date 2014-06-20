package com.raytheon.uf.edex.datadelivery.harvester;

import java.io.File;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import com.raytheon.uf.common.datadelivery.harvester.Agent;
import com.raytheon.uf.common.datadelivery.harvester.HarvesterConfig;
import com.raytheon.uf.common.datadelivery.harvester.HarvesterConfigurationManager;
import com.raytheon.uf.common.datadelivery.registry.Provider;
import com.raytheon.uf.common.datadelivery.registry.handlers.IProviderHandler;
import com.raytheon.uf.common.localization.IPathManager;
import com.raytheon.uf.common.localization.LocalizationContext;
import com.raytheon.uf.common.localization.LocalizationContext.LocalizationLevel;
import com.raytheon.uf.common.localization.LocalizationContext.LocalizationType;
import com.raytheon.uf.common.localization.LocalizationFile;
import com.raytheon.uf.common.localization.PathManagerFactory;
import com.raytheon.uf.common.registry.ebxml.RegistryUtil;
import com.raytheon.uf.common.status.IUFStatusHandler;
import com.raytheon.uf.common.status.UFStatus;
import com.raytheon.uf.edex.registry.ebxml.init.RegistryInitializedListener;

/**
 * Handler for harvesting MetaData/Parameter/DataSet objects from harvested provider inputs.
 * 
 * <pre>
 * 
 * SOFTWARE HISTORY
 * Date         Ticket#    Engineer    Description
 * ------------ ---------- ----------- --------------------------
 * Jun 17, 2014 3120       dhladky     abstracted out from CrawlMetaData handler.
 * 
 * </pre>
 * 
 * @author dhladky
 * @version 1.0
 */

public abstract class MetaDataHandler implements RegistryInitializedListener {

    protected static final IUFStatusHandler statusHandler = UFStatus
            .getHandler(MetaDataHandler.class);
    
    /**
     * Read in the config files
     * 
     * @return
     */

    public Map<String, HarvesterConfig> readConfigs() {
        Map<String, HarvesterConfig> hcs = null;

        IPathManager pm = PathManagerFactory.getPathManager();
        LocalizationContext lc = pm.getContext(LocalizationType.COMMON_STATIC,
                LocalizationLevel.SITE);
        List<LocalizationFile> files = Launcher.getLocalizedFiles();

        if (files != null) {
            for (LocalizationFile file : files) {
                try {
                    HarvesterConfig hc = HarvesterConfigurationManager
                            .getHarvesterFile(file.getFile());
                    Agent agent = hc.getAgent();

                    if (agent != null) {
                        // create if null
                        if (hcs == null) {
                            hcs = new HashMap<String, HarvesterConfig>();
                        }
                        // place into config map by provider name key
                        hcs.put(hc.getProvider().getName(), hc);
                    }
                } catch (Exception e1) {
                    statusHandler.error(
                            "Serialization Error Reading Config files", e1);
                }
            }

            if (hcs == null) {
                File path = pm.getFile(lc, "datadelivery/harvester");
                statusHandler.info("No Configs found: "
                        + path.getAbsolutePath());
            }
        }

        return hcs;
    }

    /**
     * map of harvester configurations
     */
    protected Map<String, HarvesterConfig> hconfigs = null;

    /**
     * The provider handler
     */
    protected IProviderHandler providerHandler = null;

    @Override
    public void executeAfterRegistryInit() {
        statusHandler
                .info("<<<<<<<<<<<<<<<<<<<<< INITIALIZING META DATA HANDLER >>>>>>>>>>>>>>>>>>>>>>");
        hconfigs = readConfigs();

        if (hconfigs != null) {
            for (Entry<String, HarvesterConfig> entry : hconfigs.entrySet()) {
                try {

                    Provider provider = entry.getValue().getProvider();
                    statusHandler.info("Inserting/Updating Provider: "
                            + provider.getName() + ": "
                            + provider.getServiceType());
                    providerHandler.update(RegistryUtil.registryUser, provider);

                } catch (Exception e) {
                    statusHandler.error("Error inserting/updating Provider! ",
                            e);
                }
            }
        }
    }

}

