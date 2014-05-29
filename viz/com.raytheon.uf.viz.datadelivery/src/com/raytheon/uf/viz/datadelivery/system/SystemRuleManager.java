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
package com.raytheon.uf.viz.datadelivery.system;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Marshaller;
import javax.xml.bind.Unmarshaller;

import com.raytheon.uf.common.datadelivery.bandwidth.IBandwidthService;
import com.raytheon.uf.common.datadelivery.registry.DataType;
import com.raytheon.uf.common.datadelivery.registry.Network;
import com.raytheon.uf.common.datadelivery.registry.Subscription;
import com.raytheon.uf.common.datadelivery.registry.Subscription.SubscriptionPriority;
import com.raytheon.uf.common.datadelivery.service.subscription.GridSubscriptionOverlapConfig;
import com.raytheon.uf.common.datadelivery.service.subscription.PointSubscriptionOverlapConfig;
import com.raytheon.uf.common.datadelivery.service.subscription.SubscriptionOverlapConfig;
import com.raytheon.uf.common.localization.FileUpdatedMessage;
import com.raytheon.uf.common.localization.ILocalizationFileObserver;
import com.raytheon.uf.common.localization.IPathManager;
import com.raytheon.uf.common.localization.LocalizationContext;
import com.raytheon.uf.common.localization.LocalizationContext.LocalizationLevel;
import com.raytheon.uf.common.localization.LocalizationContext.LocalizationType;
import com.raytheon.uf.common.localization.LocalizationFile;
import com.raytheon.uf.common.localization.PathManagerFactory;
import com.raytheon.uf.common.localization.exception.LocalizationOpFailedException;
import com.raytheon.uf.common.status.IUFStatusHandler;
import com.raytheon.uf.common.status.UFStatus;
import com.raytheon.uf.common.status.UFStatus.Priority;
import com.raytheon.uf.viz.datadelivery.subscription.xml.LatencyRuleXML;
import com.raytheon.uf.viz.datadelivery.subscription.xml.LatencyRulesXML;
import com.raytheon.uf.viz.datadelivery.subscription.xml.PriorityRuleXML;
import com.raytheon.uf.viz.datadelivery.subscription.xml.PriorityRulesXML;
import com.raytheon.uf.viz.datadelivery.subscription.xml.RuleXML;
import com.raytheon.uf.viz.datadelivery.subscription.xml.RulesXML;
import com.raytheon.uf.viz.datadelivery.utils.DataDeliveryUtils;
import com.raytheon.uf.viz.datadelivery.utils.DataSetFrequency;
import com.raytheon.uf.viz.datadelivery.utils.NameOperationItems;
import com.raytheon.uf.viz.datadelivery.utils.TypeOperationItems;

/**
 * System Rule Manager.
 * 
 * <pre>
 * 
 * SOFTWARE HISTORY
 * 
 * Date         Ticket#    Engineer    Description
 * ------------ ---------- ----------- --------------------------
 * Sep 17, 2012    730      jpiatt     Initial creation.
 * Oct 23, 2012   1286      djohnson   Hook into bandwidth management.
 * Jan 04, 2013   1420      mpduff     Move rules into a single file.
 * Jan 25, 2013   1528      djohnson   Subscription priority is now an enum.
 * Jun 04, 2013    223      mpduff     Implement point data types.
 * Jul 11, 2013   2106      djohnson   setAvailableBandwidth service now returns names of subscriptions.
 * Oct 03, 2013   2386      mpduff     Add overlap rules.
 * Nov 19, 2013   2387      skorolev   Add system status refresh listeners.
 * 
 * </pre>
 * 
 * @author jpiatt
 * @version 1.0
 */
public class SystemRuleManager {

    /** SystemRuleManager instance */
    private static final SystemRuleManager instance = new SystemRuleManager();

    /** Directory Path to rules */
    private final String RULE_PATH = "datadelivery" + File.separator
            + "systemManagement" + File.separator + "rules" + File.separator;

    /** Latency rule file */
    private final String LATENCY_RULE_FILE = RULE_PATH + "latencyRules.xml";

    /** Priority rule file */
    private final String PRIORITY_RULE_FILE = RULE_PATH + "priorityRules.xml";

    /** Point overlap rule file */
    private final String POINT_SUB_RULE_FILE = RULE_PATH
            + "POINTSubscriptionOverlapRules.xml";

    /** Grid overlap rule file */
    private final String GRID_SUB_RULE_FILE = RULE_PATH
            + "GRIDSubscriptionOverlapRules.xml";

    /** Status Handler */
    private final IUFStatusHandler statusHandler = UFStatus
            .getHandler(SystemRuleManager.class);

    /** Latency Rules Localization File */
    private LocalizationFile latencyRulesLocFile;

    /** Priority Rules Localization File */
    private LocalizationFile priorityRulesLocFile;

    /** JAXB context */
    private JAXBContext jax;

    /** Marshaller object */
    private Marshaller marshaller;

    /** Unmarshaller object */
    private Unmarshaller unmarshaller;

    /** Bandwidth service */
    private IBandwidthService bandwidthService;

    /** Latency Rules XML object */
    private LatencyRulesXML latencyRules;

    /** Priority Rules XML object */
    private PriorityRulesXML priorityRules;

    /** Map of DataType -> Rules config xml object */
    private Map<DataType, SubscriptionOverlapConfig> overlapRulesMap;

    /** List of listeners */
    private final List<IRulesUpdateListener> listeners = new ArrayList<IRulesUpdateListener>();

    /** List of system refresh listeners */
    private final List<ISystemStatusListener> refListeners = new ArrayList<ISystemStatusListener>();

    /**
     * Constructor.
     */
    private SystemRuleManager() {
        createContext();
    }

    /**
     * Get an instance of the SystemRuleManager.
     * 
     * @return instance
     */
    public static SystemRuleManager getInstance() {
        return instance;
    }

    /**
     * Create the JAXB context
     */
    @SuppressWarnings("rawtypes")
    private void createContext() {
        Class[] classes = new Class[] { RuleXML.class, PriorityRuleXML.class,
                LatencyRuleXML.class, LatencyRulesXML.class,
                PriorityRulesXML.class, RulesXML.class, OperatorTypes.class,
                TypeOperationItems.class, NameOperationItems.class,
                SubscriptionOverlapConfig.class,
                GridSubscriptionOverlapConfig.class,
                PointSubscriptionOverlapConfig.class };

        try {
            jax = JAXBContext.newInstance(classes);
            this.unmarshaller = jax.createUnmarshaller();
            this.marshaller = jax.createMarshaller();
            this.marshaller.setProperty(Marshaller.JAXB_FORMATTED_OUTPUT, true);
        } catch (Exception e) {
            statusHandler.handle(Priority.ERROR, e.getLocalizedMessage(), e);
        }
    }

    /**
     * Get the names of the priority rules
     * 
     * @return String[] of names
     */
    public List<String> getPriorityRuleNames() {
        return getPriorityRules(false).getRuleNames();
    }

    /**
     * Load the priority rule.
     * 
     * @param name
     *            the name of the rule
     * @return the PriorityRuleXML object
     */
    public PriorityRuleXML getPriorityRule(String name) {
        PriorityRulesXML priorityRules = getPriorityRules(false);
        for (PriorityRuleXML rule : priorityRules.getRules()) {
            if (rule.getRuleName().equals(name)) {
                return rule;
            }
        }

        return new PriorityRuleXML();
    }

    /**
     * Load the latency rule.
     * 
     * @param name
     *            the name of the rule
     * @return the LatencyRuleXML object
     */
    public LatencyRuleXML getLatencyRule(String name) {
        LatencyRulesXML latencyRules = getLatencyRules(false);
        for (LatencyRuleXML rule : latencyRules.getRules()) {
            if (rule.getRuleName().equals(name)) {
                return rule;
            }
        }

        return new LatencyRuleXML();
    }

    /**
     * Get latency file names
     * 
     * @return String[] Array of latency rule names
     */
    public List<String> getLatencyRuleNames() {
        return getLatencyRules(false).getRuleNames();
    }

    /**
     * Get the subscription overlap rules for the provided data type.
     * 
     * @param dataType
     *            The data type
     * @return The Subscription overlap config object
     */
    public SubscriptionOverlapConfig getSubscriptionOverlapRules(
            DataType dataType) {
        return getSubscriptionOverlapRules(false).get(dataType);
    }

    /**
     * Save priority rules.
     * 
     * @param xmlObj
     *            the rules to save
     * @return true if saved successful
     */
    public boolean savePriorityRules(PriorityRulesXML xmlObj) {
        IPathManager pm = PathManagerFactory.getPathManager();

        try {
            // If site, then write out, otherwise save it as site.
            if (priorityRulesLocFile.getContext().getLocalizationLevel()
                    .equals(LocalizationLevel.SITE)) {
                marshaller.marshal(xmlObj, priorityRulesLocFile.getFile());
                return priorityRulesLocFile.save();
            } else {
                LocalizationContext context = pm.getContext(
                        LocalizationType.COMMON_STATIC, LocalizationLevel.SITE);

                priorityRulesLocFile = pm.getLocalizationFile(context,
                        this.PRIORITY_RULE_FILE);
                addPriorityRulesFileObserver();
                marshaller.marshal(xmlObj, priorityRulesLocFile.getFile());
                return priorityRulesLocFile.save();
            }
        } catch (JAXBException e) {
            statusHandler.handle(Priority.ERROR, e.getLocalizedMessage(), e);
        } catch (LocalizationOpFailedException e) {
            statusHandler.handle(Priority.ERROR, e.getLocalizedMessage(), e);
        }

        return false;
    }

    /**
     * Save latency rules.
     * 
     * @param xmlObj
     *            the rules to save
     * @return true if saved successful
     */
    public boolean saveLatencyRules(LatencyRulesXML xmlObj) {
        IPathManager pm = PathManagerFactory.getPathManager();

        try {
            // If site, then write out, otherwise save it as site.
            if (latencyRulesLocFile.getContext().getLocalizationLevel()
                    .equals(LocalizationLevel.SITE)) {
                marshaller.marshal(xmlObj, latencyRulesLocFile.getFile());
                return latencyRulesLocFile.save();
            } else {
                LocalizationContext context = pm.getContext(
                        LocalizationType.COMMON_STATIC, LocalizationLevel.SITE);

                latencyRulesLocFile = pm.getLocalizationFile(context,
                        this.LATENCY_RULE_FILE);
                addLatencyRulesFileObserver();
                marshaller.marshal(xmlObj, latencyRulesLocFile.getFile());
                return latencyRulesLocFile.save();
            }
        } catch (JAXBException e) {
            statusHandler.handle(Priority.ERROR, e.getLocalizedMessage(), e);
        } catch (LocalizationOpFailedException e) {
            statusHandler.handle(Priority.ERROR, e.getLocalizedMessage(), e);
        }

        return false;
    }

    /**
     * Delete the latency rule.
     * 
     * @param ruleName
     *            the rule name to delete
     */
    public void deleteLatencyRule(String ruleName) {
        LatencyRulesXML latencyRules = getLatencyRules(false);

        for (LatencyRuleXML rule : latencyRules.getRules()) {
            if (rule.getRuleName().equals(ruleName)) {
                latencyRules.removeRuleByName(ruleName);
                saveLatencyRules(latencyRules);
                return;
            }
        }
    }

    /**
     * Delete the priority rule.
     * 
     * @param ruleName
     *            the rule name to delete
     */
    public void deletePriorityRule(String ruleName) {
        PriorityRulesXML priorityRules = getPriorityRules(false);

        for (PriorityRuleXML rule : priorityRules.getRules()) {
            if (rule.getRuleName().equals(ruleName)) {
                priorityRules.removeRuleByName(ruleName);
                savePriorityRules(priorityRules);
                return;
            }
        }
    }

    /**
     * Update the rule.
     * 
     * @param rule
     *            The rule to update
     * @return true if updated
     */
    public boolean updateRule(LatencyRuleXML rule) {
        LatencyRulesXML rulesXml = getLatencyRules(false);
        boolean saved = rulesXml.updateRule(rule);
        if (saved) {
            return saveLatencyRules(rulesXml);
        }

        return false;
    }

    /**
     * Update the rule.
     * 
     * @param rule
     *            The rule to update
     * @return true if updated
     */
    public boolean updateRule(PriorityRuleXML rule) {
        PriorityRulesXML rulesXml = getPriorityRules(false);
        boolean saved = rulesXml.updateRule(rule);
        if (saved) {
            saved = savePriorityRules(rulesXml);
        }

        if (!saved) {
            this.statusHandler.warn("Error saving rules.");
        }

        return saved;
    }

    /**
     * Save the rule.
     * 
     * @param rule
     *            The rule to update
     * @return true if updated
     */
    public boolean saveRule(PriorityRuleXML rule) {
        PriorityRulesXML rulesXml = getPriorityRules(false);
        boolean saved = rulesXml.addRule(rule);
        if (saved) {
            saved = savePriorityRules(rulesXml);
        }

        if (!saved) {
            this.statusHandler.warn("Error saving Priority rules.");
        }

        return saved;
    }

    /**
     * Save the rule.
     * 
     * @param rule
     *            The rule to update
     * @return true if updated
     */
    public boolean saveRule(LatencyRuleXML rule) {
        LatencyRulesXML rulesXml = getLatencyRules(false);
        boolean saved = rulesXml.addRule(rule);
        if (saved) {
            saved = saveLatencyRules(rulesXml);
        }

        if (!saved) {
            this.statusHandler.warn("Error saving Latency rules.");
        }

        return saved;
    }

    /**
     * Get the latency rules.
     * 
     * @param reread
     *            true to reread the file from disk
     * 
     * @return The latency rules xml object
     */
    private LatencyRulesXML getLatencyRules(boolean reread) {
        if (latencyRules == null || reread) {
            if (latencyRulesLocFile == null) {
                loadLatencyRules();
            }

            if (this.latencyRulesLocFile != null
                    && latencyRulesLocFile.exists()) {
                try {
                    latencyRules = (LatencyRulesXML) unmarshaller
                            .unmarshal(latencyRulesLocFile.getFile());
                } catch (JAXBException e) {
                    statusHandler.handle(Priority.ERROR,
                            e.getLocalizedMessage(), e);
                    latencyRules = new LatencyRulesXML();
                }
            }
        }

        return latencyRules;
    }

    /**
     * Get the priority rules
     * 
     * @param reread
     *            true to reread the file from disk
     * 
     * @return The priority rules xml object
     */
    private PriorityRulesXML getPriorityRules(boolean reread) {
        if (priorityRules == null || reread) {
            if (priorityRulesLocFile == null) {
                loadPriorityRules();
            }

            if (this.priorityRulesLocFile != null
                    && priorityRulesLocFile.exists()) {
                try {
                    priorityRules = (PriorityRulesXML) unmarshaller
                            .unmarshal(priorityRulesLocFile.getFile());
                } catch (JAXBException e) {
                    statusHandler.handle(Priority.ERROR,
                            e.getLocalizedMessage(), e);
                    priorityRules = new PriorityRulesXML();
                }
            }
        }
        return priorityRules;
    }

    /**
     * Get the overlap rules
     * 
     * @param reread
     *            true to reread the file from disk
     * 
     * @return The overlap rules xml object
     */
    private Map<DataType, SubscriptionOverlapConfig> getSubscriptionOverlapRules(
            boolean reread) {
        if (overlapRulesMap == null || reread) {
            loadSubscriptionOverlapRules();
        }

        return overlapRulesMap;
    }

    /**
     * Get the default latency value given the cycleTimes.
     * 
     * @param cycleTimes
     *            list of cycle times
     * @return the default latency value
     */
    public int getDefaultLatency(List<Integer> cycleTimes) {
        DataSetFrequency freq = DataSetFrequency.fromCycleTimes(cycleTimes);

        return freq.getDefaultLatency();
    }

    /**
     * Get latency value for point data.
     * 
     * @param sub
     *            the subscription object
     * @return the latency value
     */
    public int getPointDataLatency(Subscription sub) {
        LatencyRulesXML rulesXml = this.getLatencyRules(false);

        for (LatencyRuleXML rule : rulesXml.getRules()) {
            if (OpsNetFieldNames.NAME.toString().equals(rule.getRuleField())) {
                if (rule.matches(sub, null)) {
                    return rule.getLatency();
                }
            } else if (OpsNetFieldNames.SIZE.toString().equals(
                    rule.getRuleField())) {
                if (rule.matches(sub, null)) {
                    return rule.getLatency();
                }
            } else if (OpsNetFieldNames.TYPE.toString().equals(
                    rule.getRuleField())) {
                if (rule.matches(sub, null)) {
                    return rule.getLatency();
                }
            }
        }

        // Set default if none found
        return DataDeliveryUtils.POINT_DATASET_DEFAULT_LATENCY_IN_MINUTES;
    }

    /**
     * Return the lowest latency value defined by the rules.
     * 
     * @param sub
     *            The subscription
     * @param cycleTimes
     *            The available cycle times
     * @return the latency value
     */
    public int getLatency(Subscription sub, Set<Integer> cycleTimes) {
        LatencyRulesXML rulesXml = this.getLatencyRules(false);
        int latency = 999;
        boolean found = false;
        for (LatencyRuleXML rule : rulesXml.getRules()) {
            if (rule.matches(sub, cycleTimes)) {
                if (rule.getLatency() < latency) {
                    latency = rule.getLatency();
                    found = true;
                }
            }
        }

        // Set default if none found
        if (!found) {
            latency = this
                    .getDefaultLatency(new ArrayList<Integer>(cycleTimes));
        }

        return latency;
    }

    /**
     * Get the priority for the point data subscription.
     * 
     * @param sub
     *            the subscription object
     * @return the priority
     */
    public SubscriptionPriority getPointDataPriority(Subscription sub) {
        PriorityRulesXML rulesXml = this.getPriorityRules(false);
        for (PriorityRuleXML rule : rulesXml.getRules()) {
            if (OpsNetFieldNames.NAME.toString().equals(rule.getRuleField())) {
                if (rule.matches(sub, null)) {
                    return rule.getPriority();
                }
            } else if (OpsNetFieldNames.SIZE.toString().equals(
                    rule.getRuleField())) {
                if (rule.matches(sub, null)) {
                    return rule.getPriority();
                }
            } else if (OpsNetFieldNames.TYPE.toString().equals(
                    rule.getRuleField())) {
                if (rule.matches(sub, null)) {
                    return rule.getPriority();
                }
            }
        }

        return SubscriptionPriority.NORMAL;
    }

    /**
     * Return the lowest priority value defined by the rules.
     * 
     * @param sub
     *            The subscription
     * @param cycleTimes
     *            the cycle times
     * @return the priority
     */
    public SubscriptionPriority getPriority(Subscription sub,
            Set<Integer> cycleTimes) {
        PriorityRulesXML rulesXml = this.getPriorityRules(false);
        SubscriptionPriority priority = null;
        for (PriorityRuleXML rule : rulesXml.getRules()) {
            if (rule.matches(sub, cycleTimes)) {
                if (priority == null
                        || rule.getPriority().getPriorityValue() < priority
                                .getPriorityValue()) {
                    priority = rule.getPriority();
                }
            }
        }

        // Default to normal priority
        if (priority == null) {
            priority = SubscriptionPriority.NORMAL;
        }

        return priority;
    }

    /**
     * Set the {@link IBandwidthService}.
     * 
     * @param bandwidthService
     *            the bandwidthService to set
     */
    public void setBandwidthService(IBandwidthService bandwidthService) {
        this.bandwidthService = bandwidthService;
    }

    /**
     * Get the available bandwidth for a {@link Network}.
     * 
     * @param network
     *            the network
     * @return the available bandwidth
     */
    public static int getAvailableBandwidth(Network network) {
        return getInstance().bandwidthService
                .getBandwidthForNetworkInKilobytes(network);
    }

    /**
     * Propose setting the available bandwidth for a {@link Network}. If the
     * bandwidth amount will not change the scheduled subscriptions it is
     * immediately applied, otherwise the set of subscriptions that would be
     * unscheduled is returned.
     * 
     * @param network
     *            the network
     * @param bandwidth
     *            the available bandwidth
     * @return empty list if successfully applied, otherwise the set of
     *         subscription names that would be unscheduled
     */
    public static Set<String> setAvailableBandwidth(Network network,
            int bandwidth) {
        return getInstance().bandwidthService
                .proposeBandwidthForNetworkInKilobytes(network, bandwidth);
    }

    /**
     * Sets the available bandwidth for a {@link Network}.
     * 
     * @param network
     *            the network
     * @param bandwidth
     *            the bandwidth
     * @return true if successfully applied the change, false otherwise
     */
    public static boolean forceSetAvailableBandwidth(Network network,
            int bandwidth) {
        return getInstance().bandwidthService
                .setBandwidthForNetworkInKilobytes(network, bandwidth);
    }

    /**
     * Read the latency rules file.
     */
    private void loadLatencyRules() {
        IPathManager pm = PathManagerFactory.getPathManager();
        this.latencyRulesLocFile = pm
                .getStaticLocalizationFile(this.LATENCY_RULE_FILE);
        addLatencyRulesFileObserver();
        getLatencyRules(true);
    }

    /**
     * Load the priority rules file.
     */
    private void loadPriorityRules() {
        IPathManager pm = PathManagerFactory.getPathManager();
        this.priorityRulesLocFile = pm
                .getStaticLocalizationFile(this.PRIORITY_RULE_FILE);
        addPriorityRulesFileObserver();
        getPriorityRules(true);
    }

    /**
     * Load the overlap rules.
     */
    private void loadSubscriptionOverlapRules() {
        overlapRulesMap = new HashMap<DataType, SubscriptionOverlapConfig>();
        IPathManager pm = PathManagerFactory.getPathManager();

        for (DataType dt : DataType.values()) {
            LocalizationFile lf = null;
            switch (dt) {
            case GRID:
                lf = pm.getStaticLocalizationFile(this.GRID_SUB_RULE_FILE);
                if (lf != null && lf.exists()) {
                    GridSubscriptionOverlapConfig config;
                    try {
                        config = (GridSubscriptionOverlapConfig) unmarshaller
                                .unmarshal(lf.getFile());
                        overlapRulesMap.put(dt, config);
                    } catch (Exception e) {
                        statusHandler.handle(Priority.PROBLEM,
                                e.getLocalizedMessage(), e);
                    }
                }
                break;
            case POINT:
                lf = pm.getStaticLocalizationFile(this.POINT_SUB_RULE_FILE);
                if (lf != null && lf.exists()) {
                    PointSubscriptionOverlapConfig config;
                    try {
                        config = (PointSubscriptionOverlapConfig) unmarshaller
                                .unmarshal(lf.getFile());
                        overlapRulesMap.put(dt, config);
                    } catch (Exception e) {
                        statusHandler.handle(Priority.PROBLEM,
                                e.getLocalizedMessage(), e);
                    }
                }
                break;

            case PDA:
                // Not yet implemented
                break;

            default:
                break;
            }
            if (lf != null && lf.exists()) {
                addSubscriptionOverlapRulesFileObserver(lf);
            }
        }
    }

    /**
     * Add a file observer to the latency rules file to get notified when the
     * file changes.
     */
    private void addLatencyRulesFileObserver() {
        latencyRulesLocFile
                .addFileUpdatedObserver(new ILocalizationFileObserver() {
                    @Override
                    public void fileUpdated(FileUpdatedMessage message) {
                        loadLatencyRules();
                        fireUpdates();
                    }
                });
    }

    /**
     * Add a file observer to the priority rules file to get notified when the
     * file changes.
     */
    private void addPriorityRulesFileObserver() {
        priorityRulesLocFile
                .addFileUpdatedObserver(new ILocalizationFileObserver() {
                    @Override
                    public void fileUpdated(FileUpdatedMessage message) {
                        loadPriorityRules();
                        fireUpdates();
                    }
                });
    }

    /**
     * Add a file observer to the provided localization file.
     * 
     * @param lf
     *            The localization file
     */
    private void addSubscriptionOverlapRulesFileObserver(LocalizationFile lf) {
        lf.addFileUpdatedObserver(new ILocalizationFileObserver() {
            @Override
            public void fileUpdated(FileUpdatedMessage message) {
                loadSubscriptionOverlapRules();
                fireUpdates();
            }
        });
    }

    /**
     * Notify the listeners the files changed.
     */
    private void fireUpdates() {
        for (IRulesUpdateListener listener : listeners) {
            listener.update();
        }
    }

    /**
     * Register as a listener for rules file changes.
     * 
     * @param listener
     */
    public void registerAsListener(IRulesUpdateListener listener) {
        if (!listeners.contains(listener)) {
            listeners.add(listener);
        }
    }

    /**
     * Unregister as a listener for rules files changed.
     * 
     * @param listener
     */
    public void deregisterAsListener(IRulesUpdateListener listener) {
        if (listeners.contains(listener)) {
            listeners.remove(listener);
        }
    }

    /**
     * Save the overlap rules.
     * 
     * @param config
     *            The data to save
     * @param dataType
     *            The dataType
     * @return true if saved correctly
     */
    public boolean saveOverlapRule(SubscriptionOverlapConfig config,
            DataType dataType) {
        if (dataType == null) {
            throw new IllegalArgumentException("Invalid dataType, null");
        }

        final IPathManager pathManager = PathManagerFactory.getPathManager();
        LocalizationContext context = pathManager.getContext(
                LocalizationType.COMMON_STATIC, LocalizationLevel.SITE);

        String fileName = null;

        if (dataType == DataType.POINT) {
            fileName = this.POINT_SUB_RULE_FILE;
            overlapRulesMap.put(dataType, config);
        } else if (dataType == DataType.GRID) {
            fileName = this.GRID_SUB_RULE_FILE;
            overlapRulesMap.put(dataType, config);
        } else {
            throw new IllegalArgumentException(config.getClass()
                    + " Doesn't have any implementation in use");
        }

        final LocalizationFile configFile = pathManager.getLocalizationFile(
                context, fileName);

        try {
            marshaller.marshal(config, configFile.getFile());
            return configFile.save();
        } catch (JAXBException e) {
            statusHandler.handle(Priority.PROBLEM, e.getLocalizedMessage(), e);
        } catch (LocalizationOpFailedException e) {
            statusHandler.handle(Priority.PROBLEM, e.getLocalizedMessage(), e);
        }

        return false;
    }

    /**
     * Notify the listeners the system status change.
     */
    public void fireStatusChangeListener() {
        for (ISystemStatusListener listener : refListeners) {
            listener.statusRefresh();
        }

    }

    /**
     * Notify the listeners the time change.
     */
    public void fireTimeChangeListener() {
        for (ISystemStatusListener listener : refListeners) {
            listener.timeLabelRefresh();
        }

    }

    /**
     * Register as a listener for status refresh.
     * 
     * @param listener
     */
    public void registerAsRefreshListener(ISystemStatusListener listener) {
        if (!refListeners.contains(listener)) {
            refListeners.add(listener);
        }
    }

    /**
     * Unregister as a listener for status refresh.
     * 
     * @param listener
     */
    public void deregisterAsRefreshListener(ISystemStatusListener listener) {
        if (refListeners.contains(listener)) {
            refListeners.remove(listener);
        }
    }
}
