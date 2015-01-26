package com.spacecode.smartserver.command;

import com.spacecode.sdk.network.communication.RequestCode;
import com.spacecode.sdk.user.User;
import com.spacecode.sdk.user.data.FingerIndex;
import com.spacecode.smartserver.SmartServer;
import com.spacecode.smartserver.database.DbManager;
import com.spacecode.smartserver.database.entity.FingerprintEntity;
import com.spacecode.smartserver.database.repository.FingerprintRepository;
import com.spacecode.smartserver.helper.DeviceHandler;
import com.spacecode.smartserver.helper.SmartLogger;
import io.netty.channel.ChannelHandlerContext;

import java.util.concurrent.TimeoutException;
import java.util.logging.Level;

/**
 * EnrollFinger command.
 */
public class CmdEnrollFinger extends ClientCommand
{
    /**
     * Request to start enrollment process for a given user and finger index. Send (string) "true" if succeed, "false" otherwise.
     * @param ctx                       Channel between SmartServer and the client.
     * @param parameters                String array containing parameters (if any) provided by the client.
     * @throws ClientCommandException
     */
    @Override
    public synchronized void execute(final ChannelHandlerContext ctx, String[] parameters) throws ClientCommandException
    {
        // waiting for 3 parameters: username, finger index, "is Master reader?" (boolean)
        if(parameters.length != 3)
        {
            SmartServer.sendMessage(ctx, RequestCode.ENROLL_FINGER, FALSE);
            throw new ClientCommandException("Invalid number of parameters.");
        }

        final String username = parameters[0];
        final FingerIndex fingerIndex;
        final boolean masterReader = Boolean.parseBoolean(parameters[2]);

        try
        {
            int fingerIndexVal = Integer.parseInt(parameters[1]);
            fingerIndex = FingerIndex.getValueByIndex(fingerIndexVal);
        } catch(NumberFormatException nfe)
        {
            // integer for the finger index is not valid
            SmartServer.sendMessage(ctx, RequestCode.ENROLL_FINGER, FALSE);
            return;
        }

        // no finger index matching with the given integer value
        if(fingerIndex == null)
        {
            SmartServer.sendMessage(ctx, RequestCode.ENROLL_FINGER, FALSE);
            return;
        }

        // Action needs to be parallelized in order to handle New Enrollment Sample event
        parallelize(new Runnable()
        {
            @Override
            public void run()
            {
                try
                {
                    if (!DeviceHandler.getDevice().getUsersService().enrollFinger(username, fingerIndex, masterReader))
                    {
                        SmartServer.sendMessage(ctx, RequestCode.ENROLL_FINGER, FALSE);
                        return;
                    }
                } catch (TimeoutException te)
                {
                    // enrollment process timeout expired
                    SmartLogger.getLogger().log(Level.WARNING, "Enrollment process timed out for User " + username, te);
                    SmartServer.sendMessage(ctx, RequestCode.ENROLL_FINGER, FALSE);
                    return;
                }

                // persist the enrolled template in db
                User gu = DeviceHandler.getDevice().getUsersService().getUserByName(username);
                String fpTpl = gu.getFingerprintTemplate(fingerIndex);

                if (!((FingerprintRepository) DbManager.getRepository(FingerprintEntity.class))
                        .persist(username, fingerIndex.getIndex(), fpTpl))
                {
                    DeviceHandler.getDevice().getUsersService().removeFingerprint(username, fingerIndex);
                    SmartServer.sendMessage(ctx, RequestCode.ENROLL_FINGER, FALSE);
                    return;
                }

                SmartServer.sendMessage(ctx, RequestCode.ENROLL_FINGER, TRUE);
            }
        });
    }
}
