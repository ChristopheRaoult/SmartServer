package com.spacecode.smartserver.command;

import com.spacecode.sdk.network.communication.RequestCode;
import com.spacecode.sdk.user.GrantedUser;
import com.spacecode.smartserver.SmartServer;
import com.spacecode.smartserver.database.DatabaseHandler;
import com.spacecode.smartserver.helper.DeviceHandler;
import com.spacecode.smartserver.helper.SmartLogger;
import io.netty.channel.ChannelHandlerContext;

/**
 * UpdateBadge command.
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
            SmartServer.sendMessage(ctx, RequestCode.UPDATE_BADGE, FALSE);
            throw new ClientCommandException("Invalid number of parameters.");
        }

        String username = parameters[0];
        String badgeNumber = parameters[1];

        GrantedUser user = DeviceHandler.getDevice().getUsersService().getUserByName(username);

        if(user == null)
        {
            SmartServer.sendMessage(ctx, RequestCode.UPDATE_BADGE, FALSE);
            SmartLogger.getLogger().info("Unable to update badger: user not found.");
            return;
        }

        if(!DatabaseHandler.persistBadgeNumber(username, badgeNumber))
        {
            SmartLogger.getLogger().info("Unable to update badger: persist operation failed.");
            SmartServer.sendMessage(ctx, RequestCode.UPDATE_BADGE, FALSE);
            return;
        }

        user.setBadgeNumber(badgeNumber);
        SmartServer.sendMessage(ctx, RequestCode.UPDATE_BADGE, TRUE);
    }
}
