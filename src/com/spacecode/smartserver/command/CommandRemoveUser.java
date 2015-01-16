package com.spacecode.smartserver.command;

import com.spacecode.sdk.network.communication.RequestCode;
import com.spacecode.smartserver.SmartServer;
import com.spacecode.smartserver.database.DbManager;
import com.spacecode.smartserver.database.entity.UserEntity;
import com.spacecode.smartserver.database.repository.UserRepository;
import com.spacecode.smartserver.helper.DeviceHandler;
import io.netty.channel.ChannelHandlerContext;

/**
 * RemoveUser command.
 */
public class CommandRemoveUser extends ClientCommand
{
    /**
     * Request to remove a User from granted users list. Send (string) "true" if succeed, "false" otherwise.
     * @param ctx                       Channel between SmartServer and the client.
     * @param parameters                String array containing parameters (if any) provided by the client.
     * @throws ClientCommandException
     */
    @Override
    public synchronized void execute(ChannelHandlerContext ctx, String[] parameters) throws ClientCommandException
    {
        // waiting for 1 parameter: username of the user to be removed
        if(parameters.length != 1)
        {
            SmartServer.sendMessage(ctx, RequestCode.REMOVE_USER, FALSE);
            throw new ClientCommandException("Invalid number of parameters.");
        }

        if(DeviceHandler.getDevice() == null)
        {
            SmartServer.sendMessage(ctx, RequestCode.REMOVE_USER, FALSE);
            return;
        }

        String username = parameters[0];

        if(!DeviceHandler.getDevice().getUsersService().removeUser(username))
        {
            SmartServer.sendMessage(ctx, RequestCode.REMOVE_USER, FALSE);
            return;
        }

        if(!((UserRepository)DbManager.getRepository(UserEntity.class)).deleteByName(username))
        {
            SmartServer.sendMessage(ctx, RequestCode.REMOVE_USER, FALSE);
            return;
        }

        SmartServer.sendMessage(ctx, RequestCode.REMOVE_USER, TRUE);
    }
}
