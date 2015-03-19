package com.spacecode.smartserver.command;

import com.spacecode.smartserver.SmartServer;
import io.netty.channel.ChannelHandlerContext;

/**
 * Command SignInAdmin
 */
public class CmdSignInAdmin extends ClientCommand
{
    /**
     * Add the current ChannelHandlerContext to the list of authenticated (administrator) contexts.
     *
     * @param ctx                       Channel between SmartServer and the client.
     * @param parameters                String array containing parameters (if any) provided by the client.
     *
     * @throws ClientCommandException   Invalid number of parameters received.
     */
    @Override
    public void execute(ChannelHandlerContext ctx, String[] parameters) throws ClientCommandException
    {
        SmartServer.addAdministrator(ctx.channel().remoteAddress());
    }
}
