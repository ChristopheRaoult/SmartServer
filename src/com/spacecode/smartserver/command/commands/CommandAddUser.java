package com.spacecode.smartserver.command.commands;

import com.spacecode.sdk.network.communication.RequestCode;
import com.spacecode.sdk.user.GrantedUser;
import com.spacecode.smartserver.DeviceHandler;
import com.spacecode.smartserver.SmartServer;
import com.spacecode.smartserver.command.ClientCommand;
import com.spacecode.smartserver.command.ClientCommandException;
import io.netty.channel.ChannelHandlerContext;

/**
 * AddUser command.
 */
public class CommandAddUser implements ClientCommand
{
    /**
     * Request to add a new User to granted users list. Send (string) "true" if succeed, "false" otherwise.
     * @param ctx                       ChannelHandlerContext instance corresponding to the channel existing between SmartServer and the client.
     * @param parameters                String array containing parameters (if any) provided by the client.
     * @throws ClientCommandException
     */
    @Override
    public void execute(ChannelHandlerContext ctx, String[] parameters) throws ClientCommandException
    {
        if(parameters.length != 1)
        {
            SmartServer.sendMessage(ctx, RequestCode.ADD_USER, "false");
        }

        GrantedUser newUser = GrantedUser.deserialize(parameters[0]);

        if(newUser == null)
        {
            SmartServer.sendMessage(ctx, RequestCode.ADD_USER, "false");
        }

        boolean result = DeviceHandler.getDevice().getUsersService().addUser(newUser);
        SmartServer.sendMessage(ctx, RequestCode.ADD_USER, result ? "true" : "false");

        // TODO: Persist user in DB if needed (see according to unique Username)
    }
}
