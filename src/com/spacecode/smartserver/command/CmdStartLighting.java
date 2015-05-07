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
@CommandContract(paramCount = 1, deviceRequired = true)
public class CmdStartLighting extends ClientCommand
{
    /**
     * Build a list of String with provided tags UID's and start lighting order.
     * Send to the context, as a result, the list of tags UID's that could not be lighted.
     *
     * @param ctx           Channel between SmartServer and the client.
     * @param parameters    The list of tags to be lighted (at least 1, otherwise the contract is not respected).
     */
    @Override
    public void execute(ChannelHandlerContext ctx, String[] parameters)
    {
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
