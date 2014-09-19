package com.spacecode.smartserver.command;

import com.spacecode.sdk.network.communication.RequestCode;
import com.spacecode.smartserver.SmartServer;
import com.spacecode.smartserver.helper.DeviceHandler;
import io.netty.channel.ChannelHandlerContext;

/**
 * Temperature command.
 * Provide device's last temperature (if any) or Double.MIN_VALUE.
 */
public class CommandTemperature extends ClientCommand
{
    /**
     * Serialize device's last inventory and send it to current context.
     *
     * @param ctx                       ChannelHandlerContext instance corresponding to the channel existing between SmartServer and the client.
     * @param parameters                String array containing parameters (if any) provided by the client.
     *
     * @throws ClientCommandException
     */
    @Override
    public void execute(ChannelHandlerContext ctx, String[] parameters) throws ClientCommandException
    {
        SmartServer.sendMessage(ctx, RequestCode.TEMPERATURE,
                String.valueOf(DeviceHandler.getDevice().getCurrentTemperature()));
    }
}
