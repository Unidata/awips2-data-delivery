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

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import com.raytheon.uf.common.datadelivery.registry.DataSetMetaData;
import com.raytheon.uf.common.datadelivery.registry.Ensemble;
import com.raytheon.uf.common.datadelivery.registry.GriddedCoverage;
import com.raytheon.uf.common.datadelivery.registry.GriddedTime;
import com.raytheon.uf.common.datadelivery.registry.Levels;
import com.raytheon.uf.common.datadelivery.registry.OpenDapGriddedDataSet;
import com.raytheon.uf.common.datadelivery.registry.Parameter;
import com.raytheon.uf.common.datadelivery.registry.Provider;
import com.raytheon.uf.common.datadelivery.registry.Provider.ServiceType;
import com.raytheon.uf.common.datadelivery.registry.ProviderType;
import com.raytheon.uf.common.datadelivery.registry.Subscription;
import com.raytheon.uf.common.datadelivery.retrieval.xml.Retrieval;
import com.raytheon.uf.common.datadelivery.retrieval.xml.RetrievalAttribute;
import com.raytheon.uf.common.dataplugin.level.Level;
import com.raytheon.uf.common.time.DataTime;
import com.raytheon.uf.common.util.CollectionUtil;
import com.raytheon.uf.edex.datadelivery.retrieval.RetrievalGenerator;
import com.raytheon.uf.edex.datadelivery.retrieval.adapters.RetrievalAdapter;
import com.raytheon.uf.edex.datadelivery.retrieval.util.ResponseProcessingUtilities;
import com.raytheon.uf.edex.datadelivery.retrieval.util.RetrievalGeneratorUtilities;

/**
 *
 * {@link RetrievalGenerator} implementation for OpenDAP.
 *
 * <pre>
 *
 * SOFTWARE HISTORY
 *
 * Date          Ticket#  Engineer  Description
 * ------------- -------- --------- --------------------------------------------
 * Feb 20, 2011  218      dhladky   Initial creation
 * Jul 24, 2012  955      djohnson  Check multiple datasets for cycles provided
 *                                  on subscription.
 * Aug 02, 2012  955      djohnson  Use DataSetQuery to get all metadata
 *                                  objects.
 * Aug 10, 2012  1022     djohnson  Retrieve latest available url from {@link
 *                                  OpenDapGriddedDataSet}.
 * Aug 20, 2012  743      djohnson  Cycle will no longer be null.
 * Sep 24, 2012  1209     djohnson  NO_CYCLE metadatas can now fulfill
 *                                  subscriptions.
 * Oct 05, 2012  1241     djohnson  Replace RegistryManager calls with registry
 *                                  handler calls.
 * Nov 19, 2012  1166     djohnson  Clean up JAXB representation of registry
 *                                  objects.
 * Nov 25, 2012  1340     dhladky   Added type for subscriptions to retrieval
 * Dec 10, 2012  1259     bsteffen  Switch Data Delivery from LatLon to
 *                                  referenced envelopes.
 * Sep 18, 2013  2383     bgonzale  Added subscription name to log output.
 * Sep 25, 2013  1797     dhladky   separated time from gridded time
 * Sep 27, 2014  3121     dhladky   removed un-needed casting.
 * Apr 05, 2017  1045     tjensen   Add Coverage generics for DataSetMetaData
 * May 09, 2017  6186     rjpeter   Update to handle passed in DataSetMetaData.
 * Jun 13, 2017  6204     nabowle   Cleanup.
 * Jul 27, 2017  6186     rjpeter   Use Retrieval
 * Aug 02, 2017  6186     rjpeter   Moved satisfiesSubscription to DataSetMetaData.
 *
 * </pre>
 *
 * @author djohnson
 */
class OpenDAPRetrievalGenerator
        extends RetrievalGenerator<GriddedTime, GriddedCoverage> {
    public OpenDAPRetrievalGenerator() {
        super(ServiceType.OPENDAP);
    }

    /***
     * Build the necessary retrieval objects
     *
     * @param bundle
     * @return
     */
    @Override
    public List<Retrieval<GriddedTime, GriddedCoverage>> buildRetrieval(
            DataSetMetaData<GriddedTime, GriddedCoverage> dsmd,
            Subscription<GriddedTime, GriddedCoverage> subscription,
            Provider provider) {
        List<Retrieval<GriddedTime, GriddedCoverage>> retrievals = Collections
                .emptyList();
        switch (subscription.getDataSetType()) {
        case GRID:
            retrievals = getGridRetrievals(dsmd, subscription, provider);
            break;
        default:
            logger.error("Point DATA OPENDAP NOT YET IMPLEMENTED");
        }

        return retrievals;
    }

    /**
     * Gets the size of the data set
     *
     * @param c
     * @return
     */
    private int getDimensionalSize(GriddedCoverage c) {

        return c.getGridCoverage().getNx() * c.getGridCoverage().getNy();
    }

    /**
     * Process the RetrievalAttribute
     *
     * @param parm
     * @return
     */
    protected Map<DataTime, List<Level>> getGridDuplicates(String name,
            Parameter parm, List<DataTime> times, List<Level> levels,
            List<String> ensembleMembers, GriddedCoverage cov) {

        return RetrievalGeneratorUtilities.findGridDuplicates(name, times,
                levels, ensembleMembers, parm, cov.getRequestGridCoverage());
    }

    /**
     * create the grid type retrievals
     *
     * @param dsmd
     * @param sub
     * @param provider
     * @return
     */
    private List<Retrieval<GriddedTime, GriddedCoverage>> getGridRetrievals(
            DataSetMetaData<GriddedTime, GriddedCoverage> dsmd,
            Subscription<GriddedTime, GriddedCoverage> sub, Provider provider) {
        sub = removeDuplicates(sub);

        // Subscription has already been fully retrieved
        if (sub == null) {
            return Collections.emptyList();
        }

        int sfactor = getSizingFactor(getDimensionalSize(sub.getCoverage()));

        List<Ensemble> ensembles = null;
        if (sub.getEnsemble() == null) {
            ensembles = Arrays.asList((Ensemble) null);
        } else {
            ensembles = sub.getEnsemble().split(1);
        }

        List<Retrieval<GriddedTime, GriddedCoverage>> retrievals = new ArrayList<>();
        GriddedTime subTime = sub.getTime();
        for (List<Integer> timeSequence : subTime.getTimeSequences(sfactor)) {
            for (Parameter param : sub.getParameter()) {
                final Levels paramLevels = param.getLevels();
                if (CollectionUtil
                        .isNullOrEmpty(paramLevels.getSelectedLevelIndices())) {
                    // handle single level
                    paramLevels.setRequestLevelEnd(0);
                    paramLevels.setRequestLevelStart(0);
                    List<GriddedTime> times = processTime(timeSequence,
                            sub.getTime());

                    for (GriddedTime time : times) {
                        for (Ensemble ensemble : ensembles) {
                            Retrieval<GriddedTime, GriddedCoverage> retrieval = getRetrieval(
                                    dsmd, sub, provider, param, paramLevels,
                                    time, ensemble);
                            retrievals.add(retrieval);
                        }
                    }
                } else {
                    for (List<Integer> levelSequence : paramLevels
                            .getLevelSequences(sfactor)) {

                        List<GriddedTime> times = processTime(timeSequence,
                                sub.getTime());
                        List<Levels> levels = processLevels(levelSequence,
                                paramLevels);

                        // temporarily make all requests single level
                        // and time
                        for (GriddedTime time : times) {
                            for (Levels level : levels) {
                                for (Ensemble ensemble : ensembles) {
                                    Retrieval<GriddedTime, GriddedCoverage> retrieval = getRetrieval(
                                            dsmd, sub, provider, param, level,
                                            time, ensemble);
                                    retrievals.add(retrieval);
                                }
                            }
                        }
                    }
                }
            }
        }

        return retrievals;
    }

    /**
     * Get the retrieval
     *
     * @param dsmd
     * @param sub
     * @param provider
     * @param param
     * @param level
     * @param time
     * @param ensemble
     * @return
     */
    private Retrieval<GriddedTime, GriddedCoverage> getRetrieval(
            DataSetMetaData<GriddedTime, GriddedCoverage> dsmd,
            Subscription<GriddedTime, GriddedCoverage> sub, Provider provider,
            Parameter param, Levels level, GriddedTime time,
            Ensemble ensemble) {

        Retrieval<GriddedTime, GriddedCoverage> retrieval = new Retrieval<>();
        retrieval.setSubscriptionName(sub.getName());
        retrieval.setServiceType(getServiceType());
        retrieval.setUrl(dsmd.getUrl());
        retrieval.setOwner(sub.getOwner());
        retrieval.setSubscriptionType(getSubscriptionType(sub));
        retrieval.setNetwork(sub.getRoute());
        retrieval.setProvider(sub.getProvider());

        // Coverage and type processing
        GriddedCoverage cov = dsmd.getInstanceCoverage();

        if (cov == null) {
            cov = sub.getCoverage();
        }

        // Attribute processing
        RetrievalAttribute<GriddedTime, GriddedCoverage> att = new RetrievalAttribute<>();
        Parameter lparam = processParameter(param);
        att.setCoverage(cov);
        lparam.setLevels(level);
        att.setTime(time);
        att.setParameter(lparam);
        att.setEnsemble(ensemble);

        // Look up the provider's configured plugin for this data type
        ProviderType providerType = provider
                .getProviderType(sub.getDataSetType());
        retrieval.setPlugin(providerType.getPlugin());
        retrieval.setAttribute(att);

        return retrieval;
    }

    @Override
    public RetrievalAdapter<GriddedTime, GriddedCoverage> getServiceRetrievalAdapter() {
        return new OpenDAPRetrievalAdapter();
    }

    /**
     * Sizing factor so we don't run box out of Heap
     *
     * @param size
     * @return
     */
    public int getSizingFactor(int size) {

        int sfactor = 0;

        if (size > 1_000_000) {
            sfactor = 1;
        } else if (size < 1_000_000 && size > 750_000) {
            sfactor = 2;
        } else if (size < 750_000 && size > 500_000) {
            sfactor = 3;
        } else if (size < 500_000 && size > 250_000) {
            sfactor = 4;
        } else {
            sfactor = 5;
        }

        return sfactor;
    }

    /**
     * Get the level sequence
     *
     * @param levelSequence
     * @param parmLevels
     * @return
     */
    private List<Levels> processLevels(List<Integer> levelSequence,
            Levels parmLevels) {

        List<Levels> levels = new ArrayList<>();
        for (Integer currentLevelSequence : levelSequence) {
            Levels level = new Levels();
            level.setLevel(parmLevels.getLevel());
            level.setDz(parmLevels.getDz());
            level.setLevelType(parmLevels.getLevelType());
            level.setRequestLevelStart(currentLevelSequence);
            level.setRequestLevelEnd(currentLevelSequence);
            level.setName(parmLevels.getName());
            level.setSelectedLevelIndices(Arrays.asList(currentLevelSequence));
            levels.add(level);
        }

        return levels;

    }

    /**
     * Process sequences of hours for separate retrieval
     *
     * @param timeSequence
     * @param subTime
     * @return
     */
    private List<GriddedTime> processTime(List<Integer> timeSequence,
            GriddedTime subTime) {

        List<GriddedTime> times = new ArrayList<>(timeSequence.size());
        for (Integer timeSeq : timeSequence) {
            GriddedTime time = new GriddedTime();
            time.setEnd(subTime.getEnd());
            time.setStart(subTime.getStart());
            time.setNumTimes(subTime.getNumTimes());
            time.setFormat(subTime.getFormat());
            time.setStep(subTime.getStep());
            time.setStepUnit(subTime.getStepUnit());
            List<Integer> indicies = new ArrayList<>(1);
            indicies.add(timeSeq);
            time.setSelectedTimeIndices(indicies);
            time.setRequestStartTimeAsInt(timeSeq);
            time.setRequestEndTimeAsInt(timeSeq);
            times.add(time);
        }

        return times;
    }

    /**
     * Remove duplicate levels, times, subscriptions
     */
    protected Subscription<GriddedTime, GriddedCoverage> removeDuplicates(
            Subscription<GriddedTime, GriddedCoverage> sub) {

        GriddedCoverage cov = sub.getCoverage();
        GriddedTime time = sub.getTime();

        int sfactor = getSizingFactor(getDimensionalSize(cov));

        List<String> ensembles = null;
        if (sub.getEnsemble() != null && sub.getEnsemble().hasSelection()) {
            ensembles = sub.getEnsemble().getSelectedMembers();
        } else {
            ensembles = Arrays.asList((String) null);
        }

        for (List<Integer> timeSequence : time.getTimeSequences(sfactor)) {

            for (Parameter param : sub.getParameter()) {

                if (param.getLevels().getSelectedLevelIndices() == null || param
                        .getLevels().getSelectedLevelIndices().size() == 0) {
                    // levels don't matter so much here it's just one
                    param.getLevels().setRequestLevelEnd(0);
                    param.getLevels().setRequestLevelStart(0);

                    List<DataTime> times = null;

                    for (GriddedTime gtime : processTime(timeSequence, time)) {
                        times = ResponseProcessingUtilities
                                .getOpenDAPGridDataTimes(gtime);
                    }

                    List<Level> levels = ResponseProcessingUtilities
                            .getOpenDAPGridLevels(param.getLevels());

                    Map<DataTime, List<Level>> dups = getGridDuplicates(
                            sub.getName(), param, times, levels, ensembles,
                            cov);

                    for (int i = 0; i < times.size(); i++) {
                        DataTime dtime = times.get(i);
                        List<Level> levDups = dups.get(dtime);

                        if (levDups != null) {
                            // single level, remove the time
                            time.getSelectedTimeIndices()
                                    .remove(timeSequence.get(i));
                            logger.info("Removing duplicate time: "
                                    + dtime.toString());
                        }
                    }

                } else {

                    for (List<Integer> levelSequence : param.getLevels()
                            .getLevelSequences(sfactor)) {

                        List<DataTime> times = null;
                        List<Level> plevels = null;

                        for (GriddedTime gtime : processTime(timeSequence,
                                time)) {
                            times = ResponseProcessingUtilities
                                    .getOpenDAPGridDataTimes(gtime);
                        }
                        for (Levels level : processLevels(levelSequence,
                                param.getLevels())) {
                            plevels = ResponseProcessingUtilities
                                    .getOpenDAPGridLevels(level);
                        }

                        Map<DataTime, List<Level>> dups = getGridDuplicates(
                                sub.getName(), param, times, plevels, ensembles,
                                cov);

                        for (int i = 0; i < times.size(); i++) {
                            DataTime dtime = times.get(i);
                            List<Level> levDups = dups.get(dtime);

                            if (levDups != null) {

                                if (levDups.size() == plevels.size()) {
                                    // just remove the entire time
                                    time.getSelectedTimeIndices()
                                            .remove(timeSequence.get(i));
                                    logger.info("Removing duplicate time: "
                                            + dtime.toString());
                                } else {

                                    for (int j = 0; j < plevels.size(); j++) {
                                        Level lev = plevels.get(j);
                                        for (Level plev : levDups) {
                                            if (plev.equals(lev)) {
                                                param.getLevels()
                                                        .getSelectedLevelIndices()
                                                        .remove(levelSequence
                                                                .get(j));
                                                logger.info(
                                                        "Removing duplicate level: "
                                                                + lev.getMasterLevel()
                                                                        .getDescription());
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
        // remove entire subscription, it's a duplicate
        if (time.getSelectedTimeIndices().isEmpty()) {
            logger.info("Removing duplicate subscription: " + sub.getName());
            return null;
        }

        return sub;

    }
}
