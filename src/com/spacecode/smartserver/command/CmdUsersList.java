package com.spacecode.smartserver.command;

import com.spacecode.sdk.network.communication.RequestCode;
import com.spacecode.sdk.user.User;
import com.spacecode.smartserver.SmartServer;
import com.spacecode.smartserver.helper.DeviceHandler;
import io.netty.channel.ChannelHandlerContext;

import java.util.ArrayList;
import java.util.List;

/**
 * Granted Users List command.
 */
public class CmdUsersList extends ClientCommand
{
    /**
     * Request to get the granted users list. Send the list of granted Users as serialized users (strings).
     * 
     * @param ctx                       Channel between SmartServer and the client.
     * @param parameters                String array containing parameters (if any) provided by the client.
     *                                  
     * @throws ClientCommandException
     */
    @Override
    public void execute(ChannelHandlerContext ctx, String[] parameters) throws ClientCommandException
    {
        if(!DeviceHandler.isAvailable())
        {
            SmartServer.sendMessage(ctx, RequestCode.USERS_LIST);
            return;
        }

        List<String> responsePackets = new ArrayList<>();

        // add the request code first
        responsePackets.add(RequestCode.USERS_LIST);

        // then all the serialized users
        for(User user : DeviceHandler.getDevice().getUsersService().getUsers())
        {
            responsePackets.add(user.serialize());
        }

        SmartServer.sendMessage(ctx, responsePackets.toArray(new String[responsePackets.size()]));
    }
}
