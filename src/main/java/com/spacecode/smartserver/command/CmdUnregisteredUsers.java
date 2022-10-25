package com.spacecode.smartserver.command;

import com.spacecode.sdk.network.communication.RequestCode;
import com.spacecode.smartserver.SmartServer;
import com.spacecode.smartserver.helper.DeviceHandler;
import io.netty.channel.ChannelHandlerContext;

import java.util.ArrayList;
import java.util.List;

/**
 * Unregistered Users List command.
 */
@CommandContract(deviceRequired = true, responseIfInvalid = "")
public class CmdUnregisteredUsers extends ClientCommand
{
    /**
     * Request to get the unregistered users list. Send the names of unregistered Users as.
     *
     * @param ctx           Channel between SmartServer and the client.
     * @param parameters    None expected.
     */
    @Override
    public void execute(ChannelHandlerContext ctx, String[] parameters)
    {
        List<String> responsePackets = new ArrayList<>();

        // add the request code first
        responsePackets.add(RequestCode.USERS_UNREGISTERED);

        // then all the usernames
        for(String username : DeviceHandler.getDevice().getUsersService().getUnregisteredUsers())
        {
            responsePackets.add(username);
        }

        SmartServer.sendMessage(ctx, responsePackets.toArray(new String[responsePackets.size()]));
    }
}
