package com.spacecode.smartserver.command;

import com.spacecode.sdk.network.communication.RequestCode;
import com.spacecode.sdk.user.data.AccessType;
import com.spacecode.smartserver.SmartServer;
import com.spacecode.smartserver.database.DbManager;
import com.spacecode.smartserver.database.dao.DaoAccessType;
import com.spacecode.smartserver.database.dao.DaoAuthentication;
import com.spacecode.smartserver.database.entity.AuthenticationEntity;
import com.spacecode.smartserver.database.entity.UserEntity;
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
@CommandContract(paramCount = 2, strictCount = true, responseWhenInvalid = "")
public class CmdAuthenticationsList extends ClientCommand
{
    /**
     * @param ctx           Channel between SmartServer and the client.
     * @param parameters    "Start" and "End" dates (period).
     */
    @Override
    public void execute(ChannelHandlerContext ctx, String[] parameters)
    {
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

        DaoAuthentication daoAuthentication =
                (DaoAuthentication) DbManager.getDao(AuthenticationEntity.class);
        List<AuthenticationEntity> authentications =
                daoAuthentication.getAuthentications(new Date(timestampStart), new Date(timestampEnd));

        List<String> responsePackets = new ArrayList<>();
        responsePackets.add(RequestCode.AUTHENTICATIONS_LIST);

        for(AuthenticationEntity authentication : authentications)
        {
            AccessType accessType = DaoAccessType.asAccessType(authentication.getAccessType());
            String accessTypePacket =
                    accessType == AccessType.BADGE
                    ? "B"
                    : accessType == AccessType.FINGERPRINT
                    ? "F"
                    : "U";

            UserEntity authenticatedUser = authentication.getUser();
            responsePackets.add(authenticatedUser != null ? authenticatedUser.getUsername() : "Unknown User");
            responsePackets.add(String.valueOf(authentication.getCreatedAt().getTime() / 1000));
            responsePackets.add(accessTypePacket);
        }

        SmartServer.sendMessage(ctx, responsePackets.toArray(new String[responsePackets.size()]));
    }
}
