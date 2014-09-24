package com.spacecode.smartserver.command;

import com.spacecode.sdk.network.communication.RequestCode;
import com.spacecode.smartserver.SmartServer;
import com.spacecode.smartserver.database.DatabaseHandler;
import com.spacecode.smartserver.helper.SmartLogger;
import io.netty.channel.ChannelHandlerContext;

import java.util.logging.Level;

/**
 * SetSmtpServer command.
 */
public class CommandSetSmtpServer extends ClientCommand
{
    /**
     * Request to set/update SMTP server configuration for current device. Return true (if operation succeeded) or false (if failure).
     *
     * @param ctx                       ChannelHandlerContext instance corresponding to the channel existing between SmartServer and the client.
     * @param parameters                String array containing parameters (if any) provided by the client.
     *
     * @throws ClientCommandException   Invalid number of parameters received.
     */
    @Override
    public synchronized void execute(ChannelHandlerContext ctx, String[] parameters) throws ClientCommandException
    {
        // waiting for 5 parameters: server address, tcp port, username,  password, sslEnabled boolean.
        if(parameters.length != 5)
        {
            SmartServer.sendMessage(ctx, RequestCode.SET_SMTP_SERVER, FALSE);
            throw new ClientCommandException("Invalid number of parameters.");
        }

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

        if(!DatabaseHandler.persistSmtpServer(address, port, username, password, sslEnabled))
        {
            SmartServer.sendMessage(ctx, RequestCode.SET_SMTP_SERVER, FALSE);
            return;
        }

        SmartServer.sendMessage(ctx, RequestCode.SET_SMTP_SERVER, TRUE);
    }
}
