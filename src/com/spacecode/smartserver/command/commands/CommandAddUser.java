package com.spacecode.smartserver.command.commands;

import com.spacecode.sdk.network.communication.RequestCode;
import com.spacecode.sdk.user.GrantedUser;
import com.spacecode.smartserver.DeviceHandler;
import com.spacecode.smartserver.SmartServer;
import com.spacecode.smartserver.command.ClientCommand;
import com.spacecode.smartserver.command.ClientCommandException;
import com.spacecode.smartserver.database.DatabaseHandler;
import com.spacecode.smartserver.database.entity.FingerprintEntity;
import com.spacecode.smartserver.database.entity.GrantedUserEntity;
import com.spacecode.smartserver.database.repository.FingerprintRepository;
import com.spacecode.smartserver.database.repository.GrantedUserRepository;
import com.spacecode.smartserver.database.repository.Repository;
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
        // waiting for only 1 parameter: serialized rantedUser
        if(parameters.length != 1)
        {
            SmartServer.sendMessage(ctx, RequestCode.ADD_USER, "false");
            return;
        }

        GrantedUser newUser = GrantedUser.deserialize(parameters[0]);

        if(newUser == null || newUser.getUsername() == null || "".equals(newUser.getUsername().trim()))
        {
            SmartServer.sendMessage(ctx, RequestCode.ADD_USER, "false");
            return;
        }

        if(!DeviceHandler.getDevice().getUsersService().addUser(newUser))
        {
            SmartServer.sendMessage(ctx, RequestCode.ADD_USER, "false");
            return;
        }

        if(!persistNewUserInDatabase(newUser))
        {
            DeviceHandler.getDevice().getUsersService().removeUser(newUser.getUsername());
            SmartServer.sendMessage(ctx, RequestCode.ADD_USER, "false");
            return;
        }

        SmartServer.sendMessage(ctx, RequestCode.ADD_USER, "true");
    }

    private boolean persistNewUserInDatabase(GrantedUser newUser)
    {
        Repository userRepo = DatabaseHandler.getRepository(GrantedUserEntity.class);
        Repository fpRepo   = DatabaseHandler.getRepository(FingerprintEntity.class);

        if(!(userRepo instanceof GrantedUserRepository))
        {
            return false;
        }

        if(!(fpRepo instanceof FingerprintRepository))
        {
            return false;
        }

        return ((GrantedUserRepository) userRepo).insertNewUser(newUser, (FingerprintRepository) fpRepo);
    }
}
