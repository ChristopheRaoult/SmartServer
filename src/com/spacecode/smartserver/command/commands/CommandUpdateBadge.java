package com.spacecode.smartserver.command.commands;

import com.spacecode.sdk.network.communication.RequestCode;
import com.spacecode.sdk.user.GrantedUser;
import com.spacecode.smartserver.DeviceHandler;
import com.spacecode.smartserver.SmartServer;
import com.spacecode.smartserver.command.ClientCommand;
import com.spacecode.smartserver.command.ClientCommandException;
import com.spacecode.smartserver.database.DatabaseHandler;
import com.spacecode.smartserver.database.entity.GrantedUserEntity;
import com.spacecode.smartserver.database.repository.GrantedUserRepository;
import com.spacecode.smartserver.database.repository.Repository;
import io.netty.channel.ChannelHandlerContext;

/**
 * AddUser command.
 */
public class CommandUpdateBadge implements ClientCommand
{
    /**
     * Request to update an user's badge number. Return true (if operation succeeded) or false (if failure).
     * @param ctx                       ChannelHandlerContext instance corresponding to the channel existing between SmartServer and the client.
     * @param parameters                String array containing parameters (if any) provided by the client.
     * @throws ClientCommandException
     */
    @Override
    public void execute(ChannelHandlerContext ctx, String[] parameters) throws ClientCommandException
    {
        // waiting for 2 parameters: username and new badge number.
        if(parameters.length != 2)
        {
            SmartServer.sendMessage(ctx, RequestCode.UPDATE_BADGE, "false");
            throw new ClientCommandException("Invalid number of parameters.");
        }

        String username = parameters[0];
        String badgeNumber = parameters[1];

        GrantedUser user = DeviceHandler.getDevice().getUsersService().getUserByName(username);

        if(user == null)
        {
            SmartServer.sendMessage(ctx, RequestCode.UPDATE_BADGE, "false");
            return;
        }

        if(!persistNewBadgeNumber(username, badgeNumber))
        {
            SmartServer.sendMessage(ctx, RequestCode.UPDATE_BADGE, "false");
            return;
        }

        user.setBadgeNumber(badgeNumber);
        SmartServer.sendMessage(ctx, RequestCode.UPDATE_BADGE, "true");
    }

    /**
     * Persist new badge number in database.
     * @param username      User to be updated.
     * @param badgeNumber   New badge number.
     * @return              True if success, false otherwise (user not known, SQLException, etc).
     */
    private boolean persistNewBadgeNumber(String username, String badgeNumber)
    {
        Repository userRepo = DatabaseHandler.getRepository(GrantedUserEntity.class);

        if(!(userRepo instanceof GrantedUserRepository))
        {
            // not supposed to happen as the repositories map is filled automatically
            return false;
        }

        return ((GrantedUserRepository)userRepo).updateBadge(username, badgeNumber);
    }
}
