package com.spacecode.smartserver.command;

import com.spacecode.sdk.device.data.DeviceStatus;
import com.spacecode.sdk.network.communication.RequestCode;
import com.spacecode.smartserver.SmartServer;
import com.spacecode.smartserver.helper.DeviceHandler;
import io.netty.channel.ChannelHandlerContext;

import java.util.ArrayList;
import java.util.Arrays;

/**
 * StartLighting command.
 */
public class CmdStartLighting extends ClientCommand
{
    /**
     * Build a list of String with provided tags UID's and start lighting order.
     * Send to the context, as a result, the list of tags UID's that could not be lighted.
     *
     * @param ctx           Channel between SmartServer and the client.
     * @param parameters    String array containing parameters (if any) provided by the client.
     *
     * @throws ClientCommandException
     */
    @Override
    public void execute(ChannelHandlerContext ctx, String[] parameters) throws ClientCommandException
    {
        // if there is no parameter, then there is no tags to light
        if(parameters.length == 0)
        {
            throw new ClientCommandException("Invalid number of parameters [StartLighting].");
        }

        if(DeviceHandler.getDevice() == null)
        {
            SmartServer.sendMessage(ctx, RequestCode.START_LIGHTING, FALSE);
            return;
        }

        if(DeviceHandler.getDevice().getStatus() != DeviceStatus.READY)
        {
            SmartServer.sendMessage(ctx, RequestCode.START_LIGHTING, FALSE);
            return;
        }

        // create a new ArrayList from given tags (in parameters)
        boolean result = DeviceHandler.getDevice().startLightingTagsLed(new ArrayList<>(Arrays.asList(parameters)));
        SmartServer.sendMessage(ctx, RequestCode.START_LIGHTING, result ? TRUE : FALSE);
    }
}
