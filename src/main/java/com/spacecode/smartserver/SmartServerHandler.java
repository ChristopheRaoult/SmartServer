package com.spacecode.smartserver;

import com.spacecode.sdk.network.communication.MessageHandler;
import com.spacecode.smartserver.command.ClientCommandException;
import com.spacecode.smartserver.command.ClientCommandRegister;
import com.spacecode.smartserver.helper.SmartLogger;
import io.netty.channel.ChannelHandler;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;

import java.util.logging.Level;

/**
 * Default ChannelHandler, used to perform asynchronous communication through regular sockets (TCP/IP).
 */
@ChannelHandler.Sharable
final class SmartServerHandler extends SimpleChannelInboundHandler<String>
{
    private static final ClientCommandRegister COMMAND_REGISTER = new ClientCommandRegister();

    /**
     * Called when a new connection (with a client) is created.
     *
     * @param ctx ChannelHandlerContext instance corresponding to the channel created between SmartServer and the new Client.
     */
    @Override
    public void channelActive(final ChannelHandlerContext ctx)
    {
        SmartServer.addClientChannel(ctx.channel(), ctx.handler());
        SmartLogger.getLogger().info("Connection from " + ctx.channel().remoteAddress());
    }

    /**
     * Called when a message is received from a client.
     *
     * @param ctx   ChannelHandlerContext instance corresponding to the channel existing between
     *              SmartServer and the new Client.
     * @param msg   Client message (which is a "Request": a command, with potential parameters).
     */
    @Override
    protected void channelRead0(ChannelHandlerContext ctx, String msg)
    {
        handleTextRequest(ctx, msg);
    }

    private void handleTextRequest(ChannelHandlerContext ctx, String request)
    {
        if(request.trim().isEmpty())
        {
            return;
        }

        String[] parameters = request.split(Character.toString(MessageHandler.DELIMITER));

        SmartLogger.getLogger().info(ctx.channel().remoteAddress().toString()+" - "+parameters[0]);

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
     *
     * @param ctx   ChannelHandlerContext instance corresponding to the channel existing between
     *              SmartServer and the new Client.
     * @param cause Throwable instance of the raised Exception.
     */
    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause)
    {
        SmartLogger.getLogger().log(Level.WARNING, "Exception caught by SmartServerhandler", cause);
        ctx.close();
    }
}