package com.spacecode.smartserver.command;

import com.spacecode.sdk.network.communication.RequestCode;
import com.spacecode.smartserver.SmartServer;
import com.spacecode.smartserver.helper.DeviceHandler;
import io.netty.channel.ChannelHandlerContext;

/**
 * StopLighting command.
 */
@CommandContract(deviceRequired = true)
public class CmdStopLighting extends ClientCommand
{
    /**
     * Send StopLighting command to the current Device. True or false returned to client.
     *
     * @param ctx           Channel between SmartServer and the client.
     * @param parameters    None expected.
     */
    @Override
    public void execute(ChannelHandlerContext ctx, String[] parameters)
    {
        boolean result = DeviceHandler.getDevice().stopLightingTagsLed();
        SmartServer.sendMessage(ctx, RequestCode.STOP_LIGHTING, result ? TRUE : FALSE);
    }
}
