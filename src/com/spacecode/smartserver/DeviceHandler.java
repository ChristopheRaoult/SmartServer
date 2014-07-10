package com.spacecode.smartserver;

import com.spacecode.sdk.device.*;
import com.spacecode.sdk.device.data.ConnectionStatus;
import com.spacecode.sdk.device.data.PluggedDeviceInformation;
import com.spacecode.sdk.network.communication.EventCode;
import com.spacecode.sdk.user.AccessType;
import com.spacecode.sdk.user.GrantedUser;

import java.util.Map;

/**
 * Handle RFIDDevice connection, instantiation, disconnection.
 * Provide access to the device.
 */
public final class DeviceHandler
{
    private static RfidDevice _device;

    /** Must not be instantiated. */
    private DeviceHandler()
    {
    }

    /**
     * Looks for available SpaceCode devices. Only 1 can be used and only 1 must be present.
     * Instantiates RFIDDevice.
     * @return True if instantiation succeeded, False if it failed (or if number of devices != 1).
     */
    public static boolean connectDevice()
    {
        if(_device != null && _device.getConnectionStatus() == ConnectionStatus.CONNECTED)
        {
            return true;
        }

        Map<String, PluggedDeviceInformation> pluggedDevices = RfidDevice.getPluggedDevicesInformation();

        if(pluggedDevices.isEmpty() || pluggedDevices.size() > 1)
        {
            ConsoleLogger.warning("0 or more than 1 device detected.");
            return false;
        }

        for(Map.Entry<String, PluggedDeviceInformation> deviceEntry : pluggedDevices.entrySet())
        {
            PluggedDeviceInformation deviceInfo = deviceEntry.getValue();

            try
            {
                switch(deviceInfo.getDeviceType())
                {
                    case RfidDevice.DeviceType.SMARTBOARD:
                        _device = new SmartBoard(null, deviceInfo.getSerialPort());
                        break;

                    case RfidDevice.DeviceType.SMARTDRAWER:
                        _device = new SmartDrawer(null, deviceInfo.getSerialPort());
                        break;

                    default:
                        // device type unknown or not handled => return false.
                        return false;
                }

                _device.addListener(new DeviceEventHandler());
            } catch (DeviceCreationException dce)
            {
                ConsoleLogger.warning("Unable to instantiate a device.", dce);
                // if any DeviceCreationException is thrown => failure => return false.
                return false;
            }

            break;
        }

        return true;
    }

    /**
     * Release current RFIDDevice (if it has been initialized).
     */
    public static void disconnectDevice()
    {
        if(_device != null)
        {
            _device.release();
            _device = null;
        }
    }

    /**
     * Try to reconnect device five times (waiting 3sec after each try).
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
                ConsoleLogger.warning("Interrupted while trying to reconnect Device.", ie);
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
     * Created by Vincent on 30/12/13.
     */
    private static class DeviceEventHandler extends RfidDeviceEventHandler
    {
        @Override
        public void scanStarted()
        {
            SmartServer.sendAllClients(EventCode.EVENT_SCAN_STARTED);
        }

        @Override
        public void scanCompleted()
        {
            SmartServer.sendAllClients(EventCode.EVENT_SCAN_COMPLETED);
        }

        @Override
        public void tagAdded(String tagUID)
        {
            SmartServer.sendAllClients(EventCode.EVENT_TAG_ADDED, tagUID);
        }

        @Override
        public void scanFailed()
        {
            SmartServer.sendAllClients(EventCode.EVENT_SCAN_FAILED);
        }

        @Override
        public void scanCancelledByHost()
        {
            SmartServer.sendAllClients(EventCode.EVENT_SCAN_CANCELLED_BY_HOST);
        }

        @Override
        public void deviceDisconnected()
        {
            SmartServer.sendAllClients(EventCode.EVENT_DEVICE_DISCONNECTED);

            // TODO: device is manually disconnected if serial bridge enabled. DO NOT try to reconnectDevice is that case.
            // => reconnectDevice()
        }

        @Override
        public void fingerTouched(boolean isMaster)
        {
            SmartServer.sendAllClients(EventCode.EVENT_FINGER_TOUCHED, Boolean.valueOf(isMaster).toString());
        }

        @Override
        public void fingerGone(boolean isMaster)
        {
            SmartServer.sendAllClients(EventCode.EVENT_FINGER_GONE, Boolean.valueOf(isMaster).toString());
        }

        @Override
        public void fingerprintReaderConnected(boolean isMaster)
        {
            SmartServer.sendAllClients(EventCode.EVENT_FP_READER_CONNECTED, Boolean.valueOf(isMaster).toString());
        }

        @Override
        public void fingerprintReaderDisconnected(boolean isMaster)
        {
            SmartServer.sendAllClients(EventCode.EVENT_FP_READER_DISCONNECTED, Boolean.valueOf(isMaster).toString());
        }

        @Override
        public void badgeReaderDisconnected(boolean isMaster)
        {
            SmartServer.sendAllClients(EventCode.EVENT_BR_DISCONNECTED, Boolean.valueOf(isMaster).toString());
        }

        @Override
        public void fingerprintEnrollmentSample(byte sampleNumber)
        {
            SmartServer.sendAllClients(EventCode.EVENT_ENROLLMENT_SAMPLE, Byte.valueOf(sampleNumber).toString());
        }

        @Override
        public void authenticationSuccess(GrantedUser grantedUser, AccessType accessType, boolean isMaster)
        {
            super.authenticationSuccess(grantedUser, accessType, isMaster);
        }

        @Override
        public void authenticationFailure(GrantedUser grantedUser, AccessType accessType, boolean isMaster)
        {
            super.authenticationFailure(grantedUser, accessType, isMaster);
        }
    }
}
