package com.spacecode.smartserver.command;

import com.spacecode.sdk.network.communication.RequestCode;
import com.spacecode.smartserver.SmartServer;
import com.spacecode.smartserver.helper.DeviceHandler;
import io.netty.channel.ChannelHandlerContext;

/**
 * Initialization command.
 * Provide basic information of current device (serial number, device type, hardware and software version).
 */
public class CommandInitialization extends ClientCommand
{
    /**
     * Send a message to the current context containing basic information (serial number, device type, hardware version, software version, Device Status).
     *
     * @param ctx           ChannelHandlerContext instance corresponding to the channel existing between SmartServer and the client.
     * @param parameters    String array containing parameters (if any) provided by the client.
     *
     * @throws ClientCommandException
     */
    @Override
    public void execute(ChannelHandlerContext ctx, String[] parameters) throws ClientCommandException
    {
        if(DeviceHandler.getDevice() == null)
        {
            return;
        }

        SmartServer.sendMessage(ctx,
                RequestCode.INITIALIZATION,
                DeviceHandler.getDevice().getSerialNumber(),
                DeviceHandler.getDevice().getDeviceType().name(),
                DeviceHandler.getDevice().getHardwareVersion(),
                DeviceHandler.getDevice().getSoftwareVersion(),
                DeviceHandler.getDevice().getStatus().name()
        );
    }
}