package com.spacecode.smartserver.command;

import com.spacecode.sdk.network.alert.SmtpServer;
import com.spacecode.sdk.network.communication.RequestCode;
import com.spacecode.smartserver.SmartServer;
import com.spacecode.smartserver.database.DbManager;
import com.spacecode.smartserver.database.dao.DaoSmtpServer;
import com.spacecode.smartserver.database.entity.SmtpServerEntity;
import com.spacecode.smartserver.helper.SmartLogger;
import io.netty.channel.ChannelHandlerContext;

import java.util.logging.Level;

/**
 * SetSmtpServer command.
 */
@CommandContract(paramCount = 5, strictCount = true)
public class CmdSetSmtpServer extends ClientCommand
{
    /**
     * Request to set/update SMTP server configuration for current device.
     * Return true (if operation succeeded) or false (if failure).
     *
     * @param ctx           Channel between SmartServer and the client.
     * @param parameters    Server Address, TCP Port, Username, Password, SSL enabled.
     */
    @Override
    public synchronized void execute(ChannelHandlerContext ctx, String[] parameters)
    {
        String address = parameters[0];
        int port;
        String username = parameters[2], password = parameters[3];
        boolean sslEnabled = Boolean.parseBoolean(parameters[4]);

        try
        {
            port = Integer.parseInt(parameters[1]);
        } catch(NumberFormatException nfe)
        {
            SmartLogger.getLogger().log(Level.SEVERE, "TCP Port number invalid.", nfe);
            SmartServer.sendMessage(ctx, RequestCode.SET_SMTP_SERVER, FALSE);
            return;
        }

        try
        {
            SmtpServer smtpServer = new SmtpServer(address, port, username, password, sslEnabled);

            if (!((DaoSmtpServer) DbManager.getDao(SmtpServerEntity.class))
                    .persist(smtpServer))
            {
                SmartServer.sendMessage(ctx, RequestCode.SET_SMTP_SERVER, FALSE);
                return;
            }
        } catch(IllegalArgumentException iae)
        {
            SmartLogger.getLogger().log(Level.SEVERE, "Invalid SmtpServer provided.", iae);
            SmartServer.sendMessage(ctx, RequestCode.SET_SMTP_SERVER, FALSE);
            return;
        }

        SmartServer.sendMessage(ctx, RequestCode.SET_SMTP_SERVER, TRUE);
    }
}
