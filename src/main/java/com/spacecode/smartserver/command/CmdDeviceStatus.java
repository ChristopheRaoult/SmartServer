package com.spacecode.smartserver.command;

import com.spacecode.sdk.network.communication.RequestCode;
import com.spacecode.smartserver.SmartServer;
import com.spacecode.smartserver.helper.DeviceHandler;
import io.netty.channel.ChannelHandlerContext;

/**
 * DeviceStatus command.
 * Provide immediate device status. Or DeviceStatus.ERROR.name() ("ERROR") if the device is not available.
 */
@CommandContract(deviceRequired = true, responseIfInvalid = "ERROR")
public class CmdDeviceStatus extends ClientCommand
{
    /**
     * @param ctx           Channel between SmartServer and the client.
     * @param parameters    None expected.
     */
    @Override
    public void execute(ChannelHandlerContext ctx, String[] parameters)
    {
        SmartServer.sendMessage(ctx, RequestCode.DEVICE_STATUS, DeviceHandler.getDevice().getStatus().name());
    }
}
