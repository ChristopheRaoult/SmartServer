package com.spacecode.smartserver.command;

import com.spacecode.sdk.network.communication.RequestCode;
import com.spacecode.sdk.user.User;
import com.spacecode.smartserver.SmartServer;
import com.spacecode.smartserver.helper.DeviceHandler;
import io.netty.channel.ChannelHandlerContext;

/**
 * UserByName command.
 */
public class CommandUserByName extends ClientCommand
{
    /**
     * Request to get a copy-instance of a granted user. Send the serialized GrantedUser instance.
     * @param ctx                       Channel between SmartServer and the client.
     * @param parameters                String array containing parameters (if any) provided by the client.
     * @throws ClientCommandException
     */
    @Override
    public void execute(ChannelHandlerContext ctx, String[] parameters) throws ClientCommandException
    {
        // waiting for 1 parameter: username.
        if(parameters.length != 1)
        {
            SmartServer.sendMessage(ctx, RequestCode.USER_BY_NAME, "");
            throw new ClientCommandException("Invalid number of parameters.");
        }

        if(DeviceHandler.getDevice() == null)
        {
            SmartServer.sendMessage(ctx, RequestCode.USER_BY_NAME, "");
            return;
        }

        String username = parameters[0];

        User user = DeviceHandler.getDevice().getUsersService().getUserByName(username);

        if(user == null)
        {
            SmartServer.sendMessage(ctx, RequestCode.USER_BY_NAME, "");
            return;
        }

        SmartServer.sendMessage(ctx, RequestCode.USER_BY_NAME, user.serialize());
    }
}
