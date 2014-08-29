package com.spacecode.smartserver.command.commands;

import com.spacecode.sdk.network.communication.RequestCode;
import com.spacecode.sdk.user.FingerIndex;
import com.spacecode.smartserver.DeviceHandler;
import com.spacecode.smartserver.SmartServer;
import com.spacecode.smartserver.command.ClientCommand;
import com.spacecode.smartserver.command.ClientCommandException;
import com.spacecode.smartserver.database.DatabaseHandler;
import com.spacecode.smartserver.database.entity.FingerprintEntity;
import com.spacecode.smartserver.database.entity.GrantedUserEntity;
import com.spacecode.smartserver.database.repository.FingerprintRepository;
import com.spacecode.smartserver.database.repository.GrantedUserRepository;
import com.spacecode.smartserver.database.repository.Repository;
import io.netty.channel.ChannelHandlerContext;

import java.util.concurrent.TimeoutException;

/**
 * EnrollFinger command.
 */
public class CommandEnrollFinger implements ClientCommand
{
    /**
     * Request to start enrollment process for a given user and finger index. Send (string) "true" if succeed, "false" otherwise.
     * @param ctx                       ChannelHandlerContext instance corresponding to the channel existing between SmartServer and the client.
     * @param parameters                String array containing parameters (if any) provided by the client.
     * @throws ClientCommandException
     */
    @Override
    public void execute(ChannelHandlerContext ctx, String[] parameters) throws ClientCommandException
    {
        // waiting for 3 parameters: username, finger index, "is Master reader?" (boolean)
        if(parameters.length != 3)
        {
            SmartServer.sendMessage(ctx, RequestCode.ENROLL_FINGER, "false");
            return;
        }

        String username = parameters[0];
        FingerIndex fingerIndex;
        boolean masterReader = Boolean.parseBoolean(parameters[2]);

        try
        {
            int fingerIndexVal = Integer.parseInt(parameters[1]);
            fingerIndex = FingerIndex.getValueByIndex(fingerIndexVal);
        } catch(NumberFormatException nfe)
        {
            // integer for the finger index is not valid
            SmartServer.sendMessage(ctx, RequestCode.ENROLL_FINGER, "false");
            return;
        }

        // no finger index matching with the given integer value
        if(fingerIndex == null)
        {
            SmartServer.sendMessage(ctx, RequestCode.ENROLL_FINGER, "false");
            return;
        }

        try
        {
            if(!DeviceHandler.getDevice().getUsersService().enrollFinger(username, fingerIndex, masterReader))
            {
                SmartServer.sendMessage(ctx, RequestCode.ENROLL_FINGER, "false");
                return;
            }
        } catch (TimeoutException e)
        {
            // enrollment process timeout expired
            SmartServer.sendMessage(ctx, RequestCode.ENROLL_FINGER, "false");
            return;
        }

        String fpTpl = DeviceHandler.getDevice().getUsersService().getUserByName(username).getFingerprintTemplate(fingerIndex);

        if(!persistNewFingerprintInDatabase(username, fingerIndex.getIndex(), fpTpl))
        {
            SmartServer.sendMessage(ctx, RequestCode.ENROLL_FINGER, "false");
            return;
        }

        SmartServer.sendMessage(ctx, RequestCode.ENROLL_FINGER, "true");
    }

    /**
     * Get FingerprintRepository and start data persistence process.
     * @param username      User to be attached to the fingerprint entity.
     * @param fingerIndex   Finger index (int) to be written in new row.
     * @param fpTpl         Base64 encoded fingerprint template.
     * @return              True if success, false otherwise (user unknown in DB, SQLException...).
     */
    private boolean persistNewFingerprintInDatabase(String username, int fingerIndex, String fpTpl)
    {
        Repository userRepo = DatabaseHandler.getRepository(GrantedUserEntity.class);
        Repository fpRepo   = DatabaseHandler.getRepository(FingerprintEntity.class);

        if(!(userRepo instanceof GrantedUserRepository))
        {
            return false;
        }

        GrantedUserEntity gue = ((GrantedUserRepository) userRepo).getEntityBy(GrantedUserEntity.USERNAME, username);

        if(gue == null)
        {
            return false;
        }

        if(!(fpRepo instanceof FingerprintRepository))
        {
            return false;
        }

        return fpRepo.insert(new FingerprintEntity(gue, fingerIndex, fpTpl));
    }
}
