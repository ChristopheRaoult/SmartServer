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
public class CommandUpdateBadge extends ClientCommand
{
    /**
     * Request to update an user's badge number. Return true (if operation succeeded) or false (if failure).
     * @param ctx                       ChannelHandlerContext instance corresponding to the channel existing between SmartServer and the client.
     * @param parameters                String array containing parameters (if any) provided by the client.
     * @throws ClientCommandException
     */
    @Override
    public synchronized void execute(ChannelHandlerContext ctx, String[] parameters) throws ClientCommandException
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

        if(!DatabaseHandler.persistBadgeNumber(username, badgeNumber))
        {
            SmartServer.sendMessage(ctx, RequestCode.UPDATE_BADGE, "false");
            return;
        }

        user.setBadgeNumber(badgeNumber);
        SmartServer.sendMessage(ctx, RequestCode.UPDATE_BADGE, "true");
    }
}
