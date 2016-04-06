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
package com.raytheon.uf.viz.datadelivery.subscription.subset;

import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.graphics.GC;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.List;
import org.eclipse.swt.widgets.Text;

import com.raytheon.uf.common.datadelivery.registry.DataType;
import com.raytheon.uf.common.localization.LocalizationFile;
import com.raytheon.uf.viz.datadelivery.subscription.subset.xml.SubsetXML;
import com.raytheon.uf.viz.datadelivery.utils.DataDeliveryUtils;

/**
 * The Saved Subsets tab.
 * 
 * <pre>
 * 
 * SOFTWARE HISTORY
 * 
 * Date         Ticket#    Engineer    Description
 * ------------ ---------- ----------- --------------------------
 * Mar 30, 2012            mpduff      Initial creation.
 * Jun  4, 2012    645     jpiatt      Added tooltips.
 * Nov  1, 2012   1278     mpduff      Formatted to meet coding standard.
 * Apr 10, 2014   2864     mpduff      Changed how saved subset files are stored.
 * Oct 19, 2015   4996     dhladky     Fixed message dialog call for subsets.
 * Oct 20, 2015   4992     dhladky     Added OK button message at tester request.
 * Mar 28, 2016  5482      randerso    Fixed GUI sizing issues
 * 
 * </pre>
 * 
 * @author mpduff
 * @version 1.0
 */

public class SavedSubsetTab extends SubsetTab {

    /** Parent composite */
    private final Composite comp;

    /** Subset list */
    private List subsetList;

    /** ITabAction callback */
    private final ITabAction callback;

    /** Load button */
    private Button loadBtn;

    /** Delete button */
    private Button deleteBtn;

    /** File extension */
    String extension = ".xml";

    /** The type of data loaded in the dialog */
    private final DataType dataType;

    /**
     * Constructor.
     * 
     * @param comp
     *            Composite holding these controls
     * @param dataType
     *            The datatype of the loaded data
     * @param callback
     *            The class for callbacks
     */
    public SavedSubsetTab(Composite comp, DataType dataType, ITabAction callback) {
        this.comp = comp;
        this.callback = callback;
        this.dataType = dataType;

        init();
    }

    /**
     * Initialize components
     */
    private void init() {
        GridLayout gl = new GridLayout(1, false);
        comp.setLayout(gl);
        GridData gd = new GridData(SWT.FILL, SWT.DEFAULT, true, false);
        comp.setLayoutData(gd);

        subsetList = new List(comp, SWT.MULTI | SWT.BORDER | SWT.V_SCROLL);
        GC gc = new GC(subsetList);
        int textWidth = gc.getFontMetrics().getAverageCharWidth() * 45;
        gc.dispose();
        gd = new GridData(SWT.FILL, SWT.FILL, true, true);
        gd.widthHint = textWidth;
        subsetList.setLayoutData(gd);

        Composite btnComp = new Composite(comp, SWT.NONE);
        gl = new GridLayout(3, true);
        btnComp.setLayout(gl);
        gd = new GridData(SWT.CENTER, SWT.DEFAULT, true, false);
        btnComp.setLayoutData(gd);

        int buttonWidth = btnComp.getDisplay().getDPI().x;
        loadBtn = new Button(btnComp, SWT.PUSH);
        loadBtn.setText("Load");
        gd = new GridData(SWT.DEFAULT, SWT.DEFAULT, true, false);
        gd.minimumWidth = buttonWidth;
        loadBtn.setLayoutData(gd);
        loadBtn.setToolTipText("Highlight subset and click to load");
        loadBtn.addSelectionListener(new SelectionAdapter() {
            @Override
            public void widgetSelected(SelectionEvent e) {
                handleLoadSubset();
            }
        });

        Button saveBtn = new Button(btnComp, SWT.PUSH);
        saveBtn.setText("Save");
        gd = new GridData(SWT.DEFAULT, SWT.DEFAULT, true, false);
        gd.minimumWidth = buttonWidth;
        saveBtn.setLayoutData(gd);
        saveBtn.setToolTipText("Highlight subset and click to save");
        saveBtn.addSelectionListener(new SelectionAdapter() {
            @Override
            public void widgetSelected(SelectionEvent e) {
                handleSaveSubset();
            }
        });

        deleteBtn = new Button(btnComp, SWT.PUSH);
        deleteBtn.setText("Delete");
        gd = new GridData(SWT.DEFAULT, SWT.DEFAULT, true, false);
        gd.minimumWidth = buttonWidth;
        deleteBtn.setLayoutData(gd);
        deleteBtn.setToolTipText("Highlight subset and click to delete");
        deleteBtn.addSelectionListener(new SelectionAdapter() {
            @Override
            public void widgetSelected(SelectionEvent e) {
                handleDeleteSubset();
            }
        });

        loadList();

        disableButtons();

    }

    private void loadList() {
        subsetList.removeAll();
        // Get the subset data
        for (LocalizationFile locFile : SubsetFileManager.getInstance()
                .getLocalizationFiles(this.dataType)) {
            String locFileName = locFile.getFile().getName();
            subsetList.add(SubsetXML.getBaseSubsetName(locFileName));
        }
    }

    private void handleDeleteSubset() {
        if (subsetList.getSelectionCount() > 0) {
            int response = DataDeliveryUtils.showMessageNonCallback(
                    comp.getShell(), SWT.YES | SWT.NO, "Delete Subset?",
                    "Are you sure you want to delete this subset?");
            String subsetName = subsetList.getItem(subsetList
                    .getSelectionIndex());
            subsetName = subsetName + extension;
            if (response == SWT.YES) {
                SubsetFileManager.getInstance().deleteSubset(subsetName,
                        this.dataType);
                loadList();
            }
        } else {
            DataDeliveryUtils.showMessageOk(comp.getShell(),
                    "No Subset Selection", "Please select a Subset to Delete.");
        }

        disableButtons();

    }

    private void handleLoadSubset() {
        if (subsetList.getSelectionCount() > 0) {
            String subsetName = subsetList.getItem(subsetList
                    .getSelectionIndex());
            subsetName = subsetName + extension;
            callback.handleLoadSubset(subsetName);
        } else {
            DataDeliveryUtils.showMessageOk(comp.getShell(),
                    "No Subset Selection", "Please select a Subset to load.");
        }
    }

    private void handleSaveSubset() {
        callback.handleSaveSubset();
        loadList();
    }

    /**
     * Enable buttons.
     * 
     * @param name
     *            subset name
     */
    public void enableButtons(Text name) {

        if (name != null) {
            loadBtn.setEnabled(true);
            deleteBtn.setEnabled(true);
        }
    }

    /**
     * Disable buttons.
     */
    public void disableButtons() {

        if (subsetList.getItemCount() == 0) {
            loadBtn.setEnabled(false);
            deleteBtn.setEnabled(false);
        }
    }
}
