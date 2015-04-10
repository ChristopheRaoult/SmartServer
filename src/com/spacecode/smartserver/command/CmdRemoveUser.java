package com.spacecode.smartserver.command;

import com.spacecode.sdk.network.communication.RequestCode;
import com.spacecode.sdk.user.User;
import com.spacecode.sdk.user.data.GrantType;
import com.spacecode.smartserver.SmartServer;
import com.spacecode.smartserver.database.DbManager;
import com.spacecode.smartserver.database.dao.DaoUser;
import com.spacecode.smartserver.database.entity.UserEntity;
import com.spacecode.smartserver.helper.DeviceHandler;
import com.spacecode.smartserver.helper.SmartLogger;
import io.netty.channel.ChannelHandlerContext;

/**
 * RemoveUser command.
 */
public class CmdRemoveUser extends ClientCommand
{
    /**
     * Request to remove a User from granted users list. Send (string) "true" if succeed, "false" otherwise.
     * 
     * @param ctx                       Channel between SmartServer and the client.
     * @param parameters                String array containing parameters (if any) provided by the client.
     *                                  
     * @throws ClientCommandException
     */
    @Override
    public synchronized void execute(ChannelHandlerContext ctx, String[] parameters) throws ClientCommandException
    {
        // waiting for 1 parameter: username of the user to be removed
        if(parameters.length != 1)
        {
            SmartServer.sendMessage(ctx, RequestCode.REMOVE_USER, FALSE);
            throw new ClientCommandException("Invalid number of parameters [RemoveUser].");
        }

        if(DeviceHandler.getDevice() == null)
        {
            SmartServer.sendMessage(ctx, RequestCode.REMOVE_USER, FALSE);
            return;
        }

        String username = parameters[0];
        
        User user = DeviceHandler.getDevice().getUsersService().getUserByName(username);
        
        if(user == null)
        {
            SmartServer.sendMessage(ctx, RequestCode.REMOVE_USER, FALSE);
            return;
        }
        
        GrantType grantSave = user.getPermission();

        if(!DeviceHandler.getDevice().getUsersService().removeUser(username))
        {
            SmartServer.sendMessage(ctx, RequestCode.REMOVE_USER, FALSE);
            return;
        }

        DaoUser daoUser = (DaoUser)DbManager.getDao(UserEntity.class);
        if(daoUser == null || !daoUser.removePermission(username))
        {
            // if the permission could not be removed from DB, get the user back!
            DeviceHandler.getDevice().getUsersService().updatePermission(username, grantSave);
            
            // reject the request
            SmartLogger.getLogger()
                    .warning(username + " removed from authorized users, but permission was not removed from DB.");
            SmartServer.sendMessage(ctx, RequestCode.REMOVE_USER, FALSE);
            return;
        }

        SmartServer.sendMessage(ctx, RequestCode.REMOVE_USER, TRUE);
    }
}
