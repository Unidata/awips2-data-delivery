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
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;

import org.eclipse.ui.IEditorPart;
import org.eclipse.ui.IWorkbenchWindow;

import com.raytheon.uf.common.datadelivery.registry.DataType;
import com.raytheon.uf.common.dataplugin.grid.GridConstants;
import com.raytheon.uf.common.dataplugin.grid.derivparam.CommonGridInventory;
import com.raytheon.uf.common.dataplugin.level.Level;
import com.raytheon.uf.common.dataplugin.level.LevelFactory;
import com.raytheon.uf.common.dataquery.requests.RequestConstraint;
import com.raytheon.uf.common.dataquery.requests.RequestConstraint.ConstraintType;
import com.raytheon.uf.common.status.IUFStatusHandler;
import com.raytheon.uf.common.status.UFStatus;
import com.raytheon.uf.common.status.UFStatus.Priority;
import com.raytheon.uf.common.time.BinOffset;
import com.raytheon.uf.viz.core.DescriptorMap;
import com.raytheon.uf.viz.core.IDisplayPaneContainer;
import com.raytheon.uf.viz.core.drawables.AbstractRenderableDisplay;
import com.raytheon.uf.viz.core.drawables.IDescriptor;
import com.raytheon.uf.viz.core.drawables.ResourcePair;
import com.raytheon.uf.viz.core.exception.VizException;
import com.raytheon.uf.viz.core.grid.rsc.GridLoadProperties;
import com.raytheon.uf.viz.core.map.MapDescriptor;
import com.raytheon.uf.viz.core.procedures.Bundle;
import com.raytheon.uf.viz.core.rsc.AbstractRequestableResourceData;
import com.raytheon.uf.viz.core.rsc.DisplayType;
import com.raytheon.uf.viz.core.rsc.LoadProperties;
import com.raytheon.uf.viz.core.rsc.ResourceProperties;
import com.raytheon.uf.viz.core.rsc.ResourceType;
import com.raytheon.uf.viz.core.rsc.capabilities.DisplayTypeCapability;
import com.raytheon.uf.viz.datacube.DataCubeContainer;
import com.raytheon.uf.viz.productbrowser.ProductBrowserLabel;
import com.raytheon.uf.viz.productbrowser.ProductBrowserPreference;
import com.raytheon.uf.viz.productbrowser.pref.PreferenceBasedDataDefinition;
import com.raytheon.viz.grid.GridProductBrowserDataFormatter;
import com.raytheon.viz.grid.rsc.GridResourceData;
import com.raytheon.viz.grid.xml.FieldDisplayTypesFactory;
import com.raytheon.viz.pointdata.PlotModels;
import com.raytheon.viz.pointdata.rsc.PlotResourceData;
import com.raytheon.viz.pointdata.util.PointDataInventory;
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
 * Mar 08, 2016  4621     tjensen   Added support for Derived Parameters for
 *                                  Grid products
 * Mar 16, 2016  3919     tjensen   Cleanup unneeded interfaces
 * May 04, 2016  5599     tjensen   Fixed PDA label population.
 * Aug 22, 2016  5843     tjensen   Remove PDA from DD Product Browser
 * Apr 03, 2018  7240     tjensen   Find all Grid data with DD prefix; Condense
 *                                  Point data; Cleanup
 *
 * </pre>
 *
 * @author mpduff
 */

public class DataDeliveryProductBrowserDataDefinition
        implements PreferenceBasedDataDefinition {

    private static final String DD_MODEL_PREFIX = "DD_";

    private final IUFStatusHandler statusHandler = UFStatus
            .getHandler(DataDeliveryProductBrowserDataDefinition.class);

    /** Constant for MADIS plugin lookups */
    private static final String MADIS = "madis";

    /** Constant for GRID plugin lookups */
    private static final String GRID = "grid";

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

    private static final String[] GRID_ORDER = new String[] {
            GridConstants.DATASET_ID, GridConstants.PARAMETER_ABBREVIATION,
            GridConstants.MASTER_LEVEL_NAME, GridConstants.LEVEL_ID };

    /** cache lookup plugins to DataDelivery names map **/
    private final Map<DataType, String> productMap;

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
        protoProductMap.put(DataType.POINT, POINT);
        productMap = Collections.unmodifiableMap(protoProductMap);
    }

    public List<ProductBrowserLabel> populateData(String[] selection) {

        if (selection.length == 1) {
            // Get provider names
            String[] dataTypes = getDataTypes();
            return formatData("Data Type", dataTypes, selection);
        }

        if (selection.length == 2) {
            if (selection[1].equalsIgnoreCase(DataType.GRID.name())) {
                /*
                 * Must remove the first selection so this matches with the grid
                 * version
                 */
                String[] usedSelection = realignSelection(selection);
                return populateGridUpperData(usedSelection);
            } else if (selection[1].equalsIgnoreCase(DataType.POINT.name())) {
                String[] order = extractOrder(selection);
                String[] results;
                try {
                    results = PlotModels.getInstance().getInventory()
                            .getAvailableTypes(MADIS);
                    String param = order[1];
                    return formatData(param, results, selection);
                } catch (VizException e) {
                    statusHandler.error("Error retrieving point data types", e);
                }
            }
        }

        if (selection.length == 3) {
            if (selection[1].equalsIgnoreCase(DataType.POINT.name())) {
                String[] order = extractOrder(selection);
                String[] results = PlotModels.getInstance().getLevels(MADIS,
                        SVG);
                String param = order[2];
                return formatData(param, results, selection);
            } else if (selection[1].equalsIgnoreCase(DataType.GRID.name())) {
                /*
                 * Must remove the first selection so this matches with the grid
                 * version
                 */
                String[] usedSelection = realignSelection(selection);
                return populateGridUpperData(usedSelection);

            }
        }

        if (selection.length >= 4) {
            if (selection[1].equalsIgnoreCase(DataType.GRID.name())) {
                /*
                 * Must remove the first selection so this matches with the grid
                 * version
                 */
                String[] usedSelection = realignSelection(selection);
                return populateGridUpperData(usedSelection);
            }
        }
        return null;
    }

    /**
     * Populate the Upper (Higher Level menus) of the product drop downs for
     * Grid data
     *
     * @param selection
     * @return
     */
    protected List<ProductBrowserLabel> populateGridUpperData(
            String[] selection) {

        List<ProductBrowserLabel> parameters = null;
        boolean product = false;
        String[] order = extractOrder(selection);

        String param = order[selection.length - 1];
        HashMap<String, RequestConstraint> queryList = getProductParameters(
                selection, order);
        product = selection.length == order.length;

        String[] temp = null;

        try {
            CommonGridInventory inventory = (CommonGridInventory) DataCubeContainer
                    .getInventory(GridConstants.GRID);
            BlockingQueue<String> returnQueue = new LinkedBlockingQueue<>();
            if (param.equals(GridConstants.DATASET_ID)) {
                queryList.put(GridConstants.DATASET_ID, new RequestConstraint(
                        DD_MODEL_PREFIX + "%", ConstraintType.LIKE));
                inventory.checkSources(queryList, returnQueue);
                temp = returnQueue.toArray(new String[0]);
            } else if (param.equals(GridConstants.PARAMETER_ABBREVIATION)) {
                inventory.checkParameters(queryList, false, returnQueue);
                temp = returnQueue.toArray(new String[0]);
            } else if (param.equals(GridConstants.MASTER_LEVEL_NAME)) {
                inventory.checkLevels(queryList, returnQueue);
                Set<String> masterlevels = new HashSet<>();
                LevelFactory lf = LevelFactory.getInstance();
                for (String levelid : returnQueue) {
                    Level level = lf.getLevel(levelid);
                    masterlevels.add(level.getMasterLevel().getName());
                }
                temp = masterlevels.toArray(new String[0]);
            } else if (param.equals(GridConstants.LEVEL_ID)) {
                inventory.checkLevels(queryList, returnQueue);
                temp = returnQueue.toArray(new String[0]);
            }
        } catch (InterruptedException e) {
            statusHandler.handle(Priority.WARN,
                    "Unable to determine available menu data for " + product);
        }

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
     * product order arrays (GRID_ORDER, etc) Meaning: You have to back off one
     * selection index in order to process the correct index of the selection
     * array to produce the correct resource.
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
        }
        /* Data Type or point data. */
        List<ProductBrowserLabel> temp = new ArrayList<>();
        String[] order = extractOrder(selection);
        for (String parameter : parameters) {
            ProductBrowserLabel label = new ProductBrowserLabel(parameter,
                    parameter);
            temp.add(label);
            if (order == null || !param.equals(order[order.length - 1])) {
                label.setProduct(false);
            } else {
                label.setProduct(true);
            }
            label.setData(parameter);
        }
        Collections.sort(temp);
        return temp;
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
        for (String parameter : parameters) {
            temp.add(new ProductBrowserLabel(parameter, null));
        }
        Collections.sort(temp);
        return temp;
    }

    public AbstractRequestableResourceData getResourceData(String[] selection) {

        AbstractRequestableResourceData resourceData = null;
        String selectedDataType = selection[1];

        if (selectedDataType.equalsIgnoreCase(DataType.POINT.name())) {
            PlotResourceData plotResourceData = new PlotResourceData();
            plotResourceData
                    .setBinOffset(new BinOffset(frameOffset, frameOffset));
            plotResourceData.setRetrieveData(false);
            plotResourceData.setUpdatingOnMetadataOnly(true);
            plotResourceData.setTopOfTheHour(false);
            plotResourceData.setPlotSource(selection[selection.length - 3] + " "
                    + selection[selection.length - 2] + " "
                    + selection[selection.length - 1]);
            plotResourceData.setPlotModelFile(SVG);
            plotResourceData.setLevelKey(selection[selection.length - 1]);
            resourceData = plotResourceData;
        } else if (selectedDataType.equalsIgnoreCase(DataType.GRID.name())) {
            resourceData = new GridResourceData();
        }

        return resourceData;
    }

    public HashMap<String, RequestConstraint> getProductParameters(
            String[] selection, String[] order) {

        String productName = extractProductName(selection);

        if (productName.equalsIgnoreCase(DataType.POINT.name())) {
            return getPointProductParameters(selection, order);
        } else if (productName.equalsIgnoreCase(DataType.GRID.name())) {
            return getGridProductParameters(selection, order);
        } else {
            throw new IllegalArgumentException(
                    "Invalid data type: " + productName);
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
        }
        return getUpperProductParameters(selection, order);
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
            queryList.put(inv.getTypeKey(selection[1]),
                    new RequestConstraint(selection[2]));
        }

        return queryList;
    }

    /**
     * Extract data types from the subscriptions listed
     *
     * @return string[]
     */
    @SuppressWarnings("rawtypes")
    private String[] getDataTypes() {
        List<String> dataTypes = new ArrayList<>();
        for (String type : productMap.values()) {
            dataTypes.add(type.substring(0, 1).toUpperCase()
                    + type.substring(1).toLowerCase());
        }
        return dataTypes.toArray(new String[dataTypes.size()]);
    }

    protected Map<String, String> createKeyValMap(String[] selection) {
        Map<String, String> keyVals = new HashMap<>();
        int index = 2;
        String[] order = extractOrder(selection);
        for (String element : order) {
            if (index < selection.length) {
                keyVals.put(element, selection[index]);
                index += 1;
            } else {
                break;
            }
        }
        return keyVals;
    }

    /**
     * Determine valid display types depending on the DataType of the selection
     *
     * @param selection
     * @return
     */
    public Map<ResourceType, List<DisplayType>> getDisplayTypes(
            String[] selection) {
        Map<String, String> keyValMap = createKeyValMap(selection);

        for (String selectedDataType : selection) {
            if (selectedDataType.equalsIgnoreCase(DataType.GRID.name())) {
                Map<ResourceType, List<DisplayType>> type = new HashMap<>();
                List<DisplayType> types = new ArrayList<>();

                if (keyValMap
                        .containsKey(GridConstants.PARAMETER_ABBREVIATION)) {
                    types = FieldDisplayTypesFactory.getInstance()
                            .getDisplayTypes(keyValMap
                                    .get(GridConstants.PARAMETER_ABBREVIATION));
                } else {
                    // Default to these
                    types.add(DisplayType.CONTOUR);
                    types.add(DisplayType.IMAGE);
                }

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
        if (editorPart != null
                && id.equals(editorPart.getEditorSite().getId())) {
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
        AbstractVizPerspectiveManager mgr = VizPerspectiveListener
                .getInstance(window).getActivePerspectiveManager();
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
        Map<ResourceType, List<DisplayType>> displayTypeMap = getDisplayTypes(
                selection);
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
        AbstractRequestableResourceData resourceData = getResourceData(
                selection);

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
            ProductBrowserLabel label = new ProductBrowserLabel(DATA_DELIVERY,
                    DATA_DELIVERY);
            label.setData(DATA_DELIVERY);
            label.setProduct(false);
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

        for (String name : selection) {
            if (name.equalsIgnoreCase(DataType.GRID.name())) {
                order = GRID_ORDER;
                break;
            } else if (name.equalsIgnoreCase(DataType.POINT.name())) {
                order = POINT_ORDER;
                break;
            }
        }

        return order;
    }

}
