package com.spacecode.smartserver.command.commands;

import com.j256.ormlite.misc.TransactionManager;
import com.spacecode.sdk.network.communication.RequestCode;
import com.spacecode.sdk.user.FingerIndex;
import com.spacecode.sdk.user.GrantedUser;
import com.spacecode.smartserver.DeviceHandler;
import com.spacecode.smartserver.SmartLogger;
import com.spacecode.smartserver.SmartServer;
import com.spacecode.smartserver.command.ClientCommand;
import com.spacecode.smartserver.command.ClientCommandException;
import com.spacecode.smartserver.database.DatabaseHandler;
import com.spacecode.smartserver.database.entity.FingerprintEntity;
import com.spacecode.smartserver.database.entity.GrantTypeEntity;
import com.spacecode.smartserver.database.entity.GrantedAccessEntity;
import com.spacecode.smartserver.database.entity.GrantedUserEntity;
import com.spacecode.smartserver.database.repository.GrantTypeRepository;
import com.spacecode.smartserver.database.repository.Repository;
import io.netty.channel.ChannelHandlerContext;

import java.sql.SQLException;
import java.util.concurrent.Callable;
import java.util.logging.Level;

/**
 * AddUser command.
 */
public class CommandAddUser implements ClientCommand
{
    /**
     * Request to add a new User to granted users list. Send (string) "true" if succeed, "false" otherwise.
     * @param ctx                       ChannelHandlerContext instance corresponding to the channel existing between SmartServer and the client.
     * @param parameters                String array containing parameters (if any) provided by the client.
     * @throws ClientCommandException
     */
    @Override
    public void execute(ChannelHandlerContext ctx, String[] parameters) throws ClientCommandException
    {
        // waiting for only 1 parameter: serialized rantedUser
        if(parameters.length != 1)
        {
            SmartServer.sendMessage(ctx, RequestCode.ADD_USER, "false");
            throw new ClientCommandException("Invalid number of parameters.");
        }

        GrantedUser newUser = GrantedUser.deserialize(parameters[0]);

        if(newUser == null || newUser.getUsername() == null || "".equals(newUser.getUsername().trim()))
        {
            SmartServer.sendMessage(ctx, RequestCode.ADD_USER, "false");
            return;
        }

        if(!DeviceHandler.getDevice().getUsersService().addUser(newUser))
        {
            SmartServer.sendMessage(ctx, RequestCode.ADD_USER, "false");
            return;
        }

        if(!persistNewUser(newUser))
        {
            DeviceHandler.getDevice().getUsersService().removeUser(newUser.getUsername());
            SmartServer.sendMessage(ctx, RequestCode.ADD_USER, "false");
            return;
        }

        SmartServer.sendMessage(ctx, RequestCode.ADD_USER, "true");
    }

    /**
     * Process the data persistence:
     * GrantedUserEntity, Fingerprint(s), GrantedAccessEntity.
     *
     * @param newUser   Instance of GrantedUser (SDK) to be added to database.
     * @return          True if success, false otherwise (username already used, SQLException, etc).
     */
    private boolean persistNewUser(final GrantedUser newUser)
    {
        try
        {
            TransactionManager.callInTransaction(DatabaseHandler.getConnectionSource(), new Callable<Object>()
            {
                @Override
                public Object call() throws Exception
                {// First, get & create the user
                    Repository userRepo = DatabaseHandler.getRepository(GrantedUserEntity.class);

                    GrantedUserEntity gue = new GrantedUserEntity(newUser);

                    if(!userRepo.insert(gue))
                    {
                        throw new SQLException("Failed when inserting new user.");
                    }

                    // get GrantTypeEntity instance corresponding to newUser grant type
                    Repository grantTypeRepo = DatabaseHandler.getRepository(GrantTypeEntity.class);
                    GrantTypeEntity gte = ((GrantTypeRepository) grantTypeRepo)
                            .fromGrantType(newUser.getGrantType());

                    // create & persist fingerprints and access
                    Repository fpRepo = DatabaseHandler.getRepository(FingerprintEntity.class);
                    Repository gaRepo = DatabaseHandler.getRepository(GrantedAccessEntity.class);

                    GrantedAccessEntity gae = new GrantedAccessEntity(gue, DatabaseHandler.getDeviceConfiguration(), gte);

                    // add the fingerprints
                    for(FingerIndex index : newUser.getEnrolledFingersIndexes())
                    {
                        if(!fpRepo.insert(
                                new FingerprintEntity(gue, index.getIndex(),
                                        newUser.getFingerprintTemplate(index))
                        ))
                        {
                            throw new SQLException("Failed when inserting new fingerprint.");
                        }
                    }

                    // add the access to current device (if any)
                    if(!gaRepo.insert(gae))
                    {
                        throw new SQLException("Failed when inserting new granted access.");
                    }

                    return null;
                }
            });
        } catch (SQLException sqle)
        {
            SmartLogger.getLogger().log(Level.SEVERE, "Error while persisting new user.", sqle);
            return false;
        }

        return true;
    }
}
