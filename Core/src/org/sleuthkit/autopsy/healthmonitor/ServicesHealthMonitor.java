/*
 * Autopsy Forensic Browser
 *
 * Copyright 2018 Basis Technology Corp.
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
package org.sleuthkit.autopsy.healthmonitor;

import com.google.common.util.concurrent.ThreadFactoryBuilder;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Map;
import java.util.HashMap;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.logging.Level;
import org.apache.commons.dbcp2.BasicDataSource;
import org.sleuthkit.autopsy.coordinationservice.CoordinationService;
import org.sleuthkit.autopsy.core.UserPreferences;
import org.sleuthkit.autopsy.core.UserPreferencesException;
import org.sleuthkit.autopsy.coreutils.Logger;
import org.sleuthkit.autopsy.coreutils.ModuleSettings;
import org.sleuthkit.datamodel.CaseDbConnectionInfo;
import org.sleuthkit.datamodel.CaseDbSchemaVersionNumber;

/**
 * Class for recording data on the health of the system.
 * 
 * For timing data:
 * Modules will call getTimingMetric() before the code to be timed to get a TimingMetric object
 * Modules will call submitTimingMetric() with the obtained TimingMetric object to log it
 */
public class ServicesHealthMonitor {
    
    private final static Logger logger = Logger.getLogger(ServicesHealthMonitor.class.getName());
    private final static String DATABASE_NAME = "ServicesHealthMonitor";
    private final static String MODULE_NAME = "ServicesHealthMonitor";
    private final static String IS_ENABLED_KEY = "is_enabled";
    private final static long DATABASE_WRITE_INTERVAL = 1; // Minutes
    public static final CaseDbSchemaVersionNumber CURRENT_DB_SCHEMA_VERSION
            = new CaseDbSchemaVersionNumber(1, 0);
    
    private static final AtomicBoolean isEnabled = new AtomicBoolean(false);
    private static ServicesHealthMonitor instance;
    
    private ScheduledThreadPoolExecutor periodicTasksExecutor;
    private final Map<String, TimingInfo> timingInfoMap;
    private static final int CONN_POOL_SIZE = 10;
    private BasicDataSource connectionPool = null;
    
    private ServicesHealthMonitor() throws HealthMonitorException {
        
        // Create the map to collect timing metrics. The map will exist regardless
        // of whether the monitor is enabled.
        timingInfoMap = new HashMap<>();
        
        // Read from module settings to determine if the module is enabled
        if (ModuleSettings.settingExists(MODULE_NAME, IS_ENABLED_KEY)) {
            if(ModuleSettings.getConfigSetting(MODULE_NAME, IS_ENABLED_KEY).equals("true")){
                isEnabled.set(true);
                activateMonitor();
                return;
            }
        }
        isEnabled.set(false);
    }
    
    /**
     * Get the instance of the ServicesHealthMonitor
     * @return the instance
     * @throws HealthMonitorException 
     */
    synchronized static ServicesHealthMonitor getInstance() throws HealthMonitorException {
        if (instance == null) {
            instance = new ServicesHealthMonitor();
        }
        return instance;
    }
    
    /**
     * Activate the health monitor.
     * Creates/initialized the database (if needed), clears any existing metrics 
     * out of the maps, and sets up the timer for writing to the database.
     * @throws HealthMonitorException 
     */
    private synchronized void activateMonitor() throws HealthMonitorException {
        
        logger.log(Level.INFO, "Activating Servies Health Monitor");
        
        if (!UserPreferences.getIsMultiUserModeEnabled()) {
            throw new HealthMonitorException("Multi user mode is not enabled - can not activate services health monitor");
        }
        // Set up database (if needed)
        CoordinationService.Lock lock = getExclusiveDbLock();
        if(lock == null) {
            throw new HealthMonitorException("Error getting database lock");
        }
        
        try {
            // Check if the database exists
            if (! databaseExists()) {
                
                // If not, create a new one
                System.out.println("  No database exists - setting up new one");
                createDatabase();
                initializeDatabaseSchema();
            }
            
            // Any database upgrades would happen here
            
        } finally {
            try {
                lock.release();
            } catch (CoordinationService.CoordinationServiceException ex) {
                throw new HealthMonitorException("Error releasing database lock", ex);
            }
        }
        
        // Clear out any old data
        timingInfoMap.clear();
        
        // Start the timer for database writes
        startTimer();
    }
    
    /**
     * Deactivate the health monitor.
     * This should only be used when disabling the monitor, not when Autopsy is closing.
     * Clears out any metrics that haven't been written, stops the database write timer,
     * and shuts down the connection pool.
     * @throws HealthMonitorException 
     */
    private synchronized void deactivateMonitor() throws HealthMonitorException {
        
        logger.log(Level.INFO, "Deactivating Servies Health Monitor");
        
        // Clear out the collected data
        timingInfoMap.clear();
        
        // Stop the timer
        stopTimer();
        
        // Shut down the connection pool
        shutdownConnections();
    }
    
    /**
     * Start the ScheduledThreadPoolExecutor that will handle the database writes.
     */
    private synchronized void startTimer() {
        if(periodicTasksExecutor != null) {
            // Make sure the previous executor (if it exists) has been stopped
            periodicTasksExecutor.shutdown();
        }
        periodicTasksExecutor = new ScheduledThreadPoolExecutor(1, new ThreadFactoryBuilder().setNameFormat("health_monitor_timer").build());
        periodicTasksExecutor.scheduleWithFixedDelay(new DatabaseWriteTask(), DATABASE_WRITE_INTERVAL, DATABASE_WRITE_INTERVAL, TimeUnit.MINUTES);
    }
    
    /**
     * Stop the ScheduledThreadPoolExecutor to prevent further database writes.
     */
    private synchronized void stopTimer() {
        if(periodicTasksExecutor != null) {
            periodicTasksExecutor.shutdown();
        }
    }

    /**
     * Called from the installer to set up the Health Monitor instance at startup.
     * @throws HealthMonitorException 
     */
    static synchronized void startUp() throws HealthMonitorException {
        getInstance();
    }
    
    /**
     * Enabled/disable the health monitor.
     * @param enabled true to enable the monitor, false to disable it
     * @throws HealthMonitorException 
     */
    static synchronized void setEnabled(boolean enabled) throws HealthMonitorException {
        if(enabled == isEnabled.get()) {
            // The setting has not changed, so do nothing
            return;
        }
        
        if(enabled) {
            ModuleSettings.setConfigSetting(MODULE_NAME, IS_ENABLED_KEY, "true");
            isEnabled.set(true);
            getInstance().activateMonitor();
        } else {
            ModuleSettings.setConfigSetting(MODULE_NAME, IS_ENABLED_KEY, "false");
            isEnabled.set(false);
            getInstance().deactivateMonitor();
        }
    }
    
    /**
     * Get a metric that will measure the time to execute a section of code.
     * Call this before the section of code to be timed and then
     * submit it afterward using submitTimingMetric().
     * This method is safe to call regardless of whether the Services Health
     * Monitor is enabled.
     * @param name A short but descriptive name describing the code being timed.
     *             This name will appear in the UI.
     * @return The TimingMetric object
     */
    public static TimingMetric getTimingMetric(String name) {
        if(isEnabled.get()) {
            return new TimingMetric(name);
        }
        return null;
    }
    
    /**
     * Submit the metric that was previously obtained through getTimingMetric().
     * Call this immediately after the section of code being timed.
     * This method is safe to call regardless of whether the Services Health
     * Monitor is enabled.
     * @param metric The TimingMetric object obtained from getTimingMetric()
     */
    public static void submitTimingMetric(TimingMetric metric) {
        if(isEnabled.get() && (metric != null)) {
            metric.stopTiming();
            try {
                getInstance().addTimingMetric(metric);
            } catch (HealthMonitorException ex) {
                // We don't want calling methods to have to check for exceptions, so just log it
                logger.log(Level.SEVERE, "Error adding timing metric", ex);
            }
        }
    }
    
    /**
     * Add the timing metric data to the map.
     * @param metric The metric to add. stopTiming() should already have been called.
     */
    private void addTimingMetric(TimingMetric metric) throws HealthMonitorException {

        // Do as little as possible within the synchronized block to minimize
        // blocking with multiple threads.
        synchronized(this) {
            // There's a small check-then-act situation here where isEnabled
            // may have changed before reaching this code. This is fine - 
            // the map still exists and any extra data added after the monitor
            // is disabled will be deleted if the monitor is re-enabled. This
            // seems preferable to doing another check on isEnabled within
            // the synchronized block.
            if(timingInfoMap.containsKey(metric.getName())) {
                timingInfoMap.get(metric.getName()).addMetric(metric);
            } else {
                timingInfoMap.put(metric.getName(), new TimingInfo(metric));
            }
        }
    }
    
    // TODO: Make private once testing is done
    /**
     * Write the collected metrics to the database.
     * @throws HealthMonitorException 
     */
    void writeCurrentStateToDatabase() throws HealthMonitorException {
        logger.log(Level.INFO, "Writing health monitor metrics to database");
        
        Map<String, TimingInfo> timingMapCopy;
        
        // Do as little as possible within the synchronized block since it will
        // block threads attempting to record metrics.
        synchronized(this) {
            if(! isEnabled.get()) {
                return;
            }
            
            // Make a shallow copy of the timing map. The map should be small - one entry
            // per metric name.
            timingMapCopy = new HashMap<>(timingInfoMap);
            timingInfoMap.clear();
        }
        
        // Check if there's anything to report (right now we only have the timing map)
        if(timingMapCopy.keySet().isEmpty()) {
            return;
        }
        
        // TODO: Debug
        for(String name:timingMapCopy.keySet()){
            TimingInfo info = timingMapCopy.get(name);
            long timestamp = System.currentTimeMillis();
            System.out.println("  Name: " + name + "\tTimestamp: " + timestamp + "\tAverage: " + info.getAverage() +
                    "\tMax: " + info.getMax() + "\tMin: " + info.getMin());
        }
        
        // Write to the database
        CoordinationService.Lock lock = getSharedDbLock();
        if(lock == null) {
            throw new HealthMonitorException("Error getting database lock");
        }
        
        try {
            Connection conn = connect();
            if(conn == null) {
                throw new HealthMonitorException("Error getting database connection");
            }

            // Add timing metrics to the database
            String addTimingInfoSql = "INSERT INTO timing_data (name, timestamp, count, average, max, min) VALUES (?, ?, ?, ?, ?, ?)";
            try (PreparedStatement statement = conn.prepareStatement(addTimingInfoSql)) {

                for(String name:timingMapCopy.keySet()) {
                    TimingInfo info = timingMapCopy.get(name);

                    statement.setString(1, name);
                    statement.setLong(2, System.currentTimeMillis());
                    statement.setLong(3, info.getCount());
                    statement.setLong(4, info.getAverage());
                    statement.setLong(5, info.getMax());
                    statement.setLong(6, info.getMin());

                    statement.execute();
                }

            } catch (SQLException ex) {
                throw new HealthMonitorException("Error saving metric data to database", ex);
            } finally {
                try {
                    conn.close();
                } catch (SQLException ex) {
                    logger.log(Level.SEVERE, "Error closing Connection.", ex);
                }
            }
        } finally {
            try {
                lock.release();
            } catch (CoordinationService.CoordinationServiceException ex) {
                throw new HealthMonitorException("Error releasing database lock", ex);
            }
        }
    }
    
    // TODO: debug
    synchronized void clearCurrentState() {
        timingInfoMap.clear();
    }
    
    /**
     * Call during application closing - attempts to log any remaining entries.
     */
    static synchronized void close() {
        if(isEnabled.get()) {
            
            // Stop the timer
            try {
                getInstance().stopTimer();
            } catch (HealthMonitorException ex) {
                logger.log(Level.SEVERE, "Error shutting down timer", ex);
            }
            
            // Write current data
            try {
                getInstance().writeCurrentStateToDatabase();
            } catch (HealthMonitorException ex) {
                logger.log(Level.SEVERE, "Error writing final metric data to database", ex);
            }
            
            // Shutdown connection pool
            try {
                getInstance().shutdownConnections();
            } catch (HealthMonitorException ex) {
                logger.log(Level.SEVERE, "Error shutting down connection pool", ex);
            }
        }
    }
    
    // TODO: debug
    synchronized void printCurrentState() {
        System.out.println("\nTiming Info Map:");
        for(String name:timingInfoMap.keySet()) {
            System.out.print(name + "\t");
            timingInfoMap.get(name).print();
        }
    }
    
    // TODO: Change to private after testing
    /**
     * Check whether the health monitor database exists.
     * Does not check the schema.
     * @return true if the database exists, false otherwise
     * @throws HealthMonitorException 
     */
    boolean databaseExists() throws HealthMonitorException {       
        try {
            // Use the same database settings as the case
            CaseDbConnectionInfo db = UserPreferences.getDatabaseConnectionInfo();
            Class.forName("org.postgresql.Driver"); //NON-NLS
            ResultSet rs = null;
            try (Connection connection = DriverManager.getConnection("jdbc:postgresql://" + db.getHost() + ":" + db.getPort() + "/postgres", db.getUserName(), db.getPassword()); //NON-NLS
                    Statement statement = connection.createStatement();) {
                String createCommand = "SELECT 1 AS result FROM pg_database WHERE datname='" + DATABASE_NAME + "'"; 
                System.out.println("  query: " + createCommand);
                rs = statement.executeQuery(createCommand);
                if(rs.next()) {
                    logger.log(Level.INFO, "Existing Services Health Monitor database found");
                    return true;
                }
            } finally {
                if(rs != null) {
                    rs.close();
                }
            }
        } catch (UserPreferencesException | ClassNotFoundException | SQLException ex) {
            throw new HealthMonitorException("Failed check for health monitor database", ex);
        }
        return false;
    }
    
    /**
     * Create a new health monitor database.
     * @throws HealthMonitorException 
     */
    private void createDatabase() throws HealthMonitorException {
        try {
            // Use the same database settings as the case
            CaseDbConnectionInfo db = UserPreferences.getDatabaseConnectionInfo();
            Class.forName("org.postgresql.Driver"); //NON-NLS
            try (Connection connection = DriverManager.getConnection("jdbc:postgresql://" + db.getHost() + ":" + db.getPort() + "/postgres", db.getUserName(), db.getPassword()); //NON-NLS
                    Statement statement = connection.createStatement();) {
                String createCommand = "CREATE DATABASE \"" + DATABASE_NAME + "\" OWNER \"" + db.getUserName() + "\""; //NON-NLS
                statement.execute(createCommand);
            }
            logger.log(Level.INFO, "Created new health monitor database " + DATABASE_NAME);
        } catch (UserPreferencesException | ClassNotFoundException | SQLException ex) {
            throw new HealthMonitorException("Failed to delete health monitor database", ex);
        }
    }
    
    // TODO: At least make private
    /**
     * Delete the current health monitor database (for testing only)
     * Make private after test
     */
    void deleteDatabase() {
        try {
            // Use the same database settings as the case
            CaseDbConnectionInfo db = UserPreferences.getDatabaseConnectionInfo();
            Class.forName("org.postgresql.Driver"); //NON-NLS
            try (Connection connection = DriverManager.getConnection("jdbc:postgresql://" + db.getHost() + ":" + db.getPort() + "/postgres", db.getUserName(), db.getPassword()); //NON-NLS
                    Statement statement = connection.createStatement();) {
                String deleteCommand = "DROP DATABASE \"" + DATABASE_NAME + "\""; //NON-NLS
                statement.execute(deleteCommand);
            }
        } catch (UserPreferencesException | ClassNotFoundException | SQLException ex) {
            logger.log(Level.SEVERE, "Failed to delete health monitor database", ex);
        }
    }

    /**
     * Setup a connection pool for db connections.
     * @throws HealthMonitorException 
     */
    private void setupConnectionPool() throws HealthMonitorException {
        try {
            CaseDbConnectionInfo db = UserPreferences.getDatabaseConnectionInfo();
        
            connectionPool = new BasicDataSource();
            connectionPool.setDriverClassName("org.postgresql.Driver");

            StringBuilder connectionURL = new StringBuilder();
            connectionURL.append("jdbc:postgresql://");
            connectionURL.append(db.getHost());
            connectionURL.append(":");
            connectionURL.append(db.getPort());
            connectionURL.append("/");
            connectionURL.append(DATABASE_NAME);

            connectionPool.setUrl(connectionURL.toString());
            connectionPool.setUsername(db.getUserName());
            connectionPool.setPassword(db.getPassword());

            // tweak pool configuration
            connectionPool.setInitialSize(2); // start with 2 connections
            connectionPool.setMaxIdle(CONN_POOL_SIZE); // max of 10 idle connections
            connectionPool.setValidationQuery("SELECT version()");
        } catch (UserPreferencesException ex) {
            throw new HealthMonitorException("Error loading database configuration", ex);
        }
    }
    
    /**
     * Shut down the connection pool
     * @throws HealthMonitorException 
     */
    private void shutdownConnections() throws HealthMonitorException {
        try {
            synchronized(this) {
                if(connectionPool != null){
                    connectionPool.close();
                    connectionPool = null; // force it to be re-created on next connect()
                }
            }
        } catch (SQLException ex) {
            throw new HealthMonitorException("Failed to close existing database connections.", ex); // NON-NLS
        }
    }
    
    /**
     * Get a database connection.
     * Sets up the connection pool if needed.
     * @return The Connection object
     * @throws HealthMonitorException 
     */
    private Connection connect() throws HealthMonitorException {
        synchronized (this) {
            if (connectionPool == null) {
                setupConnectionPool();
            }
        }

        try {
            return connectionPool.getConnection();
        } catch (SQLException ex) {
            throw new HealthMonitorException("Error getting connection from connection pool.", ex); // NON-NLS
        }
    }
    
    /**
     * Initialize the database.
     * @throws HealthMonitorException 
     */
    private void initializeDatabaseSchema() throws HealthMonitorException {
        Connection conn = connect();
        if(conn == null) {
            throw new HealthMonitorException("Error getting database connection");
        }

        // TODO: transaction
        try (Statement statement = conn.createStatement()) {
            StringBuilder createTimingTable = new StringBuilder();
            createTimingTable.append("CREATE TABLE IF NOT EXISTS timing_data (");
            createTimingTable.append("id SERIAL PRIMARY KEY,");
            createTimingTable.append("name text NOT NULL,");
            createTimingTable.append("timestamp bigint NOT NULL,");
            createTimingTable.append("count bigint NOT NULL,");
            createTimingTable.append("average bigint NOT NULL,");
            createTimingTable.append("max bigint NOT NULL,");
            createTimingTable.append("min bigint NOT NULL");
            createTimingTable.append(")");
            statement.execute(createTimingTable.toString());
            
            StringBuilder createDbInfoTable = new StringBuilder();
            createDbInfoTable.append("CREATE TABLE IF NOT EXISTS db_info (");
            createDbInfoTable.append("id SERIAL PRIMARY KEY NOT NULL,");
            createDbInfoTable.append("name text NOT NULL,");
            createDbInfoTable.append("value text NOT NULL");
            createDbInfoTable.append(")");
            statement.execute(createDbInfoTable.toString());
            
            statement.execute("INSERT INTO db_info (name, value) VALUES ('SCHEMA_VERSION', '" + CURRENT_DB_SCHEMA_VERSION.getMajor() + "')");
            statement.execute("INSERT INTO db_info (name, value) VALUES ('SCHEMA_MINOR_VERSION', '" + CURRENT_DB_SCHEMA_VERSION.getMinor() + "')");
            
        } catch (SQLException ex) {
            throw new HealthMonitorException("Error initializing database", ex);
        } finally {
            try {
                conn.close();
            } catch (SQLException ex) {
                logger.log(Level.SEVERE, "Error closing Connection.", ex);
            }
        }
    }
    
    /**
     * The task called by the ScheduledThreadPoolExecutor to handle
     * the database writes.
     */
    private final class DatabaseWriteTask implements Runnable {

        /**
         * Write current metric data to the database
         */
        @Override
        public void run() {
            try {
                getInstance().writeCurrentStateToDatabase();
            } catch (HealthMonitorException ex) {
                logger.log(Level.SEVERE, "Error writing current metrics to database", ex); //NON-NLS
            }
        }
    }
    
    /**
     * Get an exclusive lock for the health monitor database.
     * Acquire this before creating, initializing, or updating the database schema.
     * @return The lock
     * @throws HealthMonitorException 
     */
    private CoordinationService.Lock getExclusiveDbLock() throws HealthMonitorException{
        try {
            String databaseNodeName = DATABASE_NAME;
            CoordinationService.Lock lock = CoordinationService.getInstance().tryGetExclusiveLock(CoordinationService.CategoryNode.HEALTH_MONITOR, databaseNodeName, 5, TimeUnit.MINUTES);

            if(lock != null){
                return lock;
            }
            throw new HealthMonitorException("Error acquiring database lock");
        } catch (InterruptedException | CoordinationService.CoordinationServiceException ex){
            throw new HealthMonitorException("Error acquiring database lock");
        }
    } 
    
    /**
     * Get an shared lock for the health monitor database.
     * Acquire this before database reads or writes.
     * @return The lock
     * @throws HealthMonitorException 
     */
    private CoordinationService.Lock getSharedDbLock() throws HealthMonitorException{
        try {
            String databaseNodeName = DATABASE_NAME;
            CoordinationService.Lock lock = CoordinationService.getInstance().tryGetSharedLock(CoordinationService.CategoryNode.HEALTH_MONITOR, databaseNodeName, 5, TimeUnit.MINUTES);

            if(lock != null){
                return lock;
            }
            throw new HealthMonitorException("Error acquiring database lock");
        } catch (InterruptedException | CoordinationService.CoordinationServiceException ex){
            throw new HealthMonitorException("Error acquiring database lock");
        }
    } 
    
    /**
     * Internal class for collecting timing metrics.
     * Instead of storing each TimingMetric, we only store the min and max
     * seen and the number of metrics and total duration to compute the average
     * later.
     * One TimingInfo instance should be created per metric name, and
     * additional timing metrics will be added to it. 
     */
    private class TimingInfo {
        private long count; // Number of metrics collected
        private long sum;   // Sum of the durations collected (nanoseconds)
        private long max;   // Maximum value found (nanoseconds)
        private long min;   // Minimum value found (nanoseconds)
        
        TimingInfo(TimingMetric metric) throws HealthMonitorException {
            count = 1;
            sum = metric.getDuration();
            max = metric.getDuration();
            min = metric.getDuration();
        }
        
        /**
         * Add a new TimingMetric to an existing TimingInfo object.
         * This is called in a synchronized block for almost all new 
         * TimingMetric objects, so do as little processing here as possible.
         * @param metric The new metric
         * @throws HealthMonitorException Will be thrown if the metric hasn't been stopped
         */
        void addMetric(TimingMetric metric) throws HealthMonitorException {
            
            // Keep track of needed info to calculate the average
            count++;
            sum += metric.getDuration();
            
            // Check if this is the longest duration seen
            if(max < metric.getDuration()) {
                max = metric.getDuration();
            }
            
            // Check if this is the lowest duration seen
            if(min > metric.getDuration()) {
                min = metric.getDuration();
            }
        }
        
        /**
         * Get the average duration
         * @return average duration (nanoseconds)
         */
        long getAverage() {
            return sum / count;
        }
        
        /**
         * Get the maximum duration
         * @return maximum duration (nanoseconds)
         */
        long getMax() {
            return max;
        }
        
        /**
         * Get the minimum duration
         * @return minimum duration (nanoseconds)
         */
        long getMin() {
            return min;
        }
        
        /**
         * Get the total number of metrics collected
         * @return number of metrics collected
         */
        long getCount() {
            return count;
        }
        
        // TODO: debug
        void print() {
            System.out.println("count: " + count + "\tsum: " + sum + "\tmax: " + max + "\tmin: " + min);
        }
    }
}
