package com.spacecode.smartserver.command;

import com.spacecode.sdk.network.communication.RequestCode;
import com.spacecode.smartserver.DeviceHandler;
import com.spacecode.smartserver.SmartServer;
import com.spacecode.smartserver.database.DatabaseHandler;
import com.spacecode.smartserver.database.entity.GrantedUserEntity;
import com.spacecode.smartserver.database.repository.GrantedUserRepository;
import com.spacecode.smartserver.database.repository.Repository;
import io.netty.channel.ChannelHandlerContext;

/**
 * RemoveUser command.
 */
public class CommandRemoveUser extends ClientCommand
{
    /**
     * Request to remove a User from granted users list. Send (string) "true" if succeed, "false" otherwise.
     * @param ctx                       ChannelHandlerContext instance corresponding to the channel existing between SmartServer and the client.
     * @param parameters                String array containing parameters (if any) provided by the client.
     * @throws ClientCommandException
     */
    @Override
    public synchronized void execute(ChannelHandlerContext ctx, String[] parameters) throws ClientCommandException
    {
        // waiting for 1 parameter: username of the user to be removed
        if(parameters.length != 1)
        {
            SmartServer.sendMessage(ctx, RequestCode.REMOVE_USER, "false");
            throw new ClientCommandException("Invalid number of parameters.");
        }

        String username = parameters[0];

        if(!DeviceHandler.getDevice().getUsersService().removeUser(username))
        {
            SmartServer.sendMessage(ctx, RequestCode.REMOVE_USER, "false");
            return;
        }

        if(!persistUserDeletion(username))
        {
            SmartServer.sendMessage(ctx, RequestCode.REMOVE_USER, "false");
            return;
        }

        SmartServer.sendMessage(ctx, RequestCode.REMOVE_USER, "true");
    }

    /**
     * Start the user deletion process (user + fingerprints).
     * @param username  Name of to-be-deleted user.
     * @return          True if successful, false otherwise (unknown user, SQLException...).
     */
    private boolean persistUserDeletion(String username)
    {
        Repository userRepo = DatabaseHandler.getRepository(GrantedUserEntity.class);

        if(!(userRepo instanceof GrantedUserRepository))
        {
            // not supposed to happen as the repositories map is filled automatically
            return false;
        }

        GrantedUserEntity gue = ((GrantedUserRepository) userRepo).getByUsername(username);

        if(gue == null)
        {
            // user unknown
            return false;
        }

        return userRepo.delete(gue);
    }
}
