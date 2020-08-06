/*
 * Autopsy Forensic Browser
 *
 * Copyright 2020 Basis Technology Corp.
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
package org.sleuthkit.autopsy.casemodule.datasourcesummary;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import javax.swing.JLabel;
import javax.swing.table.AbstractTableModel;
import javax.swing.table.DefaultTableCellRenderer;
import org.openide.util.NbBundle.Messages;
import org.sleuthkit.autopsy.casemodule.Case;
import org.sleuthkit.datamodel.DataSource;

/**
 * A panel to display user activity.
 */
@Messages({
    "DataSourceSummaryUserActivityPanel_tab_title=User Activity",
    "DataSourceSummaryUserActivityPanel_TopProgramsTableModel_name_header=Program",
    "DataSourceSummaryUserActivityPanel_TopProgramsTableModel_folder_header=Folder",
    "DataSourceSummaryUserActivityPanel_TopProgramsTableModel_count_header=Run Times"
})
public class DataSourceSummaryUserActivityPanel extends javax.swing.JPanel {
    // Result returned for a data model if no data found.
    private static final Object[][] EMPTY_PAIRS = new Object[][]{};
    private static final int TOP_PROGS_COUNT = 10;
    
    private static final DefaultTableCellRenderer RIGHT_ALIGNED_RENDERER = new DefaultTableCellRenderer();
    
    static {
        RIGHT_ALIGNED_RENDERER.setHorizontalAlignment(JLabel.RIGHT);
    }
    
    private DataSource dataSource;
    
    /**
     * Creates new form DataSourceUserActivityPanel
     */
    public DataSourceSummaryUserActivityPanel() {
        initComponents();
        topProgramsTable.getTableHeader().setReorderingAllowed(false);
    }

    /**
     * The datasource currently used as the model in this panel.
     *
     * @return The datasource currently being used as the model in this panel.
     */
    public DataSource getDataSource() {
        return dataSource;
    }

    /**
     * Sets datasource to visualize in the panel.
     *
     * @param dataSource The datasource to use in this panel.
     */
    public void setDataSource(DataSource dataSource) {
        this.dataSource = dataSource;
        if (dataSource == null || !Case.isCaseOpen()) {
            updateTopPrograms(new TopProgramsModel(null));
        } else {
            updateTopPrograms(getTopProgramsModel(dataSource));
        }
    }
    
    /**
     * Updates the Top Programs Table in the gui.
     * @param data The data in Object[][] form to be used by the DefaultTableModel.
     */
    private void updateTopPrograms(TopProgramsModel model) {
        topProgramsTable.setModel(model);
        topProgramsTable.getColumnModel().getColumn(0).setPreferredWidth(230);
        topProgramsTable.getColumnModel().getColumn(1).setCellRenderer(RIGHT_ALIGNED_RENDERER);        
        this.repaint();
    }
    
    /**
     * The counts of top programs run.
     *
     * @param selectedDataSource The DataSource.
     *
     * @return The JTable data model of counts of program runs.
     */
    private static TopProgramsModel getTopProgramsModel(DataSource selectedDataSource) {
        List<DataSourceInfoUtilities.TopProgramsResult> topProgramList = 
                DataSourceInfoUtilities.getTopPrograms(selectedDataSource, TOP_PROGS_COUNT);
        
        if (topProgramList == null) {
            return new TopProgramsModel(null);
        } else {
            return new TopProgramsModel(topProgramList);
        }
    }
    
    private static class TopProgramsModel extends AbstractTableModel {
        // column headers for artifact counts table
        private static final String[] TOP_PROGS_COLUMN_HEADERS = new String[]{
            Bundle.DataSourceSummaryUserActivityPanel_TopProgramsTableModel_name_header(),
            Bundle.DataSourceSummaryUserActivityPanel_TopProgramsTableModel_folder_header(),
            Bundle.DataSourceSummaryUserActivityPanel_TopProgramsTableModel_count_header()
        };
    
        private final List<DataSourceInfoUtilities.TopProgramsResult> programResults;

        public TopProgramsModel(List<DataSourceInfoUtilities.TopProgramsResult> programResults) {
            this.programResults = programResults == null ? new ArrayList<>() : Collections.unmodifiableList(programResults);
        }

        @Override
        public String getColumnName(int column) {
            return column < 0 || column >= TOP_PROGS_COLUMN_HEADERS.length ? null : TOP_PROGS_COLUMN_HEADERS[column];
        }
        
        @Override
        public int getRowCount() {
            return programResults.size();
        }

        @Override
        public int getColumnCount() {
            return TOP_PROGS_COLUMN_HEADERS.length;
        }

        @Override
        public Object getValueAt(int rowIndex, int columnIndex) {
            if (rowIndex < 0 || rowIndex >= programResults.size()) {
                return null;
            }
            
            DataSourceInfoUtilities.TopProgramsResult result = programResults.get(rowIndex);
            switch (columnIndex) {
                case 0: return result.getProgramName();
                case 1: return DataSourceInfoUtilities.getShortFolderName(result.getProgramPath());
                case 2: return result.getRunTimes();
                default: return null;
            }
        }
        
    }
    
    
    /**
     * This method is called from within the constructor to initialize the form.
     * WARNING: Do NOT modify this code. The content of this method is always
     * regenerated by the Form Editor.
     */
    @SuppressWarnings("unchecked")
    // <editor-fold defaultstate="collapsed" desc="Generated Code">//GEN-BEGIN:initComponents
    private void initComponents() {

        javax.swing.JLabel programsRunLabel = new javax.swing.JLabel();
        javax.swing.JScrollPane topProgramsScrollPane = new javax.swing.JScrollPane();
        topProgramsTable = new javax.swing.JTable();

        setMinimumSize(new java.awt.Dimension(256, 300));

        org.openide.awt.Mnemonics.setLocalizedText(programsRunLabel, org.openide.util.NbBundle.getMessage(DataSourceSummaryUserActivityPanel.class, "DataSourceSummaryUserActivityPanel.programsRunLabel.text")); // NOI18N

        topProgramsScrollPane.setPreferredSize(new java.awt.Dimension(290, 187));
        topProgramsScrollPane.setViewportView(topProgramsTable);

        javax.swing.GroupLayout layout = new javax.swing.GroupLayout(this);
        this.setLayout(layout);
        layout.setHorizontalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(layout.createSequentialGroup()
                .addContainerGap()
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addComponent(programsRunLabel, javax.swing.GroupLayout.PREFERRED_SIZE, 155, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(topProgramsScrollPane, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addContainerGap(47, Short.MAX_VALUE))
        );
        layout.setVerticalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(layout.createSequentialGroup()
                .addContainerGap()
                .addComponent(programsRunLabel)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(topProgramsScrollPane, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addContainerGap(javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
        );
    }// </editor-fold>//GEN-END:initComponents


    // Variables declaration - do not modify//GEN-BEGIN:variables
    private javax.swing.JTable topProgramsTable;
    // End of variables declaration//GEN-END:variables
}
