package com.spacecode.smartserver.command;

import com.spacecode.sdk.network.communication.RequestCode;
import com.spacecode.sdk.user.data.GrantType;
import com.spacecode.smartserver.SmartServer;
import com.spacecode.smartserver.database.DbManager;
import com.spacecode.smartserver.database.dao.DaoGrantedAccess;
import com.spacecode.smartserver.database.entity.GrantedAccessEntity;
import com.spacecode.smartserver.helper.DeviceHandler;
import com.spacecode.smartserver.helper.SmartLogger;
import io.netty.channel.ChannelHandlerContext;

import java.util.logging.Level;

/**
 * UpdatePermission command.
 */
@CommandContract(paramCount = 2, strictCount = true, deviceRequired = true)
public class CmdUpdatePermission extends ClientCommand
{
    /**
     * Request to update an user's permission type to this device. Return true if operation succeeded, false otherwise.
     *
     * @param ctx           Channel between SmartServer and the client.
     * @param parameters    Username, GrantType.
     */
    @Override
    public synchronized void execute(ChannelHandlerContext ctx, String[] parameters)
    {
        String username = parameters[0];
        String newPermission = parameters[1];
        GrantType grantType;

        if(username.trim().isEmpty())
        {
            SmartServer.sendMessage(ctx, RequestCode.UPDATE_PERMISSION, FALSE);
            return;
        }
        
        try
        {
            grantType = GrantType.valueOf(newPermission);
        } catch(IllegalArgumentException iae)
        {
            SmartLogger.getLogger().log(Level.SEVERE, "Invalid GrantType for permission update", iae);
            SmartServer.sendMessage(ctx, RequestCode.UPDATE_PERMISSION, FALSE);
            return;
        }

        boolean result = DeviceHandler.getDevice().getUsersService().updatePermission(username, grantType);
        
        if(!result)
        {
            SmartServer.sendMessage(ctx, RequestCode.UPDATE_PERMISSION, FALSE);
            return;
        }        

        DaoGrantedAccess daoGa = (DaoGrantedAccess) DbManager.getDao(GrantedAccessEntity.class);
        if(!daoGa.persist(username, grantType))
        {
            SmartLogger.getLogger().severe(String.format("Permission set to %s for User %s, but not persisted!", 
                    newPermission, username));
            SmartServer.sendMessage(ctx, RequestCode.UPDATE_PERMISSION, FALSE);
            return;
        }

        SmartServer.sendMessage(ctx, RequestCode.UPDATE_PERMISSION, TRUE);
    }
}
