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

        if(DeviceHandler.getDevice() == null)
        {
            SmartServer.sendMessage(ctx, RequestCode.ENROLL_FINGER, FALSE);
            return;
        }

        final String username = parameters[0];
        final FingerIndex fingerIndex;
        final boolean masterReader = Boolean.parseBoolean(parameters[2]);
        final String template = parameters.length > 3 ? parameters[3] : null;

        final User gu = DeviceHandler.getDevice().getUsersService().getUserByName(username);
        
        if(gu == null)
        {
            // unknown user
            SmartServer.sendMessage(ctx, RequestCode.ENROLL_FINGER, FALSE);
            return;
        }
        
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
        
        final String oldTemplate = gu.getFingerprintTemplate(fingerIndex);
        
        // if no template has been provided: process a "normal" enrollment
        if(template == null || template.trim().isEmpty())
        {
            // Action needs to be parallelized in order to handle New Enrollment Sample event
            parallelize(new Runnable()
            {
                @Override
                public void run()
                {
                    boolean result;
                            
                    try
                    {
                         result = enrollAndPersist(gu, fingerIndex, masterReader, oldTemplate);
                    } catch (TimeoutException te)
                    {
                        SmartLogger.getLogger().log(Level.WARNING, 
                                "Enrollment process timed out for User " + username, te);
                        result = false;
                    }

                    SmartServer.sendMessage(ctx, RequestCode.ENROLL_FINGER, result ? TRUE : FALSE);
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
                    persistTemplate(gu, fingerIndex, oldTemplate) ? TRUE : FALSE);
        }
    }

    /**
     * Start the enrollment process, and if successful, try to persist the new fingerprint template in the DB.
     *
     * @param user          User owning the templates.
     * @param fingerIndex   Index of the enrolled finger.
     * @param masterReader  If true, use the master fingerprint reader. Otherwise, the slave.
     * @param oldTemplate   Template to be restored if the operation fails.
     * 
     * @return True if the enrollment AND the persistence succeeded. False otherwise.
     * 
     * @throws TimeoutException If the enrollment max. delay is up.
     */
    boolean enrollAndPersist(User user, FingerIndex fingerIndex, boolean masterReader, String oldTemplate) 
            throws TimeoutException
    {
        return DeviceHandler.getDevice().getUsersService().enrollFinger(user.getUsername(), fingerIndex, masterReader) 
                && persistTemplate(user, fingerIndex, oldTemplate);
    }

    /**
     * Persist the fingerprint template of the given user/finger index in the DB. If fails, remove the template from the
     * local user instance (in order not to let the UsersService use it for authentications).
     * 
     * @param user          User owning the templates.
     * @param fingerIndex   Index of the finger to be updated.
     * @param oldTemplate   Template to be restored if the operation fails.
     *
     * @return True if the update succeeded, false otherwise.
     */
    boolean persistTemplate(User user, FingerIndex fingerIndex, String oldTemplate)
    {
        String fpTpl = user.getFingerprintTemplate(fingerIndex);
        DaoFingerprint daoFp = (DaoFingerprint) DbManager.getDao(FingerprintEntity.class);
        
        if (daoFp == null || !daoFp.persist(user.getUsername(), fingerIndex.getIndex(), fpTpl))
        {
            DeviceHandler.getDevice().getUsersService().enrollFinger(user.getUsername(), fingerIndex, oldTemplate);
            return false;
        }

        return true;
    }
}
