package com.spacecode.smartserver.command;

import com.spacecode.sdk.network.communication.RequestCode;
import com.spacecode.sdk.user.GrantedUser;
import com.spacecode.smartserver.DeviceHandler;
import com.spacecode.smartserver.SmartServer;
import io.netty.channel.ChannelHandlerContext;

import java.util.ArrayList;
import java.util.List;

/**
 * AddUser command.
 */
public class CommandUsersList extends ClientCommand
{
    /**
     * Request to get the granted users list. Send the list of GrantedUsers as serialized users (strings).
     * @param ctx                       ChannelHandlerContext instance corresponding to the channel existing between SmartServer and the client.
     * @param parameters                String array containing parameters (if any) provided by the client.
     * @throws ClientCommandException
     */
    @Override
    public void execute(ChannelHandlerContext ctx, String[] parameters) throws ClientCommandException
    {
        List<String> serializedUsers = new ArrayList<>();

        for(GrantedUser user : DeviceHandler.getDevice().getUsersService().getUsersList())
        {
            serializedUsers.add(user.serialize());
        }

        List<String> responsePackets = new ArrayList<>();

        // add the request code first
        responsePackets.add(RequestCode.USERS_LIST);

        // then all the serialized users
        responsePackets.addAll(serializedUsers);

        SmartServer.sendMessage(ctx, responsePackets.toArray(new String[0]));
    }
}
