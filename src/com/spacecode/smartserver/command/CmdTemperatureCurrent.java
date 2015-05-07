package com.spacecode.smartserver.command;

import com.spacecode.sdk.network.communication.RequestCode;
import com.spacecode.smartserver.SmartServer;
import com.spacecode.smartserver.helper.DeviceHandler;
import io.netty.channel.ChannelHandlerContext;

/**
 * "TemperatureCurrent" command.
 *
 * Provide device's last temperature (if any) or TemperatureProbe.ERROR_VALUE ("777").
 */
@CommandContract(deviceRequired = true, responseWhenInvalid = "777")
public class CmdTemperatureCurrent extends ClientCommand
{
    /**
     * @param ctx           Channel between SmartServer and the client.
     * @param parameters    None expected.
     */
    @Override
    public void execute(ChannelHandlerContext ctx, String[] parameters)
    {
        SmartServer.sendMessage(ctx, RequestCode.TEMPERATURE_CURRENT,
                String.valueOf(DeviceHandler.getDevice().getCurrentTemperature()));
    }
}
