package com.raytheon.uf.viz.datadelivery.utils;

import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.StyledText;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.graphics.GC;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Shell;

import com.raytheon.viz.ui.dialogs.CaveSWTDialog;

/**
 * Replace a message box for messages with too big number of lines.
 * 
 * <pre>
 * 
 * SOFTWARE HISTORY
 * 
 * Date         Ticket#    Engineer    Description
 * ------------ ---------- ----------- --------------------------
 * Nov 7, 2013             skorolev    Initial creation
 * Jan 20, 2013   2291     lvenable    Fixed resizing of components.
 * Mar 28, 2016   5482     randerso    Fixed GUI sizing issues
 * 
 * </pre>
 * 
 * @author skorolev
 * @version 1.0
 */
public class TextMessageDlg extends CaveSWTDialog {

    private String messageText;

    protected TextMessageDlg(Shell parentShell, String messageTitle,
            String messageText) {
        super(parentShell, SWT.DIALOG_TRIM | SWT.RESIZE, CAVE.DO_NOT_BLOCK);
        this.messageText = messageText;
        this.setText(messageTitle);
    }

    @Override
    protected void initializeComponents(Shell shell) {
        GridData gd = new GridData(SWT.FILL, SWT.FILL, true, true);
        GridLayout gl = new GridLayout(1, false);
        Composite mainComp = new Composite(shell, SWT.NONE);
        mainComp.setLayout(gl);
        mainComp.setLayoutData(gd);

        StyledText text = new StyledText(mainComp, SWT.MULTI | SWT.WRAP
                | SWT.BORDER | SWT.H_SCROLL | SWT.V_SCROLL);
        GC gc = new GC(text);
        int textWidth = gc.getFontMetrics().getAverageCharWidth() * 65;
        gc.dispose();

        gd = new GridData(SWT.FILL, SWT.FILL, true, true);
        gd.widthHint = textWidth;
        gd.heightHint = text.getLineHeight() * 25;
        text.setLayoutData(gd);
        text.setText(messageText);

        gd = new GridData(SWT.CENTER, SWT.DEFAULT, true, false);
        gd.minimumWidth = mainComp.getDisplay().getDPI().x;
        Button okBtn = new Button(mainComp, SWT.PUSH);
        okBtn.setText("OK");
        okBtn.setLayoutData(gd);
        okBtn.setLayoutData(gd);
        okBtn.addSelectionListener(new SelectionAdapter() {
            @Override
            public void widgetSelected(SelectionEvent e) {
                close();
            }
        });
    }
}
