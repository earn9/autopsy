/*
 * Autopsy Forensic Browser
 *
 * Copyright 2011-2020 Basis Technology Corp.
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
package org.sleuthkit.autopsy.keywordsearch;

import javax.swing.JLabel;
import javax.swing.JPanel;
import org.openide.util.NbBundle;

/**
 * This panel shows current text size and allows for increasing or decreasing font size.
 */
class TextZoomPanel extends JPanel {
    static final int DEFAULT_SIZE = new JLabel().getFont().getSize();
    
    // How much font size is incremented or decremented when zooming in or zooming out respectively.
    private static final int FONT_INCREMENT_DELTA = 1;
    
    private static final double[] ZOOM_STEPS = {
        0.0625, 0.125, 0.25, 0.375, 0.5, 0.75,
        1, 1.5, 2, 2.5, 3, 4, 5, 6, 8, 10};
    
    // Identifies the center index in zoom steps (what identifies 100%).
    private static final int DEFAULT_STEP_IDX = 6;
    
    // The component to receive zoom updates.
    private final ResizableTextPanel zoomable;
    
    // On initialization, set to 100%.
    private int curStepIndex = DEFAULT_STEP_IDX;
    
    
    /**
     * Creates new form TextZoomPanel.
     * @param zoomable      the component that will receive text resize events
     */
    TextZoomPanel(ResizableTextPanel zoomable) {
        this.zoomable = zoomable;
        initComponents();
        updateEnabled();
        setZoomText();
    }

    
    private void updateEnabled() {
        boolean shouldEnable = this.zoomable != null;
        this.zoomInButton.setEnabled(shouldEnable);
        this.zoomOutButton.setEnabled(shouldEnable);
        this.zoomResetButton.setEnabled(shouldEnable);
        this.zoomTextField.setEnabled(shouldEnable);
    }
    
    /**
     * resets the font size displayed and triggers the ResizableTextPanel to
     * set their font to default size (i.e. JLabel().getFont().getSize())
     */
    synchronized void resetSize() {
        zoomStep(DEFAULT_STEP_IDX);
    }
    
    private synchronized void zoomStep(int newStep) {
        if (this.zoomable != null && newStep >= 0 && newStep < ZOOM_STEPS.length) {
            curStepIndex = newStep;
            zoomable.setTextSize((int)Math.round(ZOOM_STEPS[curStepIndex] * (double)DEFAULT_SIZE));
            setZoomText();
        }
    }
    
    private synchronized void zoomDecrement() {
        zoomStep(curStepIndex - 1);
    }
    
    private synchronized void zoomIncrement() {
        zoomStep(curStepIndex + 1);
    }
    
    private void setZoomText() {
        String percent = Long.toString(Math.round(ZOOM_STEPS[this.curStepIndex] * 100));
        zoomTextField.setText(percent + "%");
    }
    
    
    
    @NbBundle.Messages({
        "TextZoomPanel.zoomTextField.text=",
        "TextZoomPanel.zoomOutButton.text=",
        "TextZoomPanel.zoomInButton.text=",
        "TextZoomPanel.zoomResetButton.text=Reset"
    })
    /**
     * This method is called from within the constructor to initialize the form.
     * WARNING: Do NOT modify this code. The content of this method is always
     * regenerated by the Form Editor.
     */
    @SuppressWarnings("unchecked")
    // <editor-fold defaultstate="collapsed" desc="Generated Code">//GEN-BEGIN:initComponents
    private void initComponents() {

        zoomTextField = new javax.swing.JTextField();
        jSeparator1 = new javax.swing.JToolBar.Separator();
        zoomOutButton = new javax.swing.JButton();
        zoomInButton = new javax.swing.JButton();
        jSeparator2 = new javax.swing.JToolBar.Separator();
        zoomResetButton = new javax.swing.JButton();
        filler1 = new javax.swing.Box.Filler(new java.awt.Dimension(0, 0), new java.awt.Dimension(0, 0), new java.awt.Dimension(0, 0));

        setMinimumSize(new java.awt.Dimension(150, 20));
        setPreferredSize(new java.awt.Dimension(200, 20));
        setRequestFocusEnabled(false);
        setLayout(new java.awt.FlowLayout(java.awt.FlowLayout.LEFT, 0, 0));

        zoomTextField.setEditable(false);
        zoomTextField.setHorizontalAlignment(javax.swing.JTextField.RIGHT);
        zoomTextField.setText(org.openide.util.NbBundle.getMessage(TextZoomPanel.class, "TextZoomPanel.zoomTextField.text")); // NOI18N
        zoomTextField.setMaximumSize(new java.awt.Dimension(50, 2147483647));
        zoomTextField.setMinimumSize(new java.awt.Dimension(50, 20));
        zoomTextField.setPreferredSize(new java.awt.Dimension(50, 20));
        add(zoomTextField);

        jSeparator1.setMaximumSize(new java.awt.Dimension(6, 20));
        add(jSeparator1);

        zoomOutButton.setIcon(new javax.swing.ImageIcon(getClass().getResource("/org/sleuthkit/autopsy/keywordsearch/zoom-out.png"))); // NOI18N
        org.openide.awt.Mnemonics.setLocalizedText(zoomOutButton, org.openide.util.NbBundle.getMessage(TextZoomPanel.class, "TextZoomPanel.zoomOutButton.text")); // NOI18N
        zoomOutButton.setBorderPainted(false);
        zoomOutButton.setFocusable(false);
        zoomOutButton.setHorizontalTextPosition(javax.swing.SwingConstants.CENTER);
        zoomOutButton.setMaximumSize(new java.awt.Dimension(24, 24));
        zoomOutButton.setMinimumSize(new java.awt.Dimension(24, 24));
        zoomOutButton.setPreferredSize(new java.awt.Dimension(24, 24));
        zoomOutButton.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                zoomOutButtonActionPerformed(evt);
            }
        });
        add(zoomOutButton);

        zoomInButton.setIcon(new javax.swing.ImageIcon(getClass().getResource("/org/sleuthkit/autopsy/keywordsearch/zoom-in.png"))); // NOI18N
        org.openide.awt.Mnemonics.setLocalizedText(zoomInButton, org.openide.util.NbBundle.getMessage(TextZoomPanel.class, "TextZoomPanel.zoomInButton.text")); // NOI18N
        zoomInButton.setBorderPainted(false);
        zoomInButton.setFocusable(false);
        zoomInButton.setHorizontalTextPosition(javax.swing.SwingConstants.CENTER);
        zoomInButton.setMaximumSize(new java.awt.Dimension(24, 24));
        zoomInButton.setMinimumSize(new java.awt.Dimension(24, 24));
        zoomInButton.setPreferredSize(new java.awt.Dimension(24, 24));
        zoomInButton.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                zoomInButtonActionPerformed(evt);
            }
        });
        add(zoomInButton);

        jSeparator2.setOrientation(javax.swing.SwingConstants.VERTICAL);
        jSeparator2.setMaximumSize(new java.awt.Dimension(6, 20));
        add(jSeparator2);

        org.openide.awt.Mnemonics.setLocalizedText(zoomResetButton, org.openide.util.NbBundle.getMessage(TextZoomPanel.class, "TextZoomPanel.zoomResetButton.text")); // NOI18N
        zoomResetButton.setBorderPainted(false);
        zoomResetButton.setFocusable(false);
        zoomResetButton.setHorizontalTextPosition(javax.swing.SwingConstants.CENTER);
        zoomResetButton.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                zoomResetButtonActionPerformed(evt);
            }
        });
        add(zoomResetButton);
        add(filler1);
    }// </editor-fold>//GEN-END:initComponents

    private void zoomOutButtonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_zoomOutButtonActionPerformed
        zoomDecrement();
    }//GEN-LAST:event_zoomOutButtonActionPerformed

    private void zoomInButtonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_zoomInButtonActionPerformed
        zoomIncrement();
    }//GEN-LAST:event_zoomInButtonActionPerformed

    private void zoomResetButtonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_zoomResetButtonActionPerformed
        resetSize();
    }//GEN-LAST:event_zoomResetButtonActionPerformed


    // Variables declaration - do not modify//GEN-BEGIN:variables
    private javax.swing.Box.Filler filler1;
    private javax.swing.JToolBar.Separator jSeparator1;
    private javax.swing.JToolBar.Separator jSeparator2;
    private javax.swing.JButton zoomInButton;
    private javax.swing.JButton zoomOutButton;
    private javax.swing.JButton zoomResetButton;
    private javax.swing.JTextField zoomTextField;
    // End of variables declaration//GEN-END:variables
}
