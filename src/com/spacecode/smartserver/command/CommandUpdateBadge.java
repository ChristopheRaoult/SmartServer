package com.spacecode.smartserver.command;

import com.spacecode.sdk.network.communication.RequestCode;
import com.spacecode.smartserver.SmartServer;
import com.spacecode.smartserver.database.DbManager;
import com.spacecode.smartserver.database.entity.UserEntity;
import com.spacecode.smartserver.database.repository.UserRepository;
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
     * @param ctx                       Channel between SmartServer and the client.
     * @param parameters                String array containing parameters (if any) provided by the client.
     * @throws ClientCommandException
     */
    @Override
    public synchronized void execute(ChannelHandlerContext ctx, String[] parameters) throws ClientCommandException
    {
        // waiting for 2 parameters: username and badge number. Badge number is optional (none = empty badge number)
        if(parameters.length < 1)
        {
            SmartServer.sendMessage(ctx, RequestCode.UPDATE_BADGE, FALSE);
            throw new ClientCommandException("Invalid number of parameters.");
        }

        if(DeviceHandler.getDevice() == null)
        {
            SmartServer.sendMessage(ctx, RequestCode.UPDATE_BADGE, FALSE);
            return;
        }

        String username = parameters[0];
        String badgeNumber = parameters.length > 1 ? parameters[1] : "";

        if(!((UserRepository)DbManager.getRepository(UserEntity.class)).updateBadgeNumber(username, badgeNumber))
        {
            SmartLogger.getLogger().warning("Unable to update badge number: DB operation failed.");
            SmartLogger.getLogger().warning("Make sure that the user "+username+" exists.");
            SmartServer.sendMessage(ctx, RequestCode.UPDATE_BADGE, FALSE);
            return;
        }

        if(!DeviceHandler.getDevice().getUsersService().updateBadgeNumber(username, badgeNumber))
        {
            SmartLogger.getLogger().warning("Unable to update badge number: Users Service failed.");
            SmartLogger.getLogger().warning("Make sure that the user "+username+" exists.");
            SmartServer.sendMessage(ctx, RequestCode.UPDATE_BADGE, FALSE);
            return;
        }

        SmartServer.sendMessage(ctx, RequestCode.UPDATE_BADGE, TRUE);
    }
}
