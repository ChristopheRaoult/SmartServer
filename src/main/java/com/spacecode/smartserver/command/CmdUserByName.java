package com.spacecode.smartserver.command;

import com.spacecode.sdk.network.communication.RequestCode;
import com.spacecode.sdk.user.User;
import com.spacecode.smartserver.SmartServer;
import com.spacecode.smartserver.helper.DeviceHandler;
import io.netty.channel.ChannelHandlerContext;

/**
 * UserByName command.
 */
@CommandContract(paramCount = 1, strictCount = true, deviceRequired = true, responseIfInvalid = "")
public class CmdUserByName extends ClientCommand
{
    /**
     * Request to get a copy-instance of a granted user. Send the serialized GrantedUser instance.
     * 
     * @param ctx           Channel between SmartServer and the client.
     * @param parameters    Username.
     */
    @Override
    public void execute(ChannelHandlerContext ctx, String[] parameters)
    {
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
