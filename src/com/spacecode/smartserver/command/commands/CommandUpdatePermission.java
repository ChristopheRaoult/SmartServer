package com.spacecode.smartserver.command.commands;

import com.spacecode.sdk.network.communication.RequestCode;
import com.spacecode.sdk.user.GrantType;
import com.spacecode.sdk.user.GrantedUser;
import com.spacecode.smartserver.DeviceHandler;
import com.spacecode.smartserver.SmartLogger;
import com.spacecode.smartserver.SmartServer;
import com.spacecode.smartserver.command.ClientCommand;
import com.spacecode.smartserver.command.ClientCommandException;
import com.spacecode.smartserver.database.DatabaseHandler;
import com.spacecode.smartserver.database.entity.GrantTypeEntity;
import com.spacecode.smartserver.database.entity.GrantedAccessEntity;
import com.spacecode.smartserver.database.entity.GrantedUserEntity;
import com.spacecode.smartserver.database.repository.GrantTypeRepository;
import com.spacecode.smartserver.database.repository.GrantedAccessRepository;
import com.spacecode.smartserver.database.repository.GrantedUserRepository;
import com.spacecode.smartserver.database.repository.Repository;
import io.netty.channel.ChannelHandlerContext;

import java.util.Iterator;
import java.util.logging.Level;

/**
 * Update Grant Type command.
 */
public class CommandUpdatePermission implements ClientCommand
{
    /**
     * Request to update an user's permission type to this device. Return true (if operation succeeded) or false (if failure).
     * @param ctx                       ChannelHandlerContext instance corresponding to the channel existing between SmartServer and the client.
     * @param parameters                String array containing parameters (if any) provided by the client.
     * @throws ClientCommandException   If number of parameters is invalid.
     */
    @Override
    public void execute(ChannelHandlerContext ctx, String[] parameters) throws ClientCommandException
    {
        // waiting for 2 parameters: username and new Grant Type (permission on device).
        if(parameters.length != 2)
        {
            SmartServer.sendMessage(ctx, RequestCode.UPDATE_PERMISSION, "false");
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
            SmartServer.sendMessage(ctx, RequestCode.UPDATE_PERMISSION, "false");
            return;
        }

        GrantedUser user = DeviceHandler.getDevice().getUsersService().getUserByName(username);

        // grantType can't be null with "valueOf" (enum) but anyway
        if(user == null || grantType == null)
        {
            SmartServer.sendMessage(ctx, RequestCode.UPDATE_PERMISSION, "false");
            return;
        }

        if(!persistNewPermission(username, grantType))
        {
            SmartServer.sendMessage(ctx, RequestCode.UPDATE_PERMISSION, "false");
            return;
        }

        user.setPermission(grantType);
        SmartServer.sendMessage(ctx, RequestCode.UPDATE_PERMISSION, "true");
    }

    /**
     * Persist new permission in database.
     * @param username      User to be updated.
     * @param grantType     New permission.
     * @return              True if success, false otherwise (user not known, SQLException, etc).
     */
    private boolean persistNewPermission(String username, GrantType grantType)
    {
        Repository userRepo = DatabaseHandler.getRepository(GrantedUserEntity.class);
        Repository accessRepo = DatabaseHandler.getRepository(GrantedAccessEntity.class);
        Repository grantTypeRepo = DatabaseHandler.getRepository(GrantTypeEntity.class);

        if( !(userRepo instanceof GrantedUserRepository) ||
            !(grantTypeRepo instanceof GrantTypeRepository) ||
            !(accessRepo instanceof GrantedAccessRepository))
        {
            // not supposed to happen as the repositories map is filled automatically
            return false;
        }

        GrantedUserEntity gue = ((GrantedUserRepository)userRepo).getByUsername(username);

        if(gue == null)
        {
            return false;
        }

        GrantedAccessEntity gae = new GrantedAccessEntity(gue,
                DatabaseHandler.getDeviceConfiguration(),
                ((GrantTypeRepository) grantTypeRepo).fromGrantType(grantType)
                );

        Iterator<GrantedAccessEntity> it = gue.getGrantedAccesses().iterator();

        while(it.hasNext())
        {
            if(it.next().getDevice().getId() == DatabaseHandler.getDeviceConfiguration().getId())
            {
                it.remove();
                break;
            }
        }

        return gue.getGrantedAccesses().add(gae);
    }
}
