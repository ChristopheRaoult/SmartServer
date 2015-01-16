package com.spacecode.smartserver.command;

import com.spacecode.sdk.network.communication.RequestCode;
import com.spacecode.smartserver.SmartServer;
import com.spacecode.smartserver.database.DbManager;
import com.spacecode.smartserver.database.entity.SmtpServerEntity;
import com.spacecode.smartserver.database.repository.SmtpServerRepository;
import io.netty.channel.ChannelHandlerContext;

/**
 * SmtpServer command.
 */
public class CommandSmtpServer extends ClientCommand
{
    /**
     * Send SMTP server information (if any) for current device.
     *
     * @param ctx                       Channel between SmartServer and the client.
     * @param parameters                String array containing parameters (if any) provided by the client.
     *
     * @throws ClientCommandException
     */
    @Override
    public void execute(ChannelHandlerContext ctx, String[] parameters) throws ClientCommandException
    {
        SmtpServerEntity sse =
                ((SmtpServerRepository) DbManager.getRepository(SmtpServerEntity.class)).getSmtpServerConfig();

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
