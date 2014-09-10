package com.spacecode.smartserver.command;

import com.spacecode.sdk.network.communication.RequestCode;
import com.spacecode.sdk.user.GrantedUser;
import com.spacecode.smartserver.DeviceHandler;
import com.spacecode.smartserver.SmartServer;
import com.spacecode.smartserver.database.DatabaseHandler;
import io.netty.channel.ChannelHandlerContext;

/**
 * AddUser command.
 */
public class CommandAddUser extends ClientCommand
{
    /**
     * Request to add a new User to granted users list. Send (string) "true" if succeed, "false" otherwise.
     * @param ctx                       ChannelHandlerContext instance corresponding to the channel existing between SmartServer and the client.
     * @param parameters                String array containing parameters (if any) provided by the client.
     * @throws ClientCommandException
     */
    @Override
    public synchronized void execute(ChannelHandlerContext ctx, String[] parameters) throws ClientCommandException
    {
        // waiting for only 1 parameter: serialized rantedUser
        if(parameters.length != 1)
        {
            SmartServer.sendMessage(ctx, RequestCode.ADD_USER, FALSE);
            throw new ClientCommandException("Invalid number of parameters.");
        }

        GrantedUser newUser = GrantedUser.deserialize(parameters[0]);

        if(newUser == null || newUser.getUsername() == null || "".equals(newUser.getUsername().trim()))
        {
            SmartServer.sendMessage(ctx, RequestCode.ADD_USER, FALSE);
            return;
        }

        if(!DeviceHandler.getDevice().getUsersService().addUser(newUser))
        {
            SmartServer.sendMessage(ctx, RequestCode.ADD_USER, FALSE);
            return;
        }

        if(!DatabaseHandler.persistUser(newUser))
        {
            // if insert in db failed, remove user from local users.
            DeviceHandler.getDevice().getUsersService().removeUser(newUser.getUsername());
            SmartServer.sendMessage(ctx, RequestCode.ADD_USER, FALSE);
            return;
        }

        SmartServer.sendMessage(ctx, RequestCode.ADD_USER, TRUE);
    }
}
