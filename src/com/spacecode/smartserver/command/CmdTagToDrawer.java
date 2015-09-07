package com.spacecode.smartserver.command;


import com.spacecode.sdk.device.Device;
import com.spacecode.sdk.network.communication.RequestCode;
import com.spacecode.smartserver.SmartServer;
import com.spacecode.smartserver.helper.DeviceHandler;
import io.netty.channel.ChannelHandlerContext;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * "TagToDrawer" command.
 *
 * Provide the current "Tag To Axis" map of the {@link Device} instance.
 */
@CommandContract(deviceRequired = true, responseIfInvalid = "")
public class CmdTagToDrawer extends ClientCommand
{
    /**
     * @param ctx           Channel between SmartServer and the client.
     * @param parameters    "Start" and "End" dates (period).
     */
    @Override
    public void execute(ChannelHandlerContext ctx, String[] parameters)
    {
        List<String> responsePackets = new ArrayList<>();
        responsePackets.add(RequestCode.TAG_TO_DRAWER);

        for(Map.Entry<String, Byte> entry : DeviceHandler.getDevice().getTagToDrawerNumber().entrySet())
        {
            responsePackets.add(entry.getKey());
            responsePackets.add(String.valueOf(entry.getValue()));
        }

        SmartServer.sendMessage(ctx, responsePackets.toArray(new String[responsePackets.size()]));
    }
}