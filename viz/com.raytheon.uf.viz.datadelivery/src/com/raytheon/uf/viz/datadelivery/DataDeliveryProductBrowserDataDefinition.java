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
package com.raytheon.uf.viz.datadelivery;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.lang.StringUtils;
import org.eclipse.ui.IEditorPart;
import org.eclipse.ui.IWorkbenchWindow;

import com.raytheon.uf.common.datadelivery.registry.AdhocSubscription;
import com.raytheon.uf.common.datadelivery.registry.Coverage;
import com.raytheon.uf.common.datadelivery.registry.DataType;
import com.raytheon.uf.common.datadelivery.registry.Subscription;
import com.raytheon.uf.common.datadelivery.registry.Subscription.SubscriptionType;
import com.raytheon.uf.common.datadelivery.registry.handlers.IAdhocSubscriptionHandler;
import com.raytheon.uf.common.datadelivery.registry.handlers.ISubscriptionHandler;
import com.raytheon.uf.common.dataquery.requests.DbQueryRequest;
import com.raytheon.uf.common.dataquery.requests.RequestConstraint;
import com.raytheon.uf.common.dataquery.requests.RequestConstraint.ConstraintType;
import com.raytheon.uf.common.dataquery.responses.DbQueryResponse;
import com.raytheon.uf.common.registry.handler.RegistryHandlerException;
import com.raytheon.uf.common.registry.handler.RegistryObjectHandlers;
import com.raytheon.uf.common.status.IUFStatusHandler;
import com.raytheon.uf.common.status.UFStatus;
import com.raytheon.uf.common.status.UFStatus.Priority;
import com.raytheon.uf.common.time.BinOffset;
import com.raytheon.uf.viz.core.DescriptorMap;
import com.raytheon.uf.viz.core.IDisplayPaneContainer;
import com.raytheon.uf.viz.core.RecordFactory;
import com.raytheon.uf.viz.core.drawables.AbstractRenderableDisplay;
import com.raytheon.uf.viz.core.drawables.IDescriptor;
import com.raytheon.uf.viz.core.drawables.ResourcePair;
import com.raytheon.uf.viz.core.exception.VizException;
import com.raytheon.uf.viz.core.grid.rsc.GridLoadProperties;
import com.raytheon.uf.viz.core.map.MapDescriptor;
import com.raytheon.uf.viz.core.procedures.Bundle;
import com.raytheon.uf.viz.core.requests.ThriftClient;
import com.raytheon.uf.viz.core.rsc.AbstractRequestableResourceData;
import com.raytheon.uf.viz.core.rsc.DisplayType;
import com.raytheon.uf.viz.core.rsc.LoadProperties;
import com.raytheon.uf.viz.core.rsc.ResourceProperties;
import com.raytheon.uf.viz.core.rsc.ResourceType;
import com.raytheon.uf.viz.core.rsc.capabilities.DisplayTypeCapability;
import com.raytheon.uf.viz.datadelivery.utils.DataDeliveryUtils;
import com.raytheon.uf.viz.productbrowser.ProductBrowserLabel;
import com.raytheon.uf.viz.productbrowser.ProductBrowserPreference;
import com.raytheon.uf.viz.productbrowser.pref.PreferenceBasedDataDefinition;
import com.raytheon.viz.grid.GridProductBrowserDataFormatter;
import com.raytheon.viz.grid.inv.GridInventory;
import com.raytheon.viz.grid.rsc.GridResourceData;
import com.raytheon.viz.pointdata.PlotModels;
import com.raytheon.viz.pointdata.rsc.PlotResourceData;
import com.raytheon.viz.pointdata.util.PointDataInventory;
import com.raytheon.viz.satellite.rsc.SatResourceData;
import com.raytheon.viz.ui.BundleProductLoader;
import com.raytheon.viz.ui.EditorUtil;
import com.raytheon.viz.ui.VizWorkbenchManager;
import com.raytheon.viz.ui.editor.AbstractEditor;
import com.raytheon.viz.ui.perspectives.AbstractVizPerspectiveManager;
import com.raytheon.viz.ui.perspectives.VizPerspectiveListener;

/**
 * Product browser implementation for data delivery data
 * 
 * <pre>
 * 
 * SOFTWARE HISTORY
 * 
 * Date          Ticket#  Engineer  Description
 * ------------- -------- --------- --------------------------------------------
 * Sep 17, 2013  2391     mpduff    Initial creation
 * Sep 22, 2013  2246     dhladky   Setup binoffset for time into +-5 min
 *                                  intervals
 * Oct 13, 2013  2460     dhladky   Added display of Adhoc subscriptions
 * Nov 19, 2013  2458     mpduff    Only pull subscriptions for the local site
 * Nov 21, 2013  2554     dhladky   Restored ADHOC's to working.
 * Jan 14, 2014  2459     mpduff    Change Subscription status code
 * Feb 11, 2014  2771     bgonzale  Use Data Delivery ID instead of Site.
 * Jun 24, 2014  3128     bclement  changed loadProperties to be
 *                                  GridLoadProperties
 * Jul 07, 2014  3135     bsteffen  Allow reuse of definition across multiple
 *                                  tree selections.
 * Sep 09, 2014  3356     njensen   Remove CommunicationException
 * Jun 11, 2015  4042     dhladky   Refactored using bsteffen's interface to
 *                                  make it thread safe, cleaner.
 * Jun 16, 2015  4566     dhladky   Fixed error in map for Plugin names.
 * May 04, 2016  5599     tjensen   Fixed PDA label population.
 * 
 * </pre>
 * 
 * @author mpduff
 * @version 1.0
 */

public class DataDeliveryProductBrowserDataDefinition implements
        PreferenceBasedDataDefinition {

    private final IUFStatusHandler statusHandler = UFStatus
            .getHandler(DataDeliveryProductBrowserDataDefinition.class);

    /** Constant for MADIS plugin lookups */
    private static final String MADIS = "madis";

    /** Constant for GRID plugin lookups */
    private static final String GRID = "grid";

    /** Constant for sat plugin lookups */
    private static final String PDA = "satellite";

    /** Constant for point plugin lookups */
    private static final String POINT = "point";

    /** Plot model file */
    private static final String SVG = "madisObsDesign.svg";

    /** Constant */
    private static final String PLUGIN_NAME = "pluginName";

    /** Constant */
    private static final String DATA_DELIVERY = "Data Delivery";

    /** Point order */
    private static final String[] POINT_ORDER = new String[] { "type",
            "subscription", "level" };

    /** PDA order, same as point */
    private static final String[] PDA_ORDER = new String[] { "sectorID",
            "creatingEntity", "physicalElement" };

    private static final String[] GRID_ORDER = new String[] {
            GridInventory.MODEL_NAME_QUERY, GridInventory.PARAMETER_QUERY,
            GridInventory.MASTER_LEVEL_QUERY, GridInventory.LEVEL_ID_QUERY };

    /** cache lookup plugins to DataDelivery names map **/
    private Map<DataType, String> productMap;

    /**
     * Setup as 5 mins +- (60x5=300) from a reference time
     */
    private static final int frameOffset = 300;

    /**
     * Constructor.
     */
    public DataDeliveryProductBrowserDataDefinition() {

        HashMap<DataType, String> protoProductMap = new HashMap<>(3);
        protoProductMap.put(DataType.GRID, GRID);
        protoProductMap.put(DataType.PDA, PDA);
        protoProductMap.put(DataType.POINT, POINT);
        productMap = Collections.unmodifiableMap(protoProductMap);
    }

    /**
     * {@inheritDoc}
     */
    public List<ProductBrowserLabel> populateData(String[] selection) {

        if (selection.length == 1) {
            // Get provider names
            String[] dataTypes = getDataTypes();
            return formatData("Data Type", dataTypes, selection);
        }

        if (selection.length == 2) {
            String source = selection[1];
            String[] subs = getSubscriptions(source);
            return formatData("SubscriptionName", subs, selection);
        }

        if (selection.length == 3) {
            if (selection[1].equalsIgnoreCase(DataType.POINT.name())) {
                String[] order = extractOrder(selection);
                String[] results = PlotModels.getInstance().getLevels(MADIS,
                        SVG);
                String param = order[2];
                return formatData(param, results, selection);
            } else if (selection[1].equalsIgnoreCase(DataType.GRID.name())
                    || selection[1].equalsIgnoreCase(DataType.PDA.name())) {
                /*
                 * Must remove the first selection so this matches with the
                 * grid/pda version
                 */
                String[] usedSelection = realignSelection(selection);

                return populateUpperData(usedSelection);

            }
        }

        if (selection.length >= 4) {
            if (selection[1].equalsIgnoreCase(DataType.GRID.name())
                    || selection[1].equalsIgnoreCase(DataType.PDA.name())) {
                // Must remove the first selection so this matches with the grid
                // version
                String[] usedSelection = realignSelection(selection);

                return populateUpperData(usedSelection);
            }
        }
        return null;
    }

    /**
     * Populate the Upper (Higher Level menus) of the product drop downs
     * 
     * @param selection
     * @return
     */
    public List<ProductBrowserLabel> populateUpperData(String[] selection) {

        List<ProductBrowserLabel> parameters = null;
        boolean product = false;
        String[] order = extractOrder(selection);

        String param = order[selection.length - 1];
        HashMap<String, RequestConstraint> queryList = getProductParameters(
                selection, order);
        product = selection.length == order.length;

        String[] temp = queryData(param, queryList, selection);
        if (temp != null) {
            parameters = formatData(param, temp, selection);
        } else {
            throw new IllegalArgumentException(
                    "Query for menu data cannot be null..." + product);
        }

        if (parameters != null) {
            for (int i = 0; i < parameters.size(); i++) {
                parameters.get(i).setProduct(product);
            }
        }

        return parameters;
    }

    /**
     * Reorder the selection so that it lines up with the correct index of the
     * product order arrays (GRID_ORDER, PDA_ORDER, etc) Meaning: You have to
     * back off one selection index in order to process the correct index of the
     * selection array to produce the correct resource.
     * 
     * @param selection
     * @return
     */
    protected final String[] realignSelection(String[] selection) {
        String[] usedSelection = new String[selection.length - 1];
        for (int i = 1; i < selection.length; i++) {
            usedSelection[i - 1] = selection[i];
        }
        return usedSelection;
    }

    public List<ProductBrowserLabel> formatData(String param,
            String[] parameters, String[] selection) {
        if (Arrays.asList(GRID_ORDER).contains(param)) {
            return GridProductBrowserDataFormatter.formatGridData(param,
                    parameters);
        } else {
            /* Data Type or point data. */
            List<ProductBrowserLabel> temp = new ArrayList<>();
            String[] order = extractOrder(selection);
            for (int i = 0; i < parameters.length; i++) {
                ProductBrowserLabel label = new ProductBrowserLabel(
                        parameters[i], parameters[i]);
                temp.add(label);
                label.setProduct(param == order[2]);
                label.setData(parameters[i]);
            }
            Collections.sort(temp);
            return temp;
        }
    }

    /**
     * Function for taking upper menu (higher level) data and renaming it for
     * the product browser tree
     * 
     * 
     * @param param
     * @param parameters
     * @return
     */
    public List<ProductBrowserLabel> formatUpperData(String param,
            String[] parameters) {
        List<ProductBrowserLabel> temp = new ArrayList<>();
        for (int i = 0; i < parameters.length; i++) {
            temp.add(new ProductBrowserLabel(parameters[i], null));
        }
        Collections.sort(temp);
        return temp;
    }

    /**
     * {@inheritDoc}
     */
    public AbstractRequestableResourceData getResourceData(String[] selection) {

        AbstractRequestableResourceData resourceData = null;
        String selectedDataType = selection[1];

        if (selectedDataType.equalsIgnoreCase(DataType.POINT.name())) {
            PlotResourceData plotResourceData = new PlotResourceData();
            plotResourceData.setBinOffset(new BinOffset(frameOffset,
                    frameOffset));
            plotResourceData.setRetrieveData(false);
            plotResourceData.setUpdatingOnMetadataOnly(true);
            plotResourceData.setTopOfTheHour(false);
            plotResourceData.setPlotSource(selection[selection.length - 3]
                    + " " + selection[selection.length - 2] + " "
                    + selection[selection.length - 1]);
            plotResourceData.setPlotModelFile(SVG);
            plotResourceData.setLevelKey(selection[selection.length - 1]);
            resourceData = plotResourceData;
        } else if (selectedDataType.equalsIgnoreCase(DataType.GRID.name())) {
            resourceData = new GridResourceData();
        } else if (selectedDataType.equalsIgnoreCase(DataType.PDA.name())) {
            resourceData = new SatResourceData();
        }

        return resourceData;
    }

    /**
     * {@inheritDoc}
     */
    public HashMap<String, RequestConstraint> getProductParameters(
            String[] selection, String[] order) {

        String productName = extractProductName(selection);

        if (productName.equalsIgnoreCase(DataType.POINT.name())) {
            return getPointProductParameters(selection, order);
        } else if (productName.equalsIgnoreCase(DataType.GRID.name())) {
            return getGridProductParameters(selection, order);
        } else if (productName.equalsIgnoreCase(productMap.get(DataType.PDA))) {
            return getPdaProductParameters(selection, order);
        } else {
            throw new IllegalArgumentException("Invalid data type: "
                    + productName);
        }
    }

    /**
     * Getting the map of request constraints for upper menus populating the
     * resource data
     * 
     * @param selection
     * @param order
     * @return
     */
    public HashMap<String, RequestConstraint> getUpperProductParameters(
            String[] selection, String[] order) {
        HashMap<String, RequestConstraint> queryList = new HashMap<>();
        String productName = extractProductName(selection);
        queryList.put(PLUGIN_NAME, new RequestConstraint(productName));

        String[] usedSelection = realignSelection(selection);
        for (int i = 0; i < usedSelection.length; i++) {
            queryList.put(order[i], new RequestConstraint(usedSelection[i]));
        }
        return queryList;
    }

    /**
     * Get the grid parameters.
     * 
     * @param selection
     *            The selected node
     * 
     * @param order
     *            The order
     * @return The constraint map
     */
    private HashMap<String, RequestConstraint> getGridProductParameters(
            String[] selection, String[] order) {
        if (selection.length > 5) {
            /*
             * Must remove the first selection so this matches with the grid
             * version
             */
            String[] tmpSelection = realignSelection(selection);

            return getUpperProductParameters(tmpSelection, order);
        } else {
            return getUpperProductParameters(selection, order);
        }
    }

    /**
     * Get the PDA parameters
     * 
     * @param selection
     *            The selected node
     * @param order
     *            The order
     * @return The constraint map
     */
    private HashMap<String, RequestConstraint> getPdaProductParameters(
            String[] selection, String[] order) {
        if (selection.length > 4) {
            /*
             * Must remove the first selection so this matches with the pda
             * version
             */
            String[] tmpSelection = realignSelection(selection);

            return getUpperProductParameters(tmpSelection, order);
        } else {
            return getUpperProductParameters(selection, order);
        }
    }

    /**
     * Get the point parameters.
     * 
     * @param selection
     *            The selected node
     * 
     * @param order
     *            The order
     * @return The constraint map
     */
    private HashMap<String, RequestConstraint> getPointProductParameters(
            String[] selection, String[] order) {
        HashMap<String, RequestConstraint> queryList = new HashMap<>();
        queryList.put(PLUGIN_NAME, new RequestConstraint(MADIS));
        PointDataInventory inv = PlotModels.getInstance().getInventory();
        if (!inv.getTypeKey(selection[1])
                .equals(PointDataInventory.PLUGIN_NAME)) {
            queryList.put(inv.getTypeKey(selection[1]), new RequestConstraint(
                    selection[2]));
        }

        List<RequestConstraint> spatialCons = getSpatialConstraint(selection[2]);
        if (spatialCons != null && !spatialCons.isEmpty()) {
            queryList.put("location.longitude", spatialCons.get(0));
            queryList.put("location.latitude", spatialCons.get(1));
        }

        return queryList;
    }

    /**
     * Get the spatial constraints.
     * 
     * @return List of spatial constraints, or empty list if none
     */
    @SuppressWarnings("rawtypes")
    private List<RequestConstraint> getSpatialConstraint(
            String selectedSubscriptionName) {

        List<RequestConstraint> cons = new ArrayList<>();

        Coverage cov = null;
        for (Subscription s : getSubscriptions()) {
            if (s.getName().equals(selectedSubscriptionName)) {
                cov = s.getCoverage();
                break;
            }
        }

        if (cov != null) {
            RequestConstraint lonConstraint = new RequestConstraint(
                    cov.getRequestUpperLeft().x + "--"
                            + cov.getRequestLowerRight().x,
                    ConstraintType.BETWEEN);
            RequestConstraint latConstraint = new RequestConstraint(
                    cov.getRequestLowerRight().y + "--"
                            + cov.getRequestUpperLeft().y,
                    ConstraintType.BETWEEN);
            cons.add(lonConstraint);
            cons.add(latConstraint);
        }

        return cons;
    }

    /**
     * Get the subscriptions for the specified data type.
     * 
     * @param dataType
     *            The data type
     * 
     * @return Array of subscription names for the specified data type
     */
    private String[] getSubscriptions(String dataType) {
        DataType dt = DataType.valueOf(dataType.toUpperCase());
        List<String> subscriptionList = getSubscriptions(dt);
        return subscriptionList.toArray(new String[subscriptionList.size()]);
    }

    /**
     * Get the subscriptions for the specified data type.
     * 
     * @param dataType
     *            The data type
     * @return
     */
    @SuppressWarnings("rawtypes")
    private List<String> getSubscriptions(DataType dataType) {
        final List<String> subNames = new ArrayList<>();

        List<Subscription> subList = getSubscriptions();
        for (Subscription s : subList) {
            if (s.isActive()
                    || s.getSubscriptionType().equals(SubscriptionType.QUERY)) {
                if (s.getDataSetType() == dataType) {
                    subNames.add(s.getName());
                }
            }
        }

        return subNames;
    }

    /**
     * Extract data types from the subscriptions listed
     * 
     * @return string[]
     */
    @SuppressWarnings("rawtypes")
    private String[] getDataTypes() {
        List<Subscription> subList = getSubscriptions();
        List<String> dataTypes = new ArrayList<>();
        for (Subscription s : subList) {
            String dt = StringUtils.capitalize(s.getDataSetType().name()
                    .toLowerCase());
            if (!dataTypes.contains(dt)) {
                dataTypes.add(dt);
            }
        }

        return dataTypes.toArray(new String[dataTypes.size()]);
    }

    /**
     * Get a list of subscriptions from the registry.
     * 
     * @return List of subscriptions
     */
    @SuppressWarnings("rawtypes")
    private List<Subscription> getSubscriptions() {
        List<Subscription> subList = new ArrayList<>();
        final ISubscriptionHandler handler = RegistryObjectHandlers
                .get(ISubscriptionHandler.class);
        try {
            subList = handler.getByFilters(null,
                    DataDeliveryUtils.getDataDeliveryId());
        } catch (RegistryHandlerException e) {
            statusHandler.handle(Priority.PROBLEM, e.getLocalizedMessage(), e);
        }

        final IAdhocSubscriptionHandler adhochandler = RegistryObjectHandlers
                .get(IAdhocSubscriptionHandler.class);
        List<AdhocSubscription> adhocSubs = null;

        try {
            adhocSubs = adhochandler.getAll();
        } catch (RegistryHandlerException e) {
            statusHandler.handle(Priority.PROBLEM, e.getLocalizedMessage(), e);
        }

        if (adhocSubs != null) {
            subList.addAll(adhocSubs);
        }

        return subList;
    }

    public Map<ResourceType, List<DisplayType>> getDisplayTypes(
            String[] selection) {

        for (String selectedDataType : selection) {
            if (selectedDataType.equalsIgnoreCase(DataType.GRID.name())) {
                Map<ResourceType, List<DisplayType>> type = new HashMap<>();
                List<DisplayType> types = new ArrayList<>();
                types.add(DisplayType.CONTOUR);
                types.add(DisplayType.IMAGE);
                /*
                 * Stage 2 of this process will be more tricky. Need to
                 * determine in this ProductBroswer definition if both U & V
                 * wind components are available Then query both sets of PDOs
                 * and merge them into a GeneralGridGeometry so you can call
                 * VectorDataUV (Draws barbs) with the data for both.
                 */
                // types.add(DisplayType.BARB);
                type.put(ResourceType.PLAN_VIEW, types);
                return type;
            } else if (selectedDataType.equalsIgnoreCase(DataType.PDA.name())) {
                Map<ResourceType, List<DisplayType>> type = new HashMap<>();
                List<DisplayType> types = new ArrayList<>();
                types.add(DisplayType.CONTOUR);
                types.add(DisplayType.IMAGE);
                type.put(ResourceType.PLAN_VIEW, types);
                return type;
            }
        }

        // Point type just returns null
        return null;
    }

    @Override
    public List<ProductBrowserPreference> getPreferences() {
        return null;
    }

    /**
     * Gets the Display pane for the editor
     * 
     * @return
     */
    private IDisplayPaneContainer getEditor() {
        String id = DescriptorMap.getEditorId(getDescriptorClass().getName());
        IEditorPart editorPart = EditorUtil.getActiveEditor();
        if (editorPart != null && id.equals(editorPart.getEditorSite().getId())) {
            return (AbstractEditor) editorPart;
        }
        editorPart = EditorUtil.findEditor(id);
        if (editorPart != null) {
            return (AbstractEditor) editorPart;
        }
        return openNewEditor(id);
    }

    /**
     * Open the editor
     * 
     * @param editorId
     * @return
     */
    private IDisplayPaneContainer openNewEditor(String editorId) {
        IWorkbenchWindow window = VizWorkbenchManager.getInstance()
                .getCurrentWindow();
        AbstractVizPerspectiveManager mgr = VizPerspectiveListener.getInstance(
                window).getActivePerspectiveManager();
        if (mgr != null) {
            AbstractEditor editor = mgr.openNewEditor();
            if (editor == null) {
                return null;
            } else if (editorId.equals(editor.getEditorSite().getId())) {
                return editor;
            } else {
                window.getActivePage().closeEditor(editor, false);
            }
        }
        return null;
    }

    /**
     * Get the descriptor for these resources
     * 
     * @return
     */
    private Class<? extends IDescriptor> getDescriptorClass() {
        return MapDescriptor.class;
    }

    @Override
    public boolean checkAvailability() {
        return true;
    }

    @Override
    public Collection<DisplayType> getValidDisplayTypes(String[] selection) {
        Map<ResourceType, List<DisplayType>> displayTypeMap = getDisplayTypes(selection);
        EnumSet<DisplayType> result = EnumSet.noneOf(DisplayType.class);
        if (displayTypeMap != null) {
            for (List<DisplayType> displayTypes : displayTypeMap.values()) {
                result.addAll(displayTypes);
            }
        }
        return result;
    }

    @Override
    public void loadResource(String[] selection, DisplayType displayType) {

        // Get the editor
        IDisplayPaneContainer container = getEditor();
        if (container == null) {
            return;
        }

        // create the default load properties
        LoadProperties loadProperties = new GridLoadProperties();
        AbstractRequestableResourceData resourceData = getResourceData(selection);

        if (displayType != null) {
            loadProperties.getCapabilities()
                    .getCapability(resourceData, DisplayTypeCapability.class)
                    .setDisplayType(displayType);
        }

        // processing specific to MADIS, points
        String[] order = extractOrder(selection);
        (resourceData).setMetadataMap(getProductParameters(selection, order));

        // Make the resource pair
        ResourcePair pair = new ResourcePair();
        pair.setResourceData(resourceData);
        pair.setLoadProperties(loadProperties);
        pair.setProperties(new ResourceProperties());

        // Construct the display, add the pair
        AbstractRenderableDisplay display = (AbstractRenderableDisplay) container
                .getActiveDisplayPane().getRenderableDisplay();
        display = (AbstractRenderableDisplay) display.createNewDisplay();
        display.getDescriptor().getResourceList().add(pair);

        // Get the bundle, add display
        Bundle b = new Bundle();
        b.setDisplays(new AbstractRenderableDisplay[] { display });
        new BundleProductLoader(EditorUtil.getActiveVizContainer(), b)
                .schedule();
    }

    @Override
    public List<ProductBrowserLabel> getLabels(String[] selection) {
        if (selection.length == 0) {
            // This is only used for the initial population of the tree
            String[] order = extractOrder(selection);
            ProductBrowserLabel label = new ProductBrowserLabel(DATA_DELIVERY,
                    DATA_DELIVERY);
            label.setData(DATA_DELIVERY);
            label.setProduct(order.length == 0);
            return Collections.singletonList(label);
        }

        return populateData(selection);
    }

    @Override
    public String getProductInfo(String[] selection) {
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append(DATA_DELIVERY);
        for (int i = 1; i < selection.length; i++) {
            stringBuilder.append("\n");
            stringBuilder.append(selection[i]);
        }
        return stringBuilder.toString();
    }

    /**
     * Query the levels and types for the menus items
     * 
     * @param param
     * @param queryList
     * @return
     */
    protected String[] queryData(String param,
            Map<String, RequestConstraint> queryList, String[] selection) {
        try {
            String queryPluginName = extractProductName(selection);
            DbQueryRequest request = new DbQueryRequest();
            request.setEntityClass(RecordFactory.getInstance().getPluginClass(
                    queryPluginName));
            request.setConstraints(queryList);
            request.addRequestField(param);
            request.setDistinct(true);
            DbQueryResponse response = (DbQueryResponse) ThriftClient
                    .sendRequest(request);
            Object[] paramObjs = response.getFieldObjects(param, Object.class);
            if (paramObjs != null) {
                String[] params = new String[paramObjs.length];
                for (int i = 0; i < params.length; i += 1) {
                    if (paramObjs[i] != null) {
                        params[i] = paramObjs[i].toString();
                    }
                }
                return params;
            }
        } catch (VizException e) {
            statusHandler
                    .handle(Priority.PROBLEM, "Unable to perform query", e);
        }
        return null;
    }

    /**
     * Extracts the product (plugin) name from the selection string array
     * 
     * @param selection
     * @return
     */
    private String extractProductName(String[] selection) {

        String productName = null;

        for (String name : selection) {
            if (name.equalsIgnoreCase(DataType.GRID.name())) {
                productName = productMap.get(DataType.GRID);
                break;
            } else if (name.equalsIgnoreCase(DataType.PDA.name())) {
                productName = productMap.get(DataType.PDA);
                break;
            } else if (name.equalsIgnoreCase(DataType.POINT.name())) {
                productName = productMap.get(DataType.POINT);
                break;
            }
        }

        return productName;
    }

    /**
     * Extract the correct click level order list
     * 
     * @param selection
     * @return
     */
    private String[] extractOrder(String[] selection) {

        String[] order = null;
        // return default, point is default
        if (selection.length < 2) {
            return POINT_ORDER;
        } else {
            for (String name : selection) {
                if (name.equalsIgnoreCase(DataType.GRID.name())) {
                    order = GRID_ORDER;
                    break;
                } else if (name.equalsIgnoreCase(DataType.PDA.name())) {
                    order = PDA_ORDER;
                    break;
                } else if (name.equalsIgnoreCase(DataType.POINT.name())) {
                    order = POINT_ORDER;
                    break;
                }
            }
        }

        return order;
    }

}
