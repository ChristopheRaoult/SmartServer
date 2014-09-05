package com.spacecode.smartserver.command;

import com.spacecode.sdk.network.communication.RequestCode;
import com.spacecode.smartserver.DeviceHandler;
import com.spacecode.smartserver.SmartServer;
import io.netty.channel.ChannelHandlerContext;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * StartLighting command.
 */
public class CommandStartLighting extends ClientCommand
{
    /**
     * Build a list of String with provided tags UID's and start lighting order.
     * Send to the context, as a result, the list of tags UID's that could not be lighted.
     * @param ctx                       ChannelHandlerContext instance corresponding to the channel existing between SmartServer and the client.
     * @param parameters                String array containing parameters (if any) provided by the client.
     * @throws ClientCommandException
     */
    @Override
    public void execute(ChannelHandlerContext ctx, String[] parameters) throws ClientCommandException
    {
        // if there is no parameter, then there is no tags to light
        if(parameters.length == 0)
        {
            throw new ClientCommandException("Invalid number of parameters.");
        }

        // create a new editable ArrayList from given tags (in parameters). Tags successfully lighted will be removed from the list.
        List<String> tagsList = new ArrayList<>(Arrays.asList(parameters));
        DeviceHandler.getDevice().startLightingTagsLed(tagsList);

        List<String> responsePackets = new ArrayList<>();

        // add the request code first
        responsePackets.add(RequestCode.START_LIGHTING);

        // then all the tags UID left in the list (=> the ones which have not been lighted).
        responsePackets.addAll(tagsList);

        SmartServer.sendMessage(ctx, responsePackets.toArray(new String[0]));
    }
}
