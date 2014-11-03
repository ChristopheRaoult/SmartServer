package com.spacecode.smartserver.command;

import io.netty.channel.ChannelHandlerContext;

/**
 * Disconnect command.
 */
public class CommandDisconnect extends ClientCommand
{
    /**
     * Optional but better done: close the channel of the current context when the client asks for disconnection.
     *
     * @param ctx           ChannelHandlerContext instance corresponding to the channel existing between SmartServer and the client.
     * @param parameters    String array containing parameters (if any) provided by the client.
     *
     * @throws ClientCommandException
     */
    @Override
    public void execute(ChannelHandlerContext ctx, String[] parameters) throws ClientCommandException
    {
        ctx.channel().close();
    }
}