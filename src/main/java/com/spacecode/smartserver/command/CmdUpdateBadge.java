package com.spacecode.smartserver.command;

import com.spacecode.sdk.network.communication.RequestCode;
import com.spacecode.sdk.user.User;
import com.spacecode.smartserver.SmartServer;
import com.spacecode.smartserver.database.DbManager;
import com.spacecode.smartserver.database.dao.DaoUser;
import com.spacecode.smartserver.database.entity.UserEntity;
import com.spacecode.smartserver.helper.DeviceHandler;
import com.spacecode.smartserver.helper.SmartLogger;
import io.netty.channel.ChannelHandlerContext;

/**
 * UpdateBadge command.
 */
@CommandContract(paramCount = 1, deviceRequired = true)
public class CmdUpdateBadge extends ClientCommand
{
    /**
     * Request to update an user's badge number. Return true (if operation succeeded) or false (if failure).
     * 
     * @param ctx           Channel between SmartServer and the client.
     * @param parameters    Username. Optional: badge number (if none is given, the badge is empty).
     */
    @Override
    public synchronized void execute(ChannelHandlerContext ctx, String[] parameters)
    {
        String username = parameters[0];
        String badgeNumber = parameters.length > 1 ? parameters[1] : "";

        User user = DeviceHandler.getDevice().getUsersService().getUserByName(username);
        String badgeSave = "";
        
        if(user != null)
        {
            badgeSave = user.getBadgeNumber();
        }
        
        if(!DeviceHandler.getDevice().getUsersService().updateBadgeNumber(username, badgeNumber))
        {
            SmartServer.sendMessage(ctx, RequestCode.UPDATE_BADGE, FALSE);
            return;
        }

        DaoUser daoUser = (DaoUser)DbManager.getDao(UserEntity.class);
        if(!daoUser.updateBadgeNumber(username, badgeNumber))
        {
            // restore the old badge number, as we can't save the new one in the DB
            DeviceHandler.getDevice().getUsersService().updateBadgeNumber(username, badgeSave);
            
            SmartLogger.getLogger().warning(String.format("Unable to persist badge  number for %s", username));
            SmartServer.sendMessage(ctx, RequestCode.UPDATE_BADGE, FALSE);
            return;
        }

        SmartServer.sendMessage(ctx, RequestCode.UPDATE_BADGE, TRUE);
    }
}
