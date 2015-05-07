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
@CommandContract(paramCount = 6, strictCount = true)
public class CmdSetDbSettings extends ClientCommand
{
    /**
     * Request to set/update the DB settings for the current device.
     * Return true (if operation succeeded) or false (if failure).
     *
     * @param ctx           Channel between SmartServer and the client.
     * @param parameters    Host, Port, DB Name, Username, Password, DBMS.
     */
    @Override
    public synchronized void execute(ChannelHandlerContext ctx, String[] parameters)
    {
        try
        {
            String host = parameters[0];
            int port = Integer.parseInt(parameters[1]);
            String dbName = parameters[2], user = parameters[3], password = parameters[4], dbms = parameters[5];

            DbConfiguration newConfig = new DbConfiguration(host, port, dbName, user, password, dbms);

            if(!ConfManager.setDbConfiguration(newConfig))
            {
                SmartServer.sendMessage(ctx, RequestCode.SET_DB_SETTINGS, FALSE);
                return;
            }

            SmartServer.sendMessage(ctx, RequestCode.SET_DB_SETTINGS, TRUE);

            SmartLogger.getLogger().info("DB Settings have changed... Connecting to Database...");

            if(!DbManager.initializeDatabase())
            {
                SmartLogger.getLogger()
                        .severe("Unable to re-initialize Database. New database configuration may be invalid.");
            }
            return;
        } catch(NumberFormatException nfe)
        {
            SmartLogger.getLogger().log(Level.SEVERE, "TCP Port number invalid.", nfe);
        } catch(IllegalArgumentException iae)
        {
            SmartLogger.getLogger().log(Level.SEVERE, "Invalid DbConfiguration provided.", iae);
        }

        SmartServer.sendMessage(ctx, RequestCode.SET_DB_SETTINGS, FALSE);
    }
}

