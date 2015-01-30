package com.spacecode.smartserver.command;

import com.spacecode.sdk.network.DbConfiguration;
import com.spacecode.sdk.network.communication.RequestCode;
import com.spacecode.smartserver.SmartServer;
import com.spacecode.smartserver.database.DbManager;
import com.spacecode.smartserver.helper.ConfManager;
import com.spacecode.smartserver.helper.SmartLogger;
import io.netty.channel.ChannelHandlerContext;

import java.util.logging.Level;

/**
 * Command SetDbSettings.
 */
public class CmdSetDbSettings extends ClientCommand
{
    /**
     * Request to set/update the DB settings for the current device.
     * Return true (if operation succeeded) or false (if failure).
     *
     * @param ctx                       Channel between SmartServer and the client.
     * @param parameters                String array containing parameters (if any) provided by the client.
     *
     * @throws ClientCommandException   Invalid number of parameters received.
     */
    @Override
    public synchronized void execute(ChannelHandlerContext ctx, String[] parameters) throws ClientCommandException
    {
        // waiting for 6 parameters: host, port, db name, username, password, dbms.
        if(parameters.length != 6)
        {
            SmartServer.sendMessage(ctx, RequestCode.SET_DB_SETTINGS, FALSE);
            throw new ClientCommandException("Invalid number of parameters.");
        }

        try
        {
            String host = parameters[0];
            int port = Integer.parseInt(parameters[1]);
            String dbName = parameters[2], user = parameters[3], password = parameters[4], dbms = parameters[5];

            DbConfiguration newConfig = new DbConfiguration(host, port, dbName, user, password, dbms);

            SmartServer.sendMessage(ctx, RequestCode.SET_DB_SETTINGS,
                    ConfManager.setDbConfiguration(newConfig) ? TRUE : FALSE);

            SmartLogger.getLogger().info("DB Settings have changed... Connecting to Database...");

            if(!DbManager.initializeDatabase())
            {
                SmartLogger.getLogger()
                        .severe("Unable to initialize Database. New database configuration may be invalid.");
            }
        } catch(NumberFormatException nfe)
        {
            SmartLogger.getLogger().log(Level.SEVERE, "TCP Port number invalid.", nfe);
            SmartServer.sendMessage(ctx, RequestCode.SET_DB_SETTINGS, FALSE);
        }
    }
}

