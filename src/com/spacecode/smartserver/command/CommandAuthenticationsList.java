package com.spacecode.smartserver.command;

import com.spacecode.sdk.network.communication.RequestCode;
import com.spacecode.sdk.user.data.AccessType;
import com.spacecode.smartserver.SmartServer;
import com.spacecode.smartserver.database.DbManager;
import com.spacecode.smartserver.database.entity.AuthenticationEntity;
import com.spacecode.smartserver.database.repository.AccessTypeRepository;
import com.spacecode.smartserver.database.repository.AuthenticationRepository;
import com.spacecode.smartserver.helper.SmartLogger;
import io.netty.channel.ChannelHandlerContext;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.logging.Level;

/**
 * AuthenticationsList command.
 *
 * Provide authentications recorded during a certain period, if any.
 */
public class CommandAuthenticationsList extends ClientCommand
{
    /**
     * @param ctx           Channel between SmartServer and the client.
     * @param parameters    String array containing parameters (if any) provided by the client.
     *
     * @throws ClientCommandException
     */
    @Override
    public void execute(ChannelHandlerContext ctx, String[] parameters) throws ClientCommandException
    {
        // waiting for 2 parameters: start date, end date.
        if(parameters.length != 2)
        {
            SmartServer.sendMessage(ctx, RequestCode.AUTHENTICATIONS_LIST);
            throw new ClientCommandException("Invalid number of parameters.");
        }

        long timestampStart;
        long timestampEnd;

        try
        {
            timestampStart  = Long.parseLong(parameters[0]);
            timestampEnd    = Long.parseLong(parameters[1]);
        } catch(NumberFormatException nfe)
        {
            SmartLogger.getLogger().log(Level.WARNING,
                    "Invalid timestamp sent by client for Authentications.", nfe);
            SmartServer.sendMessage(ctx, RequestCode.AUTHENTICATIONS_LIST);
            return;
        }

        if(timestampEnd <= timestampStart)
        {
            SmartServer.sendMessage(ctx, RequestCode.AUTHENTICATIONS_LIST);
            return;
        }

        AuthenticationRepository repo =
                (AuthenticationRepository) DbManager.getRepository(AuthenticationEntity.class);

        List<AuthenticationEntity> authentications = repo.getAuthentications(new Date(timestampStart),
                new Date(timestampEnd),
                DbManager.getDeviceConfiguration());

        List<String> responsePackets = new ArrayList<>();
        responsePackets.add(RequestCode.AUTHENTICATIONS_LIST);

        for(AuthenticationEntity authentication : authentications)
        {
            AccessType accessType = AccessTypeRepository.asAccessType(authentication.getAccessType());
            String accessTypePacket =
                    accessType == AccessType.BADGE
                    ? "B"
                    : accessType == AccessType.FINGERPRINT
                    ? "F"
                    : "U";

            responsePackets.add(authentication.getGrantedUser().getUsername());
            responsePackets.add(String.valueOf(authentication.getCreatedAt().getTime() / 1000));
            responsePackets.add(accessTypePacket);
        }

        SmartServer.sendMessage(ctx, responsePackets.toArray(new String[0]));
    }
}
