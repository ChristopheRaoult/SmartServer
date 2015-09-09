package com.spacecode.smartserver.helper;

import com.spacecode.sdk.device.Device;
import com.spacecode.sdk.device.DeviceCreationException;
import com.spacecode.sdk.device.data.DeviceStatus;
import com.spacecode.sdk.device.data.Inventory;
import com.spacecode.sdk.device.data.PluggedDevice;
import com.spacecode.sdk.device.event.*;
import com.spacecode.sdk.device.module.AuthenticationModule;
import com.spacecode.sdk.device.module.FingerprintReaderException;
import com.spacecode.sdk.network.communication.EventCode;
import com.spacecode.sdk.user.User;
import com.spacecode.sdk.user.data.AccessType;
import com.spacecode.smartserver.SmartServer;
import com.spacecode.smartserver.database.DbManager;
import com.spacecode.smartserver.database.dao.DaoAuthentication;
import com.spacecode.smartserver.database.dao.DaoInventory;
import com.spacecode.smartserver.database.dao.DaoUser;
import com.spacecode.smartserver.database.entity.AuthenticationEntity;
import com.spacecode.smartserver.database.entity.InventoryEntity;
import com.spacecode.smartserver.database.entity.UserEntity;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;

/**
 * Handle RfidDevice connection, instantiation, disconnection, and events.
 */
public final class DeviceHandler
{
    // 
    private volatile static Device DEVICE;
    
    // if true, the new inventories will be saved in the database (if false, they won't be)
    private volatile static boolean RECORD_INVENTORY = true;

    // allows the CmdSerialBridge to set the current state of device (usb / ethernet)
    private static boolean SERIAL_PORT_FORWARDING = false;

    /** Must not be instantiated. */
    private DeviceHandler()
    {
    }

    /**
     * Looks for available SpaceCode devices. Only 1 can be used and only 1 must be present.
     * Instantiates RfidDevice.
     *
     * @return True if instantiation succeeded, False if it failed (or if number of devices != 1).
     */
    public synchronized static boolean connectDevice()
    {
        if(DEVICE != null)
        {
            return true;
        }

        Map<String, PluggedDevice> pluggedDevices = Device.getPluggedDevices();

        if(pluggedDevices.isEmpty() || pluggedDevices.size() > 1)
        {
            SmartLogger.getLogger().warning("0 or more than 1 device detected.");
            return false;
        }

        PluggedDevice deviceInfo = pluggedDevices.entrySet().iterator().next().getValue();

        try
        {
            DEVICE = new Device(null, deviceInfo.getSerialPort());
            DEVICE.addListener(new SmartEventHandler());
        } catch (DeviceCreationException dce)
        {
            SmartLogger.getLogger().log(Level.INFO, "Unable to instantiate a device.", dce);
            return false;
        }

        return true;
    }

    /**
     * Release current device (if it has been initialized).
     */
    public synchronized static void disconnectDevice()
    {
        if(DEVICE != null)
        {
            DEVICE.release();
            DEVICE = null;
        }
    }

    /**
     * Try to reconnect device during one hour (waiting five seconds after each try).
     *
     * @return True if reconnection succeeded, false otherwise.
     */
    public static boolean reconnectDevice()
    {
        boolean deviceConnected = false;
        long initialTimestamp = System.currentTimeMillis();

        while(!SERIAL_PORT_FORWARDING && (System.currentTimeMillis() - initialTimestamp < 3600000))
        {
            SmartLogger.getLogger().info("Reconnecting Device...");

            deviceConnected = connectDevice();

            if(deviceConnected)
            {
                // let all the clients know that the device is "Ready" again
                SmartServer.sendAllClients(EventCode.STATUS_CHANGED, DeviceStatus.READY.name());

                onConnected();
                break;
            }

            try
            {
                Thread.sleep(5000);
            } catch (InterruptedException ie)
            {
                SmartLogger.getLogger().log(Level.WARNING, "Interrupted while trying to reconnect Device.", ie);
                break;
            }
        }

        return deviceConnected;
    }

    /**
     * @return Currently used Device instance (null if not initialized).
     */
    public static Device getDevice()
    {
        return DEVICE;
    }

    /**
     * Connect the modules (fingerprint / badge readers, temperature probe).
     */
    public static void connectModules()
    {
        if(DEVICE == null)
        {
            SmartLogger.getLogger().warning("Unable to connect modules, the device is not initialized.");
            return;
        }

        String fprMaster = ConfManager.getDevFprMaster();
        String fprSlave = ConfManager.getDevFprSlave();

        try
        {
            if(fprMaster != null && !fprMaster.trim().isEmpty())
            {
                // 2 readers
                if(fprSlave != null && !fprSlave.trim().isEmpty())
                {
                    if(AuthenticationModule.connectFingerprintReaders(2) != 2)
                    {
                        SmartLogger.getLogger().warning("Couldn't initialize the two fingerprint readers.");
                    }

                    else if(!
                            (DEVICE.addFingerprintReader(fprMaster, true)
                            && DEVICE.addFingerprintReader(fprSlave, false))
                            )
                    {
                        SmartLogger.getLogger().warning("Couldn't connect the two fingerprint readers.");
                    }
                }

                // 1 reader
                else
                {
                    if(AuthenticationModule.connectFingerprintReaders(1) != 1)
                    {
                        SmartLogger.getLogger().warning("Couldn't initialize the fingerprint reader.");
                    }

                    else if(!DEVICE.addFingerprintReader(fprMaster, true))
                    {
                        SmartLogger.getLogger().warning("Couldn't connect the fingerprint reader.");
                    }
                }
            }
        } catch (FingerprintReaderException fre)
        {
            SmartLogger.getLogger().log(Level.INFO,
                    "An unexpected error occurred during fingerprint readers initialization.", fre);
        }

        String brMaster = ConfManager.getDevBrMaster();
        String brSlave = ConfManager.getDevBrSlave();

        if(brMaster != null && !brMaster.trim().isEmpty())
        {
            if(!DEVICE.addBadgeReader("BR1", brMaster, true))
            {
                SmartLogger.getLogger().warning("Unable to add Master Badge Reader on "+brMaster);
            }

            if(brSlave != null && !brSlave.trim().isEmpty() && !DEVICE.addBadgeReader("BR2", brSlave, false))
            {
                SmartLogger.getLogger().warning("Unable to add Slave Badge Reader on "+brSlave);
            }
        }

        connectProbeIfEnabled();
    }

    /** If a temperature probe is enabled (see ConfManager), try to add a module TemperatureProbe to the current Device.
     */
    private static void connectProbeIfEnabled()
    {
        if(ConfManager.isDevTemperature())
        {
            int measurementDelay = ConfManager.getDevTemperatureDelay();
            double measurementDelta = ConfManager.getDevTemperatureDelta();

            measurementDelay = measurementDelay == -1 ? 60  : measurementDelay;
            measurementDelta = measurementDelta == -1 ? 0.3 : measurementDelta;

            // TODO: Don't get Stuck at this point if VirtualHub cannot be contacted or the probe is unavailable
            if(!DEVICE.addTemperatureProbe("tempProbe1", measurementDelay, measurementDelta))
            {
                SmartLogger.getLogger().warning("Unable to add the Temperature probe.");
            }
        }
    }

    /** Disconnect the temperature probe, if any, and reconnect it using last parameters (see ConfManager).
     */
    public static void reloadTemperatureProbe()
    {
        DEVICE.disconnectTemperatureProbe();
        connectProbeIfEnabled();
    }

    /**
     * WARNING: Should only be called by CommandSerialBridge when Serial Bridge is enabled/disabled.
     *
     * @param state True if serial port is being forwarded through host USB-OTG. False otherwise.
     */
    public static void setForwardingSerialPort(boolean state)
    {
        SERIAL_PORT_FORWARDING = state;
    }

    /**
     * @return True if the "serial bridge" is not active and the device is initialized. False otherwise.
     */
    public static boolean isAvailable()
    {
        return !SERIAL_PORT_FORWARDING && DEVICE != null;
    }

    /**
     * Get the list of Authorized Users (from UserRepository) and load them in the UsersService.
     *
     * @return True if the operation succeeded, false otherwise.
     */
    public static boolean loadUsers()
    {
        if(!isAvailable())
        {
            return false;
        }
        
        DaoUser userRepo = (DaoUser) DbManager.getDao(UserEntity.class);

        List<User> authorizedUsers = new ArrayList<>();
        List<User> unregisteredUsers = new ArrayList<>();
        
        if(!userRepo.sortUsersFromDb(authorizedUsers, unregisteredUsers))
        {
            SmartLogger.getLogger().severe("An error occurred when getting Authorized/Unregistered users from DB.");
            return false;
        }
        
        List<User> notAddedUsers = DEVICE.getUsersService().addUsers(authorizedUsers);

        // if an authorized user could not be added...
        if(!notAddedUsers.isEmpty())
        {
            SmartLogger.getLogger().warning(notAddedUsers.size() + " Authorized users could not be added.");
            notAddedUsers.clear();
        }

        // Add all "unregistered" users and then remove them (to put them in the "unregistered" list...)
        notAddedUsers = DEVICE.getUsersService().addUsers(unregisteredUsers);

        // if an unregistered user could not be added...
        if(!notAddedUsers.isEmpty())
        {
            SmartLogger.getLogger().warning(notAddedUsers.size() + " Unregistered users could not be added.");
            notAddedUsers.clear();
        }
        
        for(User unregUser : unregisteredUsers)
        {
            DEVICE.getUsersService().removeUser(unregUser.getUsername());    
        }        

        return true;
    }

    /**
     * Get the last inventory (if any) from Db and set it as "Last Inventory" of the current device.
     *
     * @return True if an inventory has been found, false otherwise.
     */
    public static boolean loadLastInventory()
    {
        DaoInventory daoInventory = (DaoInventory) DbManager.getDao(InventoryEntity.class);
        Inventory lastInventoryRecorded = daoInventory.getLastInventory();

        if(lastInventoryRecorded == null)
        {
            return false;
        }

        DEVICE.setLastInventory(lastInventoryRecorded);
        return true;
    }

    /**
     * Called once the device is (re)connected.
     * 
     * <ul>
     *  <li>Connect the modules (temperature probe, fingerprint readers, badge readers).</li>
     *  <li>Load the users from DB.</li>
     *  <li>Load the last Inventory (if any).</li>
     *  <li>Initialize AlertCenter and TemperatureCenter.</li>
     * </ul>
     * 
     * @return False if Loading Users failed. True otherwise.
     */
    public static boolean onConnected()
    {
        boolean result = true;
        
        // Use the configuration to connect/load modules.
        // TODO: do something if any failure (try to reconnect each module which fails to connect, or any other way)
        connectModules();

        // Load users from DB into Device's UsersService.
        if(!loadUsers())
        {
            SmartLogger.getLogger().severe("FATAL ERROR: Users could not be loaded from Database.");
            result = false;
        }

        // Load last inventory from DB and load it into device.
        if(!loadLastInventory())
        {
            SmartLogger.getLogger().info("No \"last\" Inventory loaded: none found.");
        }

        AlertCenter.initialize();
        TemperatureCenter.initialize();

        return result;
    }

    /**
     * Enable or disable the recording (in the database) of inventories.
     * 
     * @param state If true, the inventories will be recorded in the database. Otherwise, they won't.
     */
    public static void setRecordInventory(boolean state)
    {
        RECORD_INVENTORY = state;
    }

    /**
     * Allow other classes to know if the DeviceHandler is currently set to record the new inventories in the database.
     * 
     * @return True if the inventories are recorded, false otherwise.
     */
    public static boolean getRecordInventory()
    {
        return RECORD_INVENTORY;
    }

    /**
     * Handle Device events and proceed according to expected SmartServer behavior.
     */
    static class SmartEventHandler implements BasicEventHandler, ScanEventHandler, DoorEventHandler,
            AccessControlEventHandler, AccessModuleEventHandler, TemperatureEventHandler, LedEventHandler,
            MaintenanceEventHandler
    {
        @Override
        public void deviceDisconnected()
        {
            SmartLogger.getLogger().info("Device Disconnected...");

            SmartServer.sendAllClients(EventCode.DEVICE_DISCONNECTED);
            DEVICE = null;

            reconnectDevice();
        }

        @Override
        public void doorOpened()
        {
            SmartServer.sendAllClients(EventCode.DOOR_OPENED);
        }

        @Override
        public void doorClosed()
        {
            SmartServer.sendAllClients(EventCode.DOOR_CLOSED);
        }

        @Override
        public void doorOpenDelay()
        {
            SmartServer.sendAllClients(EventCode.DOOR_OPEN_DELAY);
        }

        @Override
        public void scanStarted()
        {
            SmartServer.sendAllClients(EventCode.SCAN_STARTED);
        }

        @Override
        public void scanCancelledByHost()
        {
            SmartServer.sendAllClients(EventCode.SCAN_CANCELLED_BY_HOST);
        }

        @Override 
        
        public void scanCompleted()
        {
            Inventory newInventory = DEVICE.getLastInventory();
            
            // insert the new inventory in the DB only if the user wants to
            if(RECORD_INVENTORY)
            {
                // insert it only if it has some relevant information (moves, authentication)
                if(     newInventory.getNumberAdded() != 0 || 
                        newInventory.getNumberRemoved() != 0 || 
                        newInventory.getNumberPresent() != 0 ||
                        newInventory.getAccessType() != AccessType.UNDEFINED)
                {
                    DaoInventory daoInventory = (DaoInventory) DbManager.getDao(InventoryEntity.class);
                    // todo: thread this? The point is about "getLastInventory" command, which MUST return the VERY last
                    daoInventory.persist(newInventory);
                }
            }     

            SmartServer.sendAllClients(EventCode.SCAN_COMPLETED);
        }

        @Override
        public void scanFailed()
        {
            SmartServer.sendAllClients(EventCode.SCAN_FAILED);
        }

        @Override
        public void tagAdded(String tagUID)
        {
            SmartServer.sendAllClients(EventCode.TAG_ADDED, tagUID);
        }

        @Override
        public void authenticationSuccess(AuthenticationModule authModule, User user)
        {
            SmartServer.sendAllClients(EventCode.AUTHENTICATION_SUCCESS, authModule.serialize(), user.serialize());

            DaoAuthentication daoAuthentication = (DaoAuthentication) DbManager.getDao(AuthenticationEntity.class);            
            daoAuthentication.persist(user, authModule.getAccessType());
        }

        @Override
        public void authenticationFailure(AuthenticationModule authModule, User user)
        {
            SmartServer.sendAllClients(EventCode.AUTHENTICATION_FAILURE, authModule.serialize(), user.serialize());
        }

        @Override
        public void fingerTouched(boolean isMaster)
        {
            SmartServer.sendAllClients(EventCode.FINGER_TOUCHED, Boolean.valueOf(isMaster).toString());
        }

        @Override
        public void fingerprintEnrollmentSample(final byte sampleNumber)
        {
            SmartServer.sendAllClients(EventCode.ENROLLMENT_SAMPLE, String.valueOf(sampleNumber));
        }

        @Override
        public void badgeReaderConnected(boolean isMaster)
        {
            SmartLogger.getLogger().info("Badge reader (" + (isMaster ? "Master" : "Slave") + ") connected.");
        }

        @Override
        public void badgeReaderDisconnected(boolean isMaster)
        {
            SmartLogger.getLogger().info("Badge reader ("+ (isMaster ? "Master" : "Slave")+") disconnected.");
        }

        @Override
        public void badgeScanned(String badgeNumber)
        {
            SmartServer.sendAllClients(EventCode.BADGE_SCANNED, badgeNumber);
        }

        @Override
        public void scanCancelledByDoor()
        {
            SmartLogger.getLogger().info("Scan has been cancelled because someone opened the door.");
            SmartServer.sendAllClients(EventCode.SCAN_CANCELLED_BY_DOOR);
        }

        @Override
        public void temperatureMeasure(double value)
        {
            SmartServer.sendAllClients(EventCode.TEMPERATURE_MEASURE, String.valueOf(value));
        }

        @Override
        public void lightingStarted(List<String> tagsLeft)
        {
            List<String> responsePackets = new ArrayList<>();

            responsePackets.add(EventCode.LIGHTING_STARTED);
            responsePackets.addAll(tagsLeft);

            SmartServer.sendAllClients(responsePackets.toArray(new String[responsePackets.size()]));
        }

        @Override
        public void lightingStopped()
        {
            SmartServer.sendAllClients(EventCode.LIGHTING_STOPPED);
        }

        @Override
        public void deviceStatusChanged(DeviceStatus status)
        {
            SmartServer.sendAllClients(EventCode.STATUS_CHANGED, status.name());
        }

        @Override
        public void flashingProgress(int rowNumber, int rowCount)
        {
            SmartServer.sendAllClients(EventCode.FLASHING_PROGRESS,
                    String.valueOf(rowNumber),
                    String.valueOf(rowCount));
        }
        
        @Override
        public void correlationSample(int correlation, int phaseShift)
        {
            
        }

        @Override
        public void correlationSampleSeries(short[] presentSamples, short[] missingSamples)
        {            
            if(presentSamples == null || missingSamples == null)
            {
                return;
            }
            
            List<String> responsePackets = new ArrayList<>();
            
            responsePackets.add("event_correlation_series");
            
            responsePackets.add("present");
            
            for(short presentSample : presentSamples)
            {
                responsePackets.add(String.valueOf(presentSample));    
            }
            
            responsePackets.add("missing");

            for(short missingSample : missingSamples)
            {
                responsePackets.add(String.valueOf(missingSample));
            }

            SmartServer.sendAllClients(responsePackets.toArray(new String[responsePackets.size()]));
        }
    }
}
