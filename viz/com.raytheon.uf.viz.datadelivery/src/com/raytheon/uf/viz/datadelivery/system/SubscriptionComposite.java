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

import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Combo;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Group;
import org.eclipse.swt.widgets.Label;

import com.raytheon.uf.common.auth.AuthException;
import com.raytheon.uf.common.auth.user.IUser;
import com.raytheon.uf.common.datadelivery.registry.DataType;
import com.raytheon.uf.common.datadelivery.request.DataDeliveryPermission;
import com.raytheon.uf.common.datadelivery.service.subscription.SubscriptionOverlapConfig;
import com.raytheon.uf.common.datadelivery.service.subscription.SubscriptionOverlapMatchStrategy;
import com.raytheon.uf.common.localization.exception.LocalizationException;
import com.raytheon.uf.common.status.IUFStatusHandler;
import com.raytheon.uf.common.status.UFStatus;
import com.raytheon.uf.common.status.UFStatus.Priority;
import com.raytheon.uf.viz.core.auth.UserController;
import com.raytheon.uf.viz.datadelivery.services.DataDeliveryServices;
import com.raytheon.viz.ui.widgets.ApplyCancelComposite;
import com.raytheon.viz.ui.widgets.IApplyCancelAction;

/**
 * Subscription settings composite.
 * 
 * <pre>
 * 
 * SOFTWARE HISTORY
 * 
 * Date         Ticket#    Engineer    Description
 * ------------ ---------- ----------- --------------------------
 * Aug 07, 2013   2180     mpduff      Initial creation.
 * Sept 24, 2013  2386     dhladky     Started work on Multiple Type Configs
 * Oct 03, 2013   2386     mpduff      Implemented multiple type configs
 * Mar 28, 2016   5482     randerso    Fixed GUI sizing issues
 * Dec 15, 2017   6467     tgurney     Check for permission before applying
 * 
 * </pre>
 * 
 * @author mpduff
 */

public abstract class SubscriptionComposite extends Composite implements
        IApplyCancelAction, IChangeApplier {

    /** Status Handler */
    private final IUFStatusHandler statusHandler = UFStatus
            .getHandler(SubscriptionComposite.class);

    /** Rules file manager */
    protected final SystemRuleManager ruleManager = SystemRuleManager
            .getInstance();

    /** The data type */
    protected final DataType DATA_TYPE;

    /** Match strategy combo. */
    protected Combo matchStrategyCombo;

    /** Button composite */
    protected ApplyCancelComposite buttonComp;

    /** Common rule attribute composite */
    protected CommonAttributeComposite commonComp;

    /**
     * Constructor.
     * 
     * @param parent
     *            Parent Composite
     * @param dataType
     *            Data type represented on this composite
     */
    public SubscriptionComposite(Composite parent, DataType dataType) {
        super(parent, SWT.NONE);
        this.DATA_TYPE = dataType;
        init();
    }

    /**
     * Initialize class.
     */
    protected void init() {
        GridLayout gl = new GridLayout(1, true);
        GridData gd = new GridData(SWT.FILL, SWT.FILL, true, true);
        this.setLayout(gl);
        this.setLayoutData(gd);

        gd = new GridData(SWT.FILL, SWT.DEFAULT, true, false);
        gl = new GridLayout(1, false);
        Composite configurationComposite = new Composite(this, SWT.NONE);
        configurationComposite.setLayout(gl);
        configurationComposite.setLayoutData(gd);

        gl = new GridLayout(1, false);
        gd = new GridData(SWT.FILL, SWT.DEFAULT, true, false);
        gl.marginHeight = 0;
        gl.marginWidth = 0;

        commonComp = new CommonAttributeComposite(configurationComposite,
                SWT.NONE, this);
        commonComp.setLayout(gl);
        commonComp.setLayoutData(gd);

        gl = new GridLayout(1, false);
        gd = new GridData(SWT.FILL, SWT.DEFAULT, true, false);
        Group grp = new Group(configurationComposite, SWT.NONE);
        grp.setLayout(gl);
        grp.setLayoutData(gd);

        initTypeSpecific(grp);

        // Match Strategy
        Composite matchComp = new Composite(this, SWT.NONE);
        matchComp.setLayout(gl);
        gl = new GridLayout(2, false);
        gd = new GridData(SWT.DEFAULT, SWT.DEFAULT, false, false);
        matchComp.setLayoutData(gd);

        Label matchLabel = new Label(matchComp, SWT.NONE);
        gd = new GridData(SWT.DEFAULT, SWT.DEFAULT);
        matchLabel.setLayoutData(gd);
        matchLabel.setText("Match:");

        matchStrategyCombo = new Combo(matchComp, SWT.READ_ONLY);
        gd = new GridData(SWT.DEFAULT, SWT.DEFAULT);
        matchStrategyCombo.setLayoutData(gd);
        matchStrategyCombo
                .setToolTipText("Select the manner in which the rules should consider two subscriptions to overlap");
        matchStrategyCombo.addSelectionListener(new SelectionAdapter() {
            @Override
            public void widgetSelected(SelectionEvent e) {
                applyChange();
            }

        });
        for (SubscriptionOverlapMatchStrategy matchStrategy : SubscriptionOverlapMatchStrategy
                .values()) {
            matchStrategyCombo.add(matchStrategy.getDisplayString());
            matchStrategyCombo.setData(matchStrategy.getDisplayString(),
                    matchStrategy);
        }
        matchStrategyCombo.select(0);
        // Buttons
        buttonComp = new ApplyCancelComposite(this, SWT.NONE, this);

        loadConfiguration();
    }

    /**
     * Initialize the type specific attributes.
     * 
     * @param The
     *            parent group
     */
    abstract void initTypeSpecific(Group grp);

    /**
     * Load configuration data
     */
    protected void loadConfiguration() {
        SubscriptionOverlapConfig config = ruleManager
                .getSubscriptionOverlapRules(DATA_TYPE);
        if (config != null) {
            commonComp.setParameterValue(config
                    .getMaxAllowedParameterDuplication());
            commonComp
                    .setSpatialValue(config.getMaxAllowedSpatialDuplication());

            final int indexOfConfigValue = matchStrategyCombo.indexOf(config
                    .getMatchStrategy().getDisplayString());
            matchStrategyCombo.select(indexOfConfigValue);
        }
    }

    /**
     * Save configuration data
     * 
     * @return true if saved
     * @throws LocalizationException
     */
    abstract boolean saveConfiguration() throws LocalizationException;


    @Override
    public boolean apply() {
        try {
            DataDeliveryPermission perm = DataDeliveryPermission.SYSTEM_MANAGEMENT_CREATE;
            final IUser user = UserController.getUserObject();
            final String msg = user.uniqueId()
                    + " is not authorized to create, edit or delete rules."
                    + "\nPermission: " + perm.toString();
            if (DataDeliveryServices.getPermissionsService()
                    .checkPermissions(user, msg, perm.toString())
                    .hasPermission(perm.toString())) {
                return saveConfiguration();
            }
            return true;
        } catch (LocalizationException | AuthException e) {
            statusHandler.handle(Priority.ERROR,
                    "Unable to save configuration changes.", e);
        }
        return false;
    }

    @Override
    public void cancel() {
        loadConfiguration();
        buttonComp.enableButtons(false);
    }

    protected void setButtonsEnabled() {
        buttonComp.enableButtons(true);
    }

    @Override
    public void applyChange() {
        setButtonsEnabled();
    }

    /**
     * Apply all common attributes.
     * 
     * @param dataType
     *            The current data type
     * 
     * @return true if saved correctly
     */
    protected boolean applyAll(DataType dataType) {
        SubscriptionOverlapConfig commonConfig = ruleManager
                .getSubscriptionOverlapRules(dataType);
        for (DataType dt : DataType.values()) {
            if (dt == DataType.PDA) {
                // TODO implement PDA
                continue;
            }
            if (dt != dataType) {
                SubscriptionOverlapConfig config = ruleManager
                        .getSubscriptionOverlapRules(dt);
                config.setMaxAllowedParameterDuplication(commonConfig
                        .getMaxAllowedParameterDuplication());
                config.setMaxAllowedSpatialDuplication(commonConfig
                        .getMaxAllowedSpatialDuplication());
                if (!ruleManager.saveOverlapRule(config, dt)) {
                    return false;
                }
            }
        }

        return true;
    }
}