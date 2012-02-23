/*
 * Autopsy Forensic Browser
 *
 * Copyright 2011 Basis Technology Corp.
 * Contact: carrier <at> sleuthkit <dot> org
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.sleuthkit.autopsy.ingest;

import java.awt.BorderLayout;
import java.awt.Color;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.util.ArrayList;
import java.util.logging.Logger;
import javax.swing.Action;
import javax.swing.JOptionPane;
import javax.swing.SwingUtilities;
import org.openide.util.ImageUtilities;
import org.openide.util.NbBundle;
import org.openide.windows.Mode;
import org.openide.windows.TopComponent;
import org.openide.windows.WindowManager;
import org.sleuthkit.autopsy.casemodule.Case;
import org.sleuthkit.datamodel.Image;

/**
 * Top component which displays something.
 */
public final class IngestMessageTopComponent extends TopComponent implements IngestUI {

    private static IngestMessageTopComponent instance;
    private static final Logger logger = Logger.getLogger(IngestMessageTopComponent.class.getName());
    private IngestMessageMainPanel messagePanel;
    private IngestManager manager;
    private static String PREFERRED_ID = "IngestMessageTopComponent";

    public IngestMessageTopComponent() {
        initComponents();
        customizeComponents();
        registerListeners();
        setName(NbBundle.getMessage(IngestMessageTopComponent.class, "CTL_IngestMessageTopComponent"));
        setToolTipText(NbBundle.getMessage(IngestMessageTopComponent.class, "HINT_IngestMessageTopComponent"));
        //putClientProperty(TopComponent.PROP_CLOSING_DISABLED, Boolean.TRUE);

    }

    private static synchronized IngestMessageTopComponent getDefault() {
        if (instance == null) {
            instance = new IngestMessageTopComponent();
        }
        return instance;
    }

    public static synchronized IngestMessageTopComponent findInstance() {
        TopComponent win = WindowManager.getDefault().findTopComponent(PREFERRED_ID);
        if (win == null) {
            return getDefault();
        }
        if (win instanceof IngestMessageTopComponent) {
            return (IngestMessageTopComponent) win;
        }

        return getDefault();
    }

    /** This method is called from within the constructor to
     * initialize the form.
     * WARNING: Do NOT modify this code. The content of this method is
     * always regenerated by the Form Editor.
     */
    // <editor-fold defaultstate="collapsed" desc="Generated Code">//GEN-BEGIN:initComponents
    private void initComponents() {

        javax.swing.GroupLayout layout = new javax.swing.GroupLayout(this);
        this.setLayout(layout);
        layout.setHorizontalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGap(0, 332, Short.MAX_VALUE)
        );
        layout.setVerticalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGap(0, 210, Short.MAX_VALUE)
        );
    }// </editor-fold>//GEN-END:initComponents
    // Variables declaration - do not modify//GEN-BEGIN:variables
    // End of variables declaration//GEN-END:variables

    @Override
    public void componentOpened() {
        //logger.log(Level.INFO, "OPENED");
        //create manager instance
        if (manager == null) {
            manager = IngestManager.getDefault();
        }
        
        //messagePanel.setSize(this.getSize());
    }

    @Override
    public void componentClosed() {
        //logger.log(Level.INFO, "CLOSED");
        Mode mode = WindowManager.getDefault().findMode("dockedBottom");
        if (mode != null) {
            mode.dockInto(this);
            this.open();
        }
    }

    @Override
    protected void componentShowing() {
        //logger.log(Level.INFO, "SHOWING");

        Mode mode = WindowManager.getDefault().findMode("floatingLeftBottom");
        if (mode != null) {
            TopComponent[] tcs = mode.getTopComponents();
            for (int i = 0; i < tcs.length; ++i) {
                if (tcs[i] == this) //already floating
                {
                    return;
                }
            }
            mode.dockInto(this);
            this.open();
        }
        
        //messagePanel.setSize(this.getSize());
    }

    @Override
    protected void componentHidden() {
        //logger.log(Level.INFO, "HIDDEN");
        super.componentHidden();

    }

    @Override
    protected void componentActivated() {
        //logger.log(Level.INFO, "ACTIVATED");
        super.componentActivated();
    }

    @Override
    protected void componentDeactivated() {
        //logger.log(Level.INFO, "DEACTIVATED");
        super.componentDeactivated();
    }

    @Override
    public boolean canClose() {
        return true;
    }

    @Override
    public int getPersistenceType() {
        return TopComponent.PERSISTENCE_NEVER;
    }

    @Override
    public java.awt.Image getIcon() {
        return ImageUtilities.loadImage(
                "org/sleuthkit/autopsy/ingest/ingest-msg-icon.png");
    }

    void writeProperties(java.util.Properties p) {
        // better to version settings since initial version as advocated at
        // http://wiki.apidesign.org/wiki/PropertyFiles
        p.setProperty("version", "1.0");
        // TODO store your settings
    }

    void readProperties(java.util.Properties p) {
        String version = p.getProperty("version");
        // TODO read your settings according to their version
    }

    private void registerListeners() {
        //handle case change
        Case.addPropertyChangeListener(new PropertyChangeListener() {

            @Override
            public void propertyChange(PropertyChangeEvent evt) {
                if (evt.getPropertyName().equals(Case.CASE_CURRENT_CASE)) {
                    Case oldCase = (Case) evt.getOldValue();
                    if (oldCase == null) //nothing to do, new case had been opened
                    {
                        return;
                    }
                    //stop workers if running
                    if (manager == null) {
                        manager = IngestManager.getDefault();
                    }
                    manager.stopAll();
                    //clear inbox 
                    clearMessages();
                } else if (evt.getPropertyName().equals(Case.CASE_ADD_IMAGE)) {
                    final Image image = (Image) evt.getNewValue();
                    SwingUtilities.invokeLater(new Runnable() {

                        @Override
                        public void run() {
                            displayIngestDialog(image);
                        }
                    });

                }
            }
        });
    }

    private void customizeComponents() {
        //custom GUI setup not done by builder
        messagePanel = new IngestMessageMainPanel();
        messagePanel.setOpaque(true);
        setLayout(new BorderLayout());
        add(messagePanel, BorderLayout.CENTER);
        
        //this.setBackground(Color.yellow);
    }

    /**
     * Display ingest summary report in some dialog
     */
    @Override
    public void displayReport(String ingestReport) {
        JOptionPane.showMessageDialog(
                null,
                ingestReport,
                "File Ingest Summary",
                JOptionPane.INFORMATION_MESSAGE);
    }

    /**
     * Display IngestMessage from service (forwarded by IngestManager)
     */
    @Override
    public void displayMessage(IngestMessage ingestMessage) {
        messagePanel.addMessage(ingestMessage);
    }

    @Override
    public void initProgress(int maximum) {
    }

    @Override
    public void updateProgress(int progress) {
    }

    @Override
    public void clearMessages() {
        messagePanel.clearMessages();
    }

    @Override
    public void displayIngestDialog(final Image image) {
        final IngestDialog ingestDialog = new IngestDialog();
        ingestDialog.setImage(image);
        ingestDialog.display();

    }

    @Override
    public void restoreMessages() {
        //componentShowing();
    }

    @Override
    public Action[] getActions() {
        //disable TC toolbar actions
        ArrayList actions = new ArrayList();
        Action[] retVal = new Action[actions.size()];
        actions.toArray(retVal);
        return retVal;
    }
}
