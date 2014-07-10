package com.spacecode.smartserver.command.commands;

import com.spacecode.smartserver.ConsoleLogger;
import com.spacecode.smartserver.DeviceHandler;
import com.spacecode.smartserver.command.ClientCommand;
import com.spacecode.smartserver.command.ClientCommandException;
import io.netty.channel.ChannelHandlerContext;

import java.io.IOException;

/**
 * SerialBridge command.
 * /drivers/usb/gadget/composite.c has been modified to raise an uevent when the USB OTG of ARM Board is plugged/unplugged.
 * udev rules have been created to run a shell script which send this SerialBridge command.
 * The intent is to get the server to sleep while it's running the serial port forwarding.
 * Forwarding is made with "socat". From port ttyGS0 (g_serial emulated port) to ttyUSB0 (FTDI_IO USB to Serial port).
 * If the command is sent again, then stop the port forwarding and wake the server up.
 */
public class CommandSerialBridge implements ClientCommand
{
    private static Process _portForwardingProcess = null;
    private static String _pfwStartCmd = "socat /dev/ttyGS0,raw,echo=0,crnl /dev/ttyUSB0,raw,echo=0,crnl";
    private static String _pfwEndCmd = "pkill -f socat";

    /**
     * According to parameter ("ON"/"OFF"), enable or disable Serial Port forwarding ("Serial Bridge").
     * @param ctx                       ChannelHandlerContext instance corresponding to the channel existing between SmartServer and the client.
     * @param parameters                String array containing parameters (if any) provided by the client.
     * @throws ClientCommandException
     */
    @Override
    public void execute(ChannelHandlerContext ctx, String[] parameters) throws ClientCommandException
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
                // release the device serial port
                DeviceHandler.disconnectDevice();

                // execute command for port forwarding
                _portForwardingProcess = new ProcessBuilder( "/bin/sh", "-c", _pfwStartCmd ).start();
            } catch (IOException ioe)
            {
                ConsoleLogger.warning("Unable to run Port Forwarding command.", ioe);

                // reconnect to local Device
                DeviceHandler.connectDevice();
            }
        }

        else
        {
            if(_portForwardingProcess == null)
            {
                return;
            }

            // stop the process (port forwarding)
            // NOTE: calling destroy() on Process instance does not stop "socat"...
            Process killingSocatProcess = null;
            try
            {
                killingSocatProcess = new ProcessBuilder( "/bin/sh", "-c", _pfwEndCmd).start();
                killingSocatProcess.waitFor();

                _portForwardingProcess = null;

                // reconnect to local Device
                DeviceHandler.connectDevice();
            } catch (IOException | InterruptedException e)
            {
                ConsoleLogger.warning("Unable to stop Port Forwarding command.", e);
            }
        }
    }
}
