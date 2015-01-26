package com.spacecode.smartserver.command;

import com.spacecode.smartserver.helper.DeviceHandler;
import com.spacecode.smartserver.helper.SmartLogger;
import io.netty.channel.ChannelHandlerContext;

import java.io.IOException;
import java.util.logging.Level;

/**
 * SerialBridge command.
 *
 * /drivers/usb/gadget/composite.c has been modified to raise an uevent when the USB OTG of ARM Board is plugged/unplugged.
 * udev rules have been created to run a shell script which send this SerialBridge command.
 * The intent is to get the server to sleep while it's running the serial port forwarding.
 * Forwarding is made with "socat". From port ttyGS0 (g_serial emulated port) to ttyUSB0 (FTDI_IO USB to Serial port).
 * If the command is sent again, then stop the port forwarding and wake the server up.
 */
public class CmdSerialBridge extends ClientCommand
{
    private static Process _portForwardingProcess = null;
    private static String _pfwStartCmd = "socat /dev/ttyGS0,raw,echo=0,crnl /dev/ttyUSB0,raw,echo=0,crnl";
    private static String _pfwEndCmd = "pkill -f socat";

    /**
     * According to parameter ("ON"/"OFF"), enable or disable Serial Port forwarding ("Serial Bridge").
     *
     * @param ctx           Channel between SmartServer and the client.
     * @param parameters    String array containing parameters (if any) provided by the client.
     *
     * @throws ClientCommandException
     */
    @Override
    public synchronized void execute(ChannelHandlerContext ctx, String[] parameters) throws ClientCommandException
    {
        if(parameters.length == 0)
        {
            return;
        }

        if(!"ON".equals(parameters[0]) && !"OFF".equals(parameters[0]))
        {
            return;
        }

        boolean openBridge = "ON".equals(parameters[0]);

        if(openBridge)
        {
            if(_portForwardingProcess != null)
            {
                return;
            }

            try
            {
                // tell the device handler that serial port is hooked, then it doesn't try to reconnect device
                DeviceHandler.setForwardingSerialPort(true);

                // disconnect the device, release the serial port
                DeviceHandler.disconnectDevice();

                // execute command for port forwarding
                _portForwardingProcess = new ProcessBuilder( "/bin/sh", "-c", _pfwStartCmd ).start();

                SmartLogger.getLogger().severe("Running Port Forwarding command.");
            } catch (IOException ioe)
            {
                SmartLogger.getLogger().log(Level.SEVERE, "Unable to run Port Forwarding command.", ioe);

                // reconnect to local Device
                DeviceHandler.reconnectDevice();
            }
        }

        else
        {
            if(_portForwardingProcess == null)
            {
                return;
            }

            try
            {
                // stop the process (port forwarding)
                // NOTE: calling destroy() on Process instance does not stop "socat"...
                Process killingSocatProcess = new ProcessBuilder( "/bin/sh", "-c", _pfwEndCmd).start();
                killingSocatProcess.waitFor();

                _portForwardingProcess = null;

                SmartLogger.getLogger().severe("Stopped Port Forwarding command. Reconnecting Device...");

                // reconnect to local Device
                DeviceHandler.reconnectDevice();
                DeviceHandler.setForwardingSerialPort(false);
            } catch (IOException | InterruptedException e)
            {
                SmartLogger.getLogger().log(Level.SEVERE, "Unable to stop Port Forwarding command.", e);
            }
        }
    }
}
