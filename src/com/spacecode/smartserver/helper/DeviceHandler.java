package com.spacecode.smartserver.helper;

import com.spacecode.sdk.device.DeviceCreationException;
import com.spacecode.sdk.device.RfidDevice;
import com.spacecode.sdk.device.data.PluggedDeviceInformation;
import com.spacecode.sdk.device.event.*;
import com.spacecode.sdk.device.module.authentication.FingerprintReader;
import com.spacecode.sdk.network.communication.EventCode;
import com.spacecode.sdk.user.AccessType;
import com.spacecode.sdk.user.GrantedUser;
import com.spacecode.smartserver.SmartServer;
import com.spacecode.smartserver.database.DatabaseHandler;
import com.spacecode.smartserver.database.entity.DeviceEntity;

import java.util.Map;
import java.util.logging.Level;

/**
 * Handle RfidDevice connection, instantiation, disconnection, and events.
 */
public final class DeviceHandler
{
    private volatile static RfidDevice _device;

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

        Map<String, PluggedDeviceInformation> pluggedDevices = RfidDevice.getPluggedDevicesInformation();

        if(pluggedDevices.isEmpty() || pluggedDevices.size() > 1)
        {
            SmartLogger.getLogger().warning("0 or more than 1 device detected.");
            return false;
        }

        for(Map.Entry<String, PluggedDeviceInformation> deviceEntry : pluggedDevices.entrySet())
        {
            PluggedDeviceInformation deviceInfo = deviceEntry.getValue();

            try
            {
                _device = new RfidDevice(null, deviceInfo.getSerialPort());
                _device.addListener(new SmartEventHandler());
            } catch (DeviceCreationException dce)
            {
                SmartLogger.getLogger().log(Level.INFO, "Unable to instantiate a device.", dce);
                return false;
            }

            // take the first device plugged, as we only run if exactly one is available
            break;
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
     * Try to reconnect device five times (waiting 3sec after each try).
     *
     * @return True if reconnection succeeded, false otherwise.
     */
    public static boolean reconnectDevice()
    {
        byte tryStep = 0;
        boolean deviceConnected = false;

        while(!deviceConnected && tryStep < 5)
        {
            deviceConnected = connectDevice();
            ++tryStep;

            try
            {
                Thread.sleep(3000);
            } catch (InterruptedException ie)
            {
                SmartLogger.getLogger().log(Level.INFO, "Interrupted while trying to reconnect Device.", ie);
            }
        }

        return deviceConnected;
    }

    /**
     * @return Currently used RFIDDevice instance (null if not initialized).
     */
    public static RfidDevice getDevice()
    {
        return _device;
    }

    /**
     * Connect the modules (master/slave fingerprint readers, badge readers) using DeviceEntity information.
     * TODO: Retry many times if any module couldn't be initialized/connected
     *
     * @param deviceConfig  DeviceEntity instance to be read to get information about modules.
     */
    public static void connectModules(DeviceEntity deviceConfig)
    {
        if(_device == null || deviceConfig == null)
        {
            return;
        }

        String masterFpReaderSerial = deviceConfig.getFpReaderMasterSerial();
        String slaveFpReaderSerial = deviceConfig.getFpReaderSlaveSerial();

        try
        {
            if(masterFpReaderSerial != null && !"".equals(masterFpReaderSerial.trim()))
            {
                // 2 readers
                if(slaveFpReaderSerial != null && !"".equals(slaveFpReaderSerial.trim()))
                {
                    if(FingerprintReader.connectFingerprintReaders(2) != 2)
                    {
                        SmartLogger.getLogger().warning("Couldn't initialize the two fingerprint readers.");
                    }

                    else if(!
                            (_device.addFingerprintReader(masterFpReaderSerial, true)
                            && _device.addFingerprintReader(slaveFpReaderSerial, false))
                            )
                    {
                        SmartLogger.getLogger().warning("Couldn't connect the two fingerprint readers.");
                    }
                }

                // 1 reader
                else
                {
                    if(FingerprintReader.connectFingerprintReaders() != 1)
                    {
                        SmartLogger.getLogger().warning("Couldn't initialize the fingerprint reader.");
                    }

                    else if(!_device.addFingerprintReader(masterFpReaderSerial, true))
                    {
                        SmartLogger.getLogger().warning("Couldn't connect the fingerprint reader.");
                    }
                }
            }
        } catch (FingerprintReader.FingerprintReaderException fre)
        {
            SmartLogger.getLogger().log(Level.INFO,
                    "An unexpected error occurred during fingerprint readers initialization.", fre);
        }

        int nbOfBadgeReader = deviceConfig.getNbOfBadgeReader();

        if(nbOfBadgeReader == 0)
        {
            return;
        }

        if(nbOfBadgeReader >= 1)
        {
            if(!_device.addBadgeReader("/dev/ttyUSB1", true))
            {
                SmartLogger.getLogger().warning("Unable to add Master Badge Reader.");
            }

            if(nbOfBadgeReader == 2 && !_device.addBadgeReader("/dev/ttyUSB2", false))
            {
                SmartLogger.getLogger().warning("Unable to add Slave Badge Reader.");
            }
        }

        if(deviceConfig.isTemperatureEnabled())
        {
            _device.addTemperatureProbe("tempProbe1", 60, 0.2);
        }
    }

    /**
     * Handle Device events and proceed according to expected SmartServer behavior.
     */
    private static class SmartEventHandler implements DeviceEventHandler, ScanEventHandler, DoorEventHandler,
            AccessControlEventHandler, AuthenticationModuleEventHandler, TemperatureEventHandler
    {
        @Override
        public void deviceDisconnected()
        {
            SmartServer.sendAllClients(EventCode.DEVICE_DISCONNECTED);

            // TODO: device is manually disconnected if serial bridge gets enabled. DO NOT try to reconnectDevice is that case..
            // TODO: Find a way to detect if it's a disconnection because of Serial Bridge command (do nothing) or not (do reconnect)
            // => reconnectDevice()
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
            DatabaseHandler.persistInventory(_device.getLastInventory());

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
        public void authenticationSuccess(GrantedUser grantedUser, AccessType accessType, boolean isMaster)
        {
            SmartServer.sendAllClients(EventCode.AUTHENTICATION_SUCCESS, grantedUser.serialize(),
                    accessType.name(), String.valueOf(isMaster));

            DatabaseHandler.persistAuthentication(grantedUser, accessType);
        }

        @Override
        public void authenticationFailure(GrantedUser grantedUser, AccessType accessType, boolean isMaster)
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
            SmartLogger.getLogger().info("Badge reader ("+ (isMaster ? "Master" : "Slave")+") connected.");
        }

        @Override
        public void badgeReaderDisconnected(boolean isMaster)
        {
            SmartLogger.getLogger().info("Badge reader ("+ (isMaster ? "Master" : "Slave")+") disconnected.");
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
            // TODO : Create temperature handler and subscribe to this event and persist measures etc.
        }
    }
}
