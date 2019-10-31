/*
 * Autopsy Forensic Browser
 *
 * Copyright 2019 Basis Technology Corp.
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
package org.sleuthkit.autopsy.geolocation;

import java.awt.event.MouseEvent;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.util.Collection;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import javax.swing.event.MouseInputListener;
import org.jxmapviewer.OSMTileFactoryInfo;
import org.jxmapviewer.input.CenterMapListener;
import org.jxmapviewer.input.PanMouseInputListener;
import org.jxmapviewer.input.ZoomMouseWheelListenerCursor;
import org.jxmapviewer.viewer.DefaultTileFactory;
import org.jxmapviewer.viewer.GeoPosition;
import org.jxmapviewer.viewer.TileFactoryInfo;
import org.jxmapviewer.viewer.Waypoint;
import org.jxmapviewer.viewer.WaypointPainter;

/**
 * Main panel with the JJXMapViewer object and its basic controls.
 */
final class MapPanel extends javax.swing.JPanel {
    
    private static final long serialVersionUID = 1L;

    private boolean zoomChanging;
    
    private KdTree<MapWaypoint> waypointTree;

    /**
     * Creates new form MapPanel
     */
    MapPanel() {
        zoomChanging = false;
         
        initComponents();
        initMap();
    }

    /**
     * Initialize the map.
     */
    private void initMap() {

        TileFactoryInfo info = new OSMTileFactoryInfo();
        DefaultTileFactory tileFactory = new DefaultTileFactory(info);
        mapViewer.setTileFactory(tileFactory);

        // Add Mouse interactions
        MouseInputListener mia = new PanMouseInputListener(mapViewer);
        mapViewer.addMouseListener(mia);
        mapViewer.addMouseMotionListener(mia);

        mapViewer.addMouseListener(new CenterMapListener(mapViewer));
        mapViewer.addMouseWheelListener(new ZoomMouseWheelListenerCursor(mapViewer));

        // Listen to the map for a change in zoom so that we can update the slider.
        mapViewer.addPropertyChangeListener("zoom", new PropertyChangeListener() {
            @Override
            public void propertyChange(PropertyChangeEvent evt) {
                zoomSlider.setValue(mapViewer.getZoom());
            }
        });

        zoomSlider.setMinimum(tileFactory.getInfo().getMinimumZoomLevel());
        zoomSlider.setMaximum(tileFactory.getInfo().getMaximumZoomLevel());

       
        setZoom(tileFactory.getInfo().getMaximumZoomLevel()- 1);
        mapViewer.setAddressLocation(new GeoPosition(0, 0));

        // Basic painters for the way points. 
        WaypointPainter<Waypoint> waypointPainter = new WaypointPainter<Waypoint>() {
            @Override
            public Set<Waypoint> getWaypoints() {
                Set<Waypoint> set = new HashSet<>();
                if(waypointTree != null) {
                    Iterator<MapWaypoint> iterator = waypointTree.iterator();
                    while(iterator.hasNext()) {
                        set.add(iterator.next());
                    }
                }
                return set;
            }
        };

        mapViewer.setOverlayPainter(waypointPainter);
    }
    
    void setWaypoints(List<MapWaypoint> waypoints) {
        waypointTree = new KdTree<>();
        
        for (MapWaypoint waypoint: waypoints) {
            waypointTree.add(waypoint);
        }
        
        mapViewer.repaint();
    }
    
    void setCenterLocation(Waypoint waypoint) {
        mapViewer.setCenterPosition(waypoint.getPosition());
    }

    /**
     * Set the current zoom level.
     * 
     * @param zoom 
     */
    void setZoom(int zoom) {
        zoomChanging = true;
        mapViewer.setZoom(zoom);
        zoomSlider.setValue((zoomSlider.getMaximum() + zoomSlider.getMinimum())- zoom);
        zoomChanging = false;
    }
    
    /**
     * Remove all of the way points from the map.
     */
    void clearWaypoints() {
        waypointTree = null;
    }
    
    private void showPopupMenu(MouseEvent evt) {
        if (waypointTree == null) {
            return;
        }
        
        // Convert the mouse click location to latitude & longitude
        GeoPosition geopos = mapViewer.getTileFactory().pixelToGeo(evt.getPoint(), mapViewer.getZoom());
        
        // Get the 5 nearest neightbors to the point
        Collection<MapWaypoint> waypoints = waypointTree.nearestNeighbourSearch(5, MapWaypoint.getDummyWaypoint(geopos));
        
        for(MapWaypoint waypoint: waypoints) {
            
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
        java.awt.GridBagConstraints gridBagConstraints;

        mapViewer = new org.jxmapviewer.JXMapViewer();
        zoomPanel = new javax.swing.JPanel();
        zoomSlider = new javax.swing.JSlider();

        setFocusable(false);
        setLayout(new java.awt.BorderLayout());

        mapViewer.addMouseListener(new java.awt.event.MouseAdapter() {
            public void mousePressed(java.awt.event.MouseEvent evt) {
                mapViewerMousePressed(evt);
            }
            public void mouseReleased(java.awt.event.MouseEvent evt) {
                mapViewerMouseReleased(evt);
            }
        });
        mapViewer.setLayout(new java.awt.GridBagLayout());

        zoomPanel.setFocusable(false);
        zoomPanel.setOpaque(false);
        zoomPanel.setRequestFocusEnabled(false);

        zoomSlider.setMaximum(15);
        zoomSlider.setMinimum(10);
        zoomSlider.setMinorTickSpacing(1);
        zoomSlider.setOrientation(javax.swing.JSlider.VERTICAL);
        zoomSlider.setPaintTicks(true);
        zoomSlider.setSnapToTicks(true);
        zoomSlider.setMinimumSize(new java.awt.Dimension(35, 100));
        zoomSlider.setOpaque(false);
        zoomSlider.setPreferredSize(new java.awt.Dimension(35, 190));
        zoomSlider.addChangeListener(new javax.swing.event.ChangeListener() {
            public void stateChanged(javax.swing.event.ChangeEvent evt) {
                zoomSliderStateChanged(evt);
            }
        });

        javax.swing.GroupLayout zoomPanelLayout = new javax.swing.GroupLayout(zoomPanel);
        zoomPanel.setLayout(zoomPanelLayout);
        zoomPanelLayout.setHorizontalGroup(
            zoomPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(zoomPanelLayout.createSequentialGroup()
                .addGap(0, 0, 0)
                .addComponent(zoomSlider, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addContainerGap(javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
        );
        zoomPanelLayout.setVerticalGroup(
            zoomPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, zoomPanelLayout.createSequentialGroup()
                .addContainerGap(javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                .addComponent(zoomSlider, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addGap(0, 0, 0))
        );

        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.anchor = java.awt.GridBagConstraints.SOUTHWEST;
        gridBagConstraints.weightx = 1.0;
        gridBagConstraints.weighty = 1.0;
        gridBagConstraints.insets = new java.awt.Insets(4, 4, 4, 4);
        mapViewer.add(zoomPanel, gridBagConstraints);

        add(mapViewer, java.awt.BorderLayout.CENTER);
    }// </editor-fold>//GEN-END:initComponents

    private void zoomSliderStateChanged(javax.swing.event.ChangeEvent evt) {//GEN-FIRST:event_zoomSliderStateChanged
        if (!zoomChanging) {
            setZoom(zoomSlider.getValue());
        }
    }//GEN-LAST:event_zoomSliderStateChanged

    private void mapViewerMousePressed(java.awt.event.MouseEvent evt) {//GEN-FIRST:event_mapViewerMousePressed
        if(evt.isPopupTrigger()) {
            showPopupMenu(evt);
        }
    }//GEN-LAST:event_mapViewerMousePressed

    private void mapViewerMouseReleased(java.awt.event.MouseEvent evt) {//GEN-FIRST:event_mapViewerMouseReleased
        if(evt.isPopupTrigger()) {
            showPopupMenu(evt);
        }
    }//GEN-LAST:event_mapViewerMouseReleased


    // Variables declaration - do not modify//GEN-BEGIN:variables
    private org.jxmapviewer.JXMapViewer mapViewer;
    private javax.swing.JPanel zoomPanel;
    private javax.swing.JSlider zoomSlider;
    // End of variables declaration//GEN-END:variables
}
