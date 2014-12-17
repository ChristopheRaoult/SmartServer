package com.spacecode.smartserver.helper;

import com.spacecode.sdk.device.DeviceCreationException;
import com.spacecode.sdk.device.RfidDevice;
import com.spacecode.sdk.device.data.PluggedDeviceInformation;
import com.spacecode.sdk.device.event.*;
import com.spacecode.sdk.device.module.authentication.FingerprintReader;
import com.spacecode.sdk.network.communication.EventCode;
import com.spacecode.sdk.user.GrantedUser;
import com.spacecode.sdk.user.data.AccessType;
import com.spacecode.smartserver.SmartServer;
import com.spacecode.smartserver.database.DatabaseHandler;
import com.spacecode.smartserver.database.entity.DeviceEntity;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;

/**
 * Handle RfidDevice connection, instantiation, disconnection, and events.
 */
public final class DeviceHandler
{
    private volatile static RfidDevice _device;

    // allows the CommandSerialBridge to set the current state of device (usb / ethernet).
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

            if(deviceConnected)
            {
                connectModules();
            }

            ++tryStep;

            try
            {
                Thread.sleep(3000);
            } catch (InterruptedException ie)
            {
                SmartLogger.getLogger().log(Level.WARNING, "Interrupted while trying to reconnect Device.", ie);
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
     * Connect the modules (fingerprint / badge readers, temperature probe).
     * TODO: Retry many times if any module couldn't be initialized/connected
     */
    public static void connectModules()
    {
        if(_device == null)
        {
            SmartLogger.getLogger().info("Unable to connect modules [0x0001]");
            return;
        }

        DeviceEntity deviceConfig = DatabaseHandler.getDeviceConfiguration();

        if(deviceConfig == null)
        {
            SmartLogger.getLogger().info("Unable to connect modules [0x0002]");
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
                    if(FingerprintReader.connectFingerprintReader() != 1)
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
            // TODO: Stuck on this point if VirtualHub cannot be contacted or the probe is unavailable
            if(!_device.addTemperatureProbe("tempProbe1", 60, 0.2))
            {
                SmartLogger.getLogger().warning("Unable to add the Temperature probe.");
            }
        }
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
     * Handle Device events and proceed according to expected SmartServer behavior.
     */
    private static class SmartEventHandler implements DeviceEventHandler, ScanEventHandler, DoorEventHandler,
            AccessControlEventHandler, AccessModuleEventHandler, TemperatureEventHandler, LedEventHandler
    {
        @Override
        public void deviceDisconnected()
        {
            SmartServer.sendAllClients(EventCode.DEVICE_DISCONNECTED);
            _device = null;

            // if the device is in ethernet mode, try to reconnect it
            if(!SERIAL_PORT_FORWARDING)
            {
                SmartLogger.getLogger().info("Reconnecting Device...");
                // TODO: reload the last inventory from DB
                reconnectDevice();
            }
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
        }

        @Override
        public void lightingStarted(List<String> tagsLeft)
        {
            List<String> responsePackets = new ArrayList<>();

            responsePackets.add(EventCode.LIGHTING_STARTED);
            responsePackets.addAll(tagsLeft);

            SmartServer.sendAllClients(responsePackets.toArray(new String[0]));
        }
    }
}
