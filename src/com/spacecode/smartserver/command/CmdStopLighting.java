package com.spacecode.smartserver.command;

import com.spacecode.sdk.network.communication.RequestCode;
import com.spacecode.smartserver.SmartServer;
import com.spacecode.smartserver.helper.DeviceHandler;
import io.netty.channel.ChannelHandlerContext;

/**
 * StopLighting command.
 */
public class CmdStopLighting extends ClientCommand
{
    /**
     * Send StopLighting command to the current Device. True or false returned to client.
     *
     * @param ctx           Channel between SmartServer and the client.
     * @param parameters    String array containing parameters (if any) provided by the client.
     *
     * @throws ClientCommandException
     */
    @Override
    public void execute(ChannelHandlerContext ctx, String[] parameters) throws ClientCommandException
    {
        if(DeviceHandler.getDevice() == null)
        {
            SmartServer.sendMessage(ctx, RequestCode.STOP_LIGHTING, FALSE);
            return;
        }

        boolean result = DeviceHandler.getDevice().stopLightingTagsLed();
        SmartServer.sendMessage(ctx, RequestCode.STOP_LIGHTING, result ? TRUE : FALSE);
    }
}
