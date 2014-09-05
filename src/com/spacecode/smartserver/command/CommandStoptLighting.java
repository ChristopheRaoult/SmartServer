package com.spacecode.smartserver.command;

import com.spacecode.smartserver.DeviceHandler;
import io.netty.channel.ChannelHandlerContext;

/**
 * StopLighting command.
 */
public class CommandStoptLighting extends ClientCommand
{
    /**
     * Send StopLighting command to the current Device. No data is returned to client.
     * @param ctx                       ChannelHandlerContext instance corresponding to the channel existing between SmartServer and the client.
     * @param parameters                String array containing parameters (if any) provided by the client.
     * @throws ClientCommandException
     */
    @Override
    public void execute(ChannelHandlerContext ctx, String[] parameters) throws ClientCommandException
    {
        DeviceHandler.getDevice().stopLightingTagsLed();
    }
}
