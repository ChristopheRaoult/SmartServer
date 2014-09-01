package com.spacecode.smartserver;

import com.spacecode.sdk.network.communication.MessageHandler;
import com.spacecode.smartserver.command.ClientCommandException;
import com.spacecode.smartserver.command.CommandRegister;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;

import java.util.logging.Level;

/**
 * Handle new connections and messages from clients.
 * Handle any exception caught by Netty.
 */
public final class SmartServerHandler extends SimpleChannelInboundHandler<String>
{
    private static final CommandRegister COMMAND_REGISTER = new CommandRegister();

    /**
     * Called when a new connection (with a client) is created.
     * @param ctx ChannelHandlerContext instance corresponding to the channel created between SmartServer and the new Client.
     */
    @Override
    public void channelActive(final ChannelHandlerContext ctx)
    {
        SmartServer.addChannel(ctx.channel());
        SmartLogger.getLogger().info("Connection from " + ctx.channel().remoteAddress());
    }

    /**
     * Called when a message is received from a client.
     * @param ctx       ChannelHandlerContext instance corresponding to the channel existing between SmartServer and the new Client.
     * @param request   Client message (which is a "Request": a command, with potential parameters).
     */
    @Override
    protected void channelRead0(ChannelHandlerContext ctx, String request)
    {
        if(request == null || request.trim().isEmpty())
        {
            return;
        }

        SmartLogger.getLogger().info(request);

        String[] parameters = request.split(Character.toString(MessageHandler.DELIMITER));

        try
        {
            COMMAND_REGISTER.execute(ctx, parameters);
        } catch (ClientCommandException cce)
        {
            SmartLogger.getLogger().log(Level.SEVERE, "ClientCommand exception occurred.", cce);
        }
    }

    /**
     * Called when an exception has been caught by Netty communication abstraction layer.
     * @param ctx   ChannelHandlerContext instance corresponding to the channel existing between SmartServer and the new Client.
     * @param cause Throwable instance of the raised Exception.
     */
    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause)
    {
        SmartLogger.getLogger().log(Level.WARNING, "Exception caught by handler.", cause);
        ctx.close();
    }
}