package com.spacecode.smartserver.command;

import com.spacecode.sdk.network.communication.RequestCode;
import com.spacecode.sdk.user.User;
import com.spacecode.sdk.user.data.FingerIndex;
import com.spacecode.smartserver.SmartServer;
import com.spacecode.smartserver.database.DbManager;
import com.spacecode.smartserver.database.dao.DaoFingerprint;
import com.spacecode.smartserver.database.entity.FingerprintEntity;
import com.spacecode.smartserver.helper.DeviceHandler;
import io.netty.channel.ChannelHandlerContext;

/**
 * RemoveFingerprint command.
 */
public class CmdRemoveFingerprint extends ClientCommand
{
    /**
     * Request to add a new User to granted users list. Send (string) "true" if succeed, "false" otherwise.
     * @param ctx                       Channel between SmartServer and the client.
     * @param parameters                String array containing parameters (if any) provided by the client.
     * @throws ClientCommandException
     */
    @Override
    public void execute(ChannelHandlerContext ctx, String[] parameters) throws ClientCommandException
    {
        // waiting for 2 parameters: username and finger index
        if(parameters.length != 2)
        {
            SmartServer.sendMessage(ctx, RequestCode.REMOVE_FINGERPRINT, FALSE);
            throw new ClientCommandException("Invalid number of parameters [RemoveFingerprint].");
        }

        if(DeviceHandler.getDevice() == null)
        {
            SmartServer.sendMessage(ctx, RequestCode.REMOVE_FINGERPRINT, FALSE);
            return;
        }

        String username = parameters[0];
        FingerIndex fingerIndex;

        try
        {
            int fingerIndexVal = Integer.parseInt(parameters[1]);
            fingerIndex = FingerIndex.getValueByIndex(fingerIndexVal);
        } catch(NumberFormatException nfe)
        {
            // integer for the finger index is not valid
            SmartServer.sendMessage(ctx, RequestCode.REMOVE_FINGERPRINT, FALSE);
            return;
        }

        // no finger index matching with the given integer value
        if(fingerIndex == null)
        {
            SmartServer.sendMessage(ctx, RequestCode.REMOVE_FINGERPRINT, FALSE);
            return;
        }

        User user = DeviceHandler.getDevice().getUsersService().getUserByName(username);

        if(user == null)
        {
            // user could not be found
            SmartServer.sendMessage(ctx, RequestCode.REMOVE_FINGERPRINT, FALSE);
            return;
        }

        // persist deletion in database
        if(!((DaoFingerprint) DbManager.getDao(FingerprintEntity.class)).delete(username, fingerIndex.getIndex()))
        {
            SmartServer.sendMessage(ctx, RequestCode.REMOVE_FINGERPRINT, FALSE);
            return;
        }

        DeviceHandler.getDevice().getUsersService().removeFingerprint(username, fingerIndex);
        SmartServer.sendMessage(ctx, RequestCode.REMOVE_FINGERPRINT, TRUE);
    }
}
