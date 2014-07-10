package com.spacecode.smartserver;

import com.spacecode.sdk.network.communication.MessageHandler;
import com.spacecode.smartserver.command.ClientCommandException;
import com.spacecode.smartserver.command.CommandRegister;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;

import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Created by Vincent on 23/12/13.
 */

/**
 * Handle new connections and messages from clients.
 * Handle any exception caught by Netty.
 */
public final class SmartServerHandler extends SimpleChannelInboundHandler<String>
{
    private static final Logger LOGGER = Logger.getLogger(SmartServerHandler.class.getName());
    private static final CommandRegister COMMAND_REGISTER = new CommandRegister();

    /**
     * Called when a new connection (with a client) is created.
     * @param ctx ChannelHandlerContext instance corresponding to the channel created between SmartServer and the new Client.
     */
    @Override
    public void channelActive(final ChannelHandlerContext ctx)
    {
        SmartServer.addChannel(ctx.channel());
        ConsoleLogger.info("Connection from "+ ctx.channel().remoteAddress());
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

        ConsoleLogger.info(request);

        String[] parameters = request.split(Character.toString(MessageHandler.DELIMITER));

        try
        {
            COMMAND_REGISTER.execute(ctx, parameters);
        } catch (ClientCommandException cce)
        {
            LOGGER.log(Level.WARNING, cce.getMessage(), cce);
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
        LOGGER.log(Level.WARNING, "Exception caught by handler.", cause);
        ctx.close();
    }
}