package com.spacecode.smartserver.command;

import com.spacecode.sdk.device.module.TemperatureProbe;
import com.spacecode.sdk.network.communication.RequestCode;
import com.spacecode.smartserver.SmartServer;
import com.spacecode.smartserver.helper.DeviceHandler;
import io.netty.channel.ChannelHandlerContext;

/**
 * "TemperatureCurrent" command.
 *
 * Provide device's last temperature (if any) or TemperatureProbe.ERROR_VALUE.
 */
public class CommandTempCurrent extends ClientCommand
{
    /**
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
            SmartServer.sendMessage(ctx, RequestCode.TEMP_CURRENT, String.valueOf(TemperatureProbe.ERROR_VALUE));
            return;
        }

        SmartServer.sendMessage(ctx, RequestCode.TEMP_CURRENT,
                String.valueOf(DeviceHandler.getDevice().getCurrentTemperature()));
    }
}
