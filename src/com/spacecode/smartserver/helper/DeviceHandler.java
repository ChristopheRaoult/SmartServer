package com.spacecode.smartserver.helper;

import com.spacecode.sdk.device.Device;
import com.spacecode.sdk.device.DeviceCreationException;
import com.spacecode.sdk.device.data.DeviceStatus;
import com.spacecode.sdk.device.data.Inventory;
import com.spacecode.sdk.device.data.PluggedDeviceInformation;
import com.spacecode.sdk.device.event.*;
import com.spacecode.sdk.device.module.authentication.FingerprintReader;
import com.spacecode.sdk.device.module.authentication.FingerprintReaderException;
import com.spacecode.sdk.network.communication.EventCode;
import com.spacecode.sdk.user.User;
import com.spacecode.sdk.user.data.AccessType;
import com.spacecode.smartserver.SmartServer;
import com.spacecode.smartserver.database.DbManager;
import com.spacecode.smartserver.database.entity.AuthenticationEntity;
import com.spacecode.smartserver.database.entity.InventoryEntity;
import com.spacecode.smartserver.database.entity.UserEntity;
import com.spacecode.smartserver.database.repository.AuthenticationRepository;
import com.spacecode.smartserver.database.repository.InventoryRepository;
import com.spacecode.smartserver.database.repository.UserRepository;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;

/**
 * Handle RfidDevice connection, instantiation, disconnection, and events.
 */
public final class DeviceHandler
{
    private volatile static Device _device;

    // allows the CmdSerialBridge to set the current state of device (usb / ethernet).
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
        if(_device != null)
        {
            return true;
        }

        Map<String, PluggedDeviceInformation> pluggedDevices = Device.getPluggedDevicesInformation();

        if(pluggedDevices.isEmpty() || pluggedDevices.size() > 1)
        {
            SmartLogger.getLogger().warning("0 or more than 1 device detected.");
            return false;
        }

        PluggedDeviceInformation deviceInfo = pluggedDevices.entrySet().iterator().next().getValue();

        try
        {
            _device = new Device(null, deviceInfo.getSerialPort());
            _device.addListener(new SmartEventHandler());
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
        if(_device != null)
        {
            _device.release();
            _device = null;
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

                // reconnect modules, reload the users and the last inventory
                connectModules();

                if(!loadAuthorizedUsers())
                {
                    SmartLogger.getLogger().warning("Failed on loading authorized users when reconnecting the Device.");
                }

                if(!loadLastInventory())
                {
                    SmartLogger.getLogger().warning("Failed on loading the last inventory when reconnecting the Device.");
                }
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
     * @return Currently used RFIDDevice instance (null if not initialized).
     */
    public static Device getDevice()
    {
        return _device;
    }

    /**
     * Connect the modules (fingerprint / badge readers, temperature probe).
     */
    public static void connectModules()
    {
        if(_device == null)
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
                    if(FingerprintReader.connectFingerprintReaders(2) != 2)
                    {
                        SmartLogger.getLogger().warning("Couldn't initialize the two fingerprint readers.");
                    }

                    else if(!
                            (_device.addFingerprintReader(fprMaster, true)
                            && _device.addFingerprintReader(fprSlave, false))
                            )
                    {
                        SmartLogger.getLogger().warning("Couldn't connect the two fingerprint readers.");
                    }
                }

                // 1 reader
                else
                {
                    if(FingerprintReader.connectFingerprintReader() != 1)
                    {
                        SmartLogger.getLogger().warning("Couldn't initialize the fingerprint reader.");
                    }

                    else if(!_device.addFingerprintReader(fprMaster, true))
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
            if(!_device.addBadgeReader(brMaster, true))
            {
                SmartLogger.getLogger().warning("Unable to add Master Badge Reader on "+brMaster);
            }

            if(brSlave != null && !brSlave.trim().isEmpty() && !_device.addBadgeReader(brSlave, false))
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
            if(!_device.addTemperatureProbe("tempProbe1", measurementDelay, measurementDelta))
            {
                SmartLogger.getLogger().warning("Unable to add the Temperature probe.");
            }
        }
    }

    /** Disconnect the temperature probe, if any, and reconnect it using last parameters (see ConfManager).
     */
    public static void reloadTemperatureProbe()
    {
        _device.disconnectTemperatureProbe();
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
     * @return True if serial port is being forwarded through host USB-OTG. False otherwise.
     */
    public static boolean isForwardingSerialPort()
    {
        return SERIAL_PORT_FORWARDING;
    }

    /**
     * Get the list of Authorized Users (from UserRepository) and load them in the UsersService.
     *
     * @return True if the operation succeeded, false otherwise.
     */
    public static boolean loadAuthorizedUsers()
    {
        if(_device == null)
        {
            return false;
        }

        UserRepository userRepo = (UserRepository) DbManager.getRepository(UserEntity.class);
        List<User> notAddedUsers = _device.getUsersService().addUsers(userRepo.getAuthorizedUsers());

        if(!notAddedUsers.isEmpty())
        {
            SmartLogger.getLogger().warning(notAddedUsers.size() + " Users could not be loaded.");
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
        Inventory lastInventoryRecorded = ((InventoryRepository)DbManager.getRepository(InventoryEntity.class))
            .getLastInventory();

        if(lastInventoryRecorded == null)
        {
            return false;
        }

        _device.setLastInventory(lastInventoryRecorded);
        return true;
    }

    /**
     * Handle Device events and proceed according to expected SmartServer behavior.
     */
    static class SmartEventHandler implements DeviceEventHandler, ScanEventHandler, DoorEventHandler,
            AccessControlEventHandler, AccessModuleEventHandler, TemperatureEventHandler, LedEventHandler,
            MaintenanceEventHandler
    {
        @Override
        public void deviceDisconnected()
        {
            SmartLogger.getLogger().info("Device Disconnected...");

            SmartServer.sendAllClients(EventCode.DEVICE_DISCONNECTED);
            _device = null;

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
            // todo: thread this operation? The point is about "getLastInventory" command, which MUST return the VERY last
            ((InventoryRepository)DbManager.getRepository(InventoryEntity.class)).persist(_device.getLastInventory());

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
        public void authenticationSuccess(User grantedUser, AccessType accessType, boolean isMaster)
        {
            SmartServer.sendAllClients(EventCode.AUTHENTICATION_SUCCESS, grantedUser.serialize(),
                    accessType.name(), String.valueOf(isMaster));

            ((AuthenticationRepository)DbManager.getRepository(AuthenticationEntity.class))
                    .persist(grantedUser, accessType);
        }

        @Override
        public void authenticationFailure(User grantedUser, AccessType accessType, boolean isMaster)
        {
            SmartServer.sendAllClients(EventCode.AUTHENTICATION_FAILURE, grantedUser.serialize(),
                    accessType.name(), String.valueOf(isMaster));
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
        public void tagPresence()
        {
            SmartLogger.getLogger().info("Tag presence.");
        }

        @Override
        public void scanCancelledByDoor()
        {
            SmartLogger.getLogger().info("Scan has been cancelled because someone opened the door.");
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
