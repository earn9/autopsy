/*
 * Sample module in the public domain.  Feel free to use this as a template
 * for your modules.
 * 
 *  Contact: Brian Carrier [carrier <at> sleuthkit [dot] org]
 *
 *  This is free and unencumbered software released into the public domain.
 *  
 *  Anyone is free to copy, modify, publish, use, compile, sell, or
 *  distribute this software, either in source code form or as a compiled
 *  binary, for any purpose, commercial or non-commercial, and by any
 *  means.
 *  
 *  In jurisdictions that recognize copyright laws, the author or authors
 *  of this software dedicate any and all copyright interest in the
 *  software to the public domain. We make this dedication for the benefit
 *  of the public at large and to the detriment of our heirs and
 *  successors. We intend this dedication to be an overt act of
 *  relinquishment in perpetuity of all present and future rights to this
 *  software under copyright law.
 *  
 *  THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND,
 *  EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF
 *  MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT.
 *  IN NO EVENT SHALL THE AUTHORS BE LIABLE FOR ANY CLAIM, DAMAGES OR
 *  OTHER LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE,
 *  ARISING FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR
 *  OTHER DEALINGS IN THE SOFTWARE. 
 */
package org.sleuthkit.autopsy.examples;

import java.awt.Component;
import org.openide.nodes.Node;
import org.openide.util.lookup.ServiceProvider;
import org.sleuthkit.autopsy.corecomponentinterfaces.DataContentViewer;
import org.sleuthkit.datamodel.Content;
import org.sleuthkit.datamodel.TskCoreException;

/**
 * Sample module. Calculates the number of bytes with value 0x00 in the first
 * 1024-bytes. Displays the results in a label.
 */
/*
 * THis is commented out so that it is not displayed in the real UI, it is
 * compiled each time to ensure that it is compliant with the API.
 */
// @ServiceProvider(service = DataContentViewer.class)
class SampleContentViewer extends javax.swing.JPanel implements DataContentViewer {

    /**
     * Creates new form SampleContentViewer
     */
    public SampleContentViewer() {
        initComponents();
    }

    /**
     * This method is called from within the constructor to initialize the form.
     * WARNING: Do NOT modify this code. The content of this method is always
     * regenerated by the Form Editor.
     */
    @SuppressWarnings("unchecked")
    // <editor-fold defaultstate="collapsed" desc="Generated Code">//GEN-BEGIN:initComponents
    private void initComponents() {

        jLabel1 = new javax.swing.JLabel();

        org.openide.awt.Mnemonics.setLocalizedText(jLabel1, org.openide.util.NbBundle.getMessage(SampleContentViewer.class, "SampleContentViewer.jLabel1.text")); // NOI18N

        javax.swing.GroupLayout layout = new javax.swing.GroupLayout(this);
        this.setLayout(layout);
        layout.setHorizontalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(layout.createSequentialGroup()
                .addContainerGap()
                .addComponent(jLabel1)
                .addContainerGap(339, Short.MAX_VALUE))
        );
        layout.setVerticalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(layout.createSequentialGroup()
                .addGap(19, 19, 19)
                .addComponent(jLabel1)
                .addContainerGap(266, Short.MAX_VALUE))
        );
    }// </editor-fold>//GEN-END:initComponents
    // Variables declaration - do not modify//GEN-BEGIN:variables
    private javax.swing.JLabel jLabel1;
    // End of variables declaration//GEN-END:variables

    @Override
    public void setNode(Node selectedNode) {
        try {
            // reset
            if (selectedNode == null) {
                setText("");
                return;
            }

            Content content = selectedNode.getLookup().lookup(Content.class);
            if (content == null) {
                // non-content object passed in
                setText("");
                return;
            }

            setText("Doing Analysis");
            byte buffer[] = new byte[1024];
            int len = content.read(buffer, 0, 1024);
            int count = 0;
            for (int i = 0; i < len; i++) {
                if (buffer[i] == 0x00) {
                    count++;
                }
            }
            setText(count + " out of " + len + " bytes were 0x00");
        } catch (TskCoreException ex) {
            setText("Error reading file: " + ex.getLocalizedMessage());
        }
    }

    // set the text in the lable in the JPanel
    private void setText(String str) {
        jLabel1.setText(str);
    }

    @Override
    public String getTitle() {
        return "Sample";
    }

    @Override
    public String getToolTip() {
        return "Useless module";
    }

    @Override
    public DataContentViewer createInstance() {
        return new SampleContentViewer();
    }

    @Override
    public Component getComponent() {
        // we can do this because this class extends JPanel
        return this;
    }

    @Override
    public void resetComponent() {
        setText("");
    }

    @Override
    public boolean isSupported(Node node) {
        // get a Content datamodel object out of the node
        Content content = node.getLookup().lookup(Content.class);
        if (content == null) {
            return false;
        }

        // we only want files that are 1024 bytes or larger (for no good reason)
        if (content.getSize() < 1024) {
            return false;
        }
        return true;
    }

    @Override
    public int isPreferred(Node node) {
        // we return 1 since this module will operate on nearly all files
        return 1;
    }
}
