package com.spacecode.smartserver.command;

import com.spacecode.sdk.network.communication.RequestCode;
import com.spacecode.sdk.user.User;
import com.spacecode.smartserver.SmartServer;
import com.spacecode.smartserver.database.DbManager;
import com.spacecode.smartserver.database.dao.DaoUser;
import com.spacecode.smartserver.database.entity.UserEntity;
import com.spacecode.smartserver.helper.DeviceHandler;
import io.netty.channel.ChannelHandlerContext;

/**
 * AddUser command.
 */
@CommandContract(paramCount = 1, deviceRequired = true)
public class CmdAddUser extends ClientCommand
{
    /**
     * Request to add a new User to granted users list. Send (string) "true" if succeed, "false" otherwise.
     *
     * @param ctx           Channel between SmartServer and the client.
     * @param parameters    Serialized User. Optional 2nd parameter: True or False ("Override Templates"). 
     */
    @Override
    public synchronized void execute(ChannelHandlerContext ctx, String[] parameters)
    {
        User newUser = User.deserialize(parameters[0]);
        
        if(newUser == null)            
        {
            // invalid user: deserialization failed
            SmartServer.sendMessage(ctx, RequestCode.ADD_USER, FALSE);
            return;
        }
        
        String username = newUser.getUsername();
        
        if(username == null || username.trim().isEmpty())
        {
            // invalid username => reject the request
            SmartServer.sendMessage(ctx, RequestCode.ADD_USER, FALSE);
            return;
        }
        
        if(DeviceHandler.getDevice().getUsersService().getUserByName(username) != null)
        {
            // user already registered by the device => reject the request
            SmartServer.sendMessage(ctx, RequestCode.ADD_USER, FALSE);
            return;
        }

        DaoUser daoUser = (DaoUser) DbManager.getDao(UserEntity.class);
        
        // try to get a user with the same name in the DB
        UserEntity userEntity = daoUser.getByUsername(username);

        if(userEntity != null)
        {
            // a user with the same name exists in the DB => reject the request
            SmartServer.sendMessage(ctx, RequestCode.ADD_USER, FALSE);
            return;
        }

        if(!DeviceHandler.getDevice().getUsersService().addUser(newUser))
        {
            SmartServer.sendMessage(ctx, RequestCode.ADD_USER, FALSE);
            return;
        }

        if(!daoUser.persist(newUser))
        {
            // if insert in db failed, remove user from local users.
            DeviceHandler.getDevice().getUsersService().removeUser(username);
            SmartServer.sendMessage(ctx, RequestCode.ADD_USER, FALSE);
            return;
        }

        SmartServer.sendMessage(ctx, RequestCode.ADD_USER, TRUE);
    }
}
