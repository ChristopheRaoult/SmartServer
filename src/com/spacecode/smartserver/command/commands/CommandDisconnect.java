package com.spacecode.smartserver.command.commands;

import com.spacecode.smartserver.SmartServer;
import com.spacecode.smartserver.command.ClientCommand;
import com.spacecode.smartserver.command.ClientCommandException;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelFutureListener;
import io.netty.channel.ChannelHandlerContext;

/**
 * Disconnect command.
 */
public class CommandDisconnect implements ClientCommand
{
    /**
     * Optional but better done: close the channel of the current context when the client asks for disconnection.
     * @param ctx                       ChannelHandlerContext instance corresponding to the channel existing between SmartServer and the client.
     * @param parameters                String array containing parameters (if any) provided by the client.
     * @throws ClientCommandException
     */
    @Override
    public void execute(ChannelHandlerContext ctx, String[] parameters) throws ClientCommandException
    {
        ChannelFuture f = SmartServer.sendMessage(ctx, "Bye");
        f.addListener(ChannelFutureListener.CLOSE);
    }
}