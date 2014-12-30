package com.spacecode.smartserver.command;

import com.spacecode.sdk.network.communication.RequestCode;
import com.spacecode.sdk.user.User;
import com.spacecode.smartserver.SmartServer;
import com.spacecode.smartserver.database.DatabaseHandler;
import com.spacecode.smartserver.helper.DeviceHandler;
import io.netty.channel.ChannelHandlerContext;

/**
 * SetThiefFinger command.
 */
public class CommandSetThiefFinger extends ClientCommand
{
    /**
     * Request to update an user's "thief finger" index. Return true (if operation succeeded) or false (if failure).
     * @param ctx                       Channel between SmartServer and the client.
     * @param parameters                String array containing parameters (if any) provided by the client.
     * @throws ClientCommandException   Invalid number of parameters received.
     */
    @Override
    public synchronized void execute(ChannelHandlerContext ctx, String[] parameters) throws ClientCommandException
    {
        // waiting for 2 parameters: username and new badge number.
        if(parameters.length != 2)
        {
            SmartServer.sendMessage(ctx, RequestCode.SET_THIEF_FINGER, FALSE);
            throw new ClientCommandException("Invalid number of parameters.");
        }

        if(DeviceHandler.getDevice() == null)
        {
            SmartServer.sendMessage(ctx, RequestCode.SET_THIEF_FINGER, FALSE);
            return;
        }

        String username = parameters[0];
        Integer fingerIndex;

        try
        {
            fingerIndex = Integer.parseInt(parameters[1]);
        } catch(NumberFormatException nfe)
        {
            fingerIndex = null;
        }

        User user = DeviceHandler.getDevice().getUsersService().getUserByName(username);

        if(user == null)
        {
            SmartServer.sendMessage(ctx, RequestCode.SET_THIEF_FINGER, FALSE);
            return;
        }

        if(!DatabaseHandler.persistThiefFingerIndex(username, fingerIndex))
        {
            SmartServer.sendMessage(ctx, RequestCode.SET_THIEF_FINGER, FALSE);
            return;
        }

        SmartServer.sendMessage(ctx, RequestCode.SET_THIEF_FINGER, TRUE);
    }
}
