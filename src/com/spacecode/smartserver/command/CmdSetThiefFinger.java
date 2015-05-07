package com.spacecode.smartserver.command;

import com.spacecode.sdk.network.communication.RequestCode;
import com.spacecode.smartserver.SmartServer;
import com.spacecode.smartserver.database.DbManager;
import com.spacecode.smartserver.database.dao.DaoUser;
import com.spacecode.smartserver.database.entity.UserEntity;
import io.netty.channel.ChannelHandlerContext;

/**
 * SetThiefFinger command.
 */
@CommandContract(paramCount = 1)
public class CmdSetThiefFinger extends ClientCommand
{
    /**
     * Request to update an user's "thief finger" index. Return true (if operation succeeded) or false (if failure).
     * 
     * @param ctx                       Channel between SmartServer and the client.
     * @param parameters                Username. Optional: Finger index (if none, the thief finger is set to null).
     */
    @Override
    public synchronized void execute(ChannelHandlerContext ctx, String[] parameters)
    {
        String username = parameters[0];
        Integer fingerIndex = null;

        // if no finger index has been given, we assume the user wants to remove it
        if(parameters.length > 1)
        {
            try
            {
                fingerIndex = Integer.parseInt(parameters[1]);
            } catch (NumberFormatException nfe)
            {
                SmartServer.sendMessage(ctx, RequestCode.SET_THIEF_FINGER, FALSE);
                return;
            }
        }

        DaoUser daoUser = (DaoUser)DbManager.getDao(UserEntity.class);
        if(!daoUser.updateThiefFingerIndex(username, fingerIndex))
        {
            SmartServer.sendMessage(ctx, RequestCode.SET_THIEF_FINGER, FALSE);
            return;
        }

        SmartServer.sendMessage(ctx, RequestCode.SET_THIEF_FINGER, TRUE);
    }
}
