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
@CommandContract(paramCount = 2, strictCount = true, deviceRequired = true)
public class CmdRemoveFingerprint extends ClientCommand
{
    /**
     * Request to add a new User to granted users list. Send (string) "true" if succeed, "false" otherwise.
     * 
     * @param ctx           Channel between SmartServer and the client.
     * @param parameters    Username, Finger Index.
     */
    @Override
    public void execute(ChannelHandlerContext ctx, String[] parameters)
    {
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
        DaoFingerprint daoFp = (DaoFingerprint) DbManager.getDao(FingerprintEntity.class);
        if(!daoFp.delete(username, fingerIndex.getIndex()))
        {
            SmartServer.sendMessage(ctx, RequestCode.REMOVE_FINGERPRINT, FALSE);
            return;
        }

        DeviceHandler.getDevice().getUsersService().removeFingerprint(username, fingerIndex);
        SmartServer.sendMessage(ctx, RequestCode.REMOVE_FINGERPRINT, TRUE);
    }
}
