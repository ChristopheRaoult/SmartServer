package com.spacecode.smartserver.command;

import com.spacecode.sdk.network.communication.RequestCode;
import com.spacecode.sdk.user.GrantType;
import com.spacecode.sdk.user.GrantedUser;
import com.spacecode.smartserver.SmartServer;
import com.spacecode.smartserver.database.DatabaseHandler;
import com.spacecode.smartserver.helper.DeviceHandler;
import com.spacecode.smartserver.helper.SmartLogger;
import io.netty.channel.ChannelHandlerContext;

import java.util.logging.Level;

/**
 * UpdatePermission command.
 */
public class CommandUpdatePermission extends ClientCommand
{
    /**
     * Request to update an user's permission type to this device. Return true (if operation succeeded) or false (if failure).
     * @param ctx                       ChannelHandlerContext instance corresponding to the channel existing between SmartServer and the client.
     * @param parameters                String array containing parameters (if any) provided by the client.
     * @throws ClientCommandException   If number of parameters is invalid.
     */
    @Override
    public synchronized void execute(ChannelHandlerContext ctx, String[] parameters) throws ClientCommandException
    {
        // waiting for 2 parameters: username and new Grant Type (permission on device).
        if(parameters.length != 2)
        {
            SmartServer.sendMessage(ctx, RequestCode.UPDATE_PERMISSION, FALSE);
            throw new ClientCommandException("Invalid number of parameters.");
        }

        String username = parameters[0];
        String newPermission = parameters[1];
        GrantType grantType;

        try
        {
            grantType = GrantType.valueOf(newPermission);
        } catch(IllegalArgumentException iae)
        {
            SmartLogger.getLogger().log(Level.SEVERE, "Invalid GrantType for permission update", iae);
            SmartServer.sendMessage(ctx, RequestCode.UPDATE_PERMISSION, FALSE);
            return;
        }

        GrantedUser user = DeviceHandler.getDevice().getUsersService().getUserByName(username);

        // grantType can't be null with "valueOf" (enum) but anyway
        if(user == null || grantType == null)
        {
            SmartServer.sendMessage(ctx, RequestCode.UPDATE_PERMISSION, FALSE);
            return;
        }

        if(!DatabaseHandler.persistPermission(username, grantType))
        {
            SmartServer.sendMessage(ctx, RequestCode.UPDATE_PERMISSION, FALSE);
            return;
        }

        user.setPermission(grantType);
        SmartServer.sendMessage(ctx, RequestCode.UPDATE_PERMISSION, TRUE);
    }
}
