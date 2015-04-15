package com.spacecode.smartserver.command;

import com.spacecode.sdk.device.data.DeviceStatus;
import com.spacecode.sdk.network.communication.RequestCode;
import com.spacecode.smartserver.SmartServer;
import com.spacecode.smartserver.helper.DeviceHandler;
import io.netty.channel.ChannelHandlerContext;

/**
 * DeviceStatus command.
 * Provide immediate device status.
 */
public class CmdDeviceStatus extends ClientCommand
{
    /**
     * @param ctx                       Channel between SmartServer and the client.
     * @param parameters                String array containing parameters (if any) provided by the client.
     *
     * @throws ClientCommandException
     */
    @Override
    public void execute(ChannelHandlerContext ctx, String[] parameters) throws ClientCommandException
    {
        if(!DeviceHandler.isAvailable())
        {
            SmartServer.sendMessage(ctx, RequestCode.DEVICE_STATUS, DeviceStatus.ERROR.name());
            return;
        }

        SmartServer.sendMessage(ctx, RequestCode.DEVICE_STATUS, DeviceHandler.getDevice().getStatus().name());
    }
}
