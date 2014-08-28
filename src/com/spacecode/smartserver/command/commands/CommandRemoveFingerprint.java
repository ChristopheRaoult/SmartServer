package com.spacecode.smartserver.command.commands;

import com.spacecode.sdk.network.communication.RequestCode;
import com.spacecode.sdk.user.FingerIndex;
import com.spacecode.sdk.user.GrantedUser;
import com.spacecode.smartserver.DeviceHandler;
import com.spacecode.smartserver.SmartServer;
import com.spacecode.smartserver.command.ClientCommand;
import com.spacecode.smartserver.command.ClientCommandException;
import io.netty.channel.ChannelHandlerContext;

/**
 * RemoveFingerprint command.
 */
public class CommandRemoveFingerprint implements ClientCommand
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
        // waiting for 2 parameters: username and finger index
        if(parameters.length != 2)
        {
            SmartServer.sendMessage(ctx, RequestCode.REMOVE_FINGERPRINT, "false");
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
            SmartServer.sendMessage(ctx, RequestCode.REMOVE_FINGERPRINT, "false");
            return;
        }

        // no finger index matching with the given integer value
        if(fingerIndex == null)
        {
            SmartServer.sendMessage(ctx, RequestCode.REMOVE_FINGERPRINT, "false");
            return;
        }

        GrantedUser user = DeviceHandler.getDevice().getUsersService().getUserByName(username);

        if(user == null)
        {
            SmartServer.sendMessage(ctx, RequestCode.REMOVE_FINGERPRINT, "false");
            return;
        }

        user.setFingerprintTemplate(fingerIndex, null);
        SmartServer.sendMessage(ctx, RequestCode.REMOVE_FINGERPRINT, "true");

        // TODO: Persist fingerprint removal in DB if required
    }
}
