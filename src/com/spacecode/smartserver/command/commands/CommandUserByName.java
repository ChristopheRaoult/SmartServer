package com.spacecode.smartserver.command.commands;

import com.spacecode.sdk.network.communication.RequestCode;
import com.spacecode.sdk.user.GrantedUser;
import com.spacecode.smartserver.DeviceHandler;
import com.spacecode.smartserver.SmartServer;
import com.spacecode.smartserver.command.ClientCommand;
import com.spacecode.smartserver.command.ClientCommandException;
import io.netty.channel.ChannelHandlerContext;

/**
 * AddUser command.
 */
public class CommandUserByName implements ClientCommand
{
    /**
     * Request to get a copy-instance of a granted user. Send the serialized GrantedUser instance.
     * @param ctx                       ChannelHandlerContext instance corresponding to the channel existing between SmartServer and the client.
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

        String username = parameters[0];

        GrantedUser user = DeviceHandler.getDevice().getUsersService().getUserByName(username);

        if(user == null)
        {
            SmartServer.sendMessage(ctx, RequestCode.USER_BY_NAME, "");
            return;
        }

        SmartServer.sendMessage(ctx, RequestCode.USER_BY_NAME, user.serialize());
    }
}
