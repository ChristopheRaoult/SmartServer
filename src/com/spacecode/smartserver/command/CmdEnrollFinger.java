package com.spacecode.smartserver.command;

import com.spacecode.sdk.network.communication.RequestCode;
import com.spacecode.sdk.user.User;
import com.spacecode.sdk.user.data.FingerIndex;
import com.spacecode.smartserver.SmartServer;
import com.spacecode.smartserver.database.DbManager;
import com.spacecode.smartserver.database.dao.DaoFingerprint;
import com.spacecode.smartserver.database.entity.FingerprintEntity;
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
     * Try to start an enrollment process for a given user and finger index. Sends back "true" if succeed, "false" otherwise.
     * 
     * @param ctx                       Channel between SmartServer and the client.
     * @param parameters                String array containing parameters (if any) provided by the client.
     *                                  
     * @throws ClientCommandException
     */
    @Override
    public synchronized void execute(final ChannelHandlerContext ctx, String[] parameters) throws ClientCommandException
    {
        // waiting for 3 parameters: username, finger index, "is Master reader?", OPTIONAL 4th: fingerprint template
        if(parameters.length < 3)
        {
            SmartServer.sendMessage(ctx, RequestCode.ENROLL_FINGER, FALSE);
            throw new ClientCommandException("Invalid number of parameters [EnrollFinger].");
        }

        final String username = parameters[0];
        final FingerIndex fingerIndex;
        final boolean masterReader = Boolean.parseBoolean(parameters[2]);
        final String template = parameters.length > 3 ? parameters[3] : null;

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
        
        // if no template has been provided: process a "normal" enrollment
        if(template == null || template.trim().isEmpty())
        {
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

                    SmartServer.sendMessage(ctx, RequestCode.ENROLL_FINGER,
                            persistTemplate(username, fingerIndex) ? TRUE : FALSE);
                }
            });
        }
        
        else
        {
            // a template has been given by the user: simply override the current fingerprint template and persist it
            if(!DeviceHandler.getDevice().getUsersService().enrollFinger(username, fingerIndex, template))
            {
                SmartServer.sendMessage(ctx, RequestCode.ENROLL_FINGER, FALSE);
                return;
            }

            SmartServer.sendMessage(ctx, RequestCode.ENROLL_FINGER,
                    persistTemplate(username, fingerIndex) ? TRUE : FALSE);
        }
    }

    /**
     * Persist the fingerprint template of the given user/finger index in the DB. If fails, remove the template from the
     * local user instance (in order not to let the UsersService use it for authentications).
     * 
     * @param username      Name of the user.
     * @param fingerIndex   Index of the finger to be updated.
     * 
     * @return True if the update succeeded, false otherwise.
     */
    private boolean persistTemplate(String username, FingerIndex fingerIndex)
    {
        // persist the enrolled template in db
        User gu = DeviceHandler.getDevice().getUsersService().getUserByName(username);
        String fpTpl = gu.getFingerprintTemplate(fingerIndex);

        DaoFingerprint daoFp = (DaoFingerprint) DbManager.getDao(FingerprintEntity.class);
        
        if (daoFp == null || !daoFp.persist(username, fingerIndex.getIndex(), fpTpl))
        {
            DeviceHandler.getDevice().getUsersService().removeFingerprint(username, fingerIndex);
            return false;
        }

        return true;
    }
}
