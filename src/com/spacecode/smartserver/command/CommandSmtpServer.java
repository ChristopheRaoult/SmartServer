package com.spacecode.smartserver.command;

import com.spacecode.sdk.network.communication.RequestCode;
import com.spacecode.smartserver.SmartServer;
import com.spacecode.smartserver.database.DatabaseHandler;
import com.spacecode.smartserver.database.entity.SmtpServerEntity;
import io.netty.channel.ChannelHandlerContext;

/**
 * SmtpServer command.
 */
public class CommandSmtpServer extends ClientCommand
{
    /**
     * Send SMTP server information (if any) for current device.
     *
     * @param ctx                       ChannelHandlerContext instance corresponding to the channel existing between SmartServer and the client.
     * @param parameters                String array containing parameters (if any) provided by the client.
     *
     * @throws ClientCommandException
     */
    @Override
    public void execute(ChannelHandlerContext ctx, String[] parameters) throws ClientCommandException
    {
        SmtpServerEntity sse = DatabaseHandler.getSmtpServerConfiguration();

        if(sse == null)
        {
            SmartServer.sendMessage(ctx, RequestCode.SMTP_SERVER);
        }

        else
        {
            SmartServer.sendMessage(ctx, RequestCode.SMTP_SERVER, sse.getAddress(), String.valueOf(sse.getPort()),
                    sse.getUsername(), sse.getPassword(), String.valueOf(sse.isSslEnabled()));
        }
    }
}
