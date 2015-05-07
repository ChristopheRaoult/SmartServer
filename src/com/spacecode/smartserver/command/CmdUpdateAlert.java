package com.spacecode.smartserver.command;

import com.spacecode.sdk.network.alert.Alert;
import com.spacecode.sdk.network.alert.AlertTemperature;
import com.spacecode.sdk.network.alert.AlertType;
import com.spacecode.sdk.network.communication.RequestCode;
import com.spacecode.smartserver.SmartServer;
import com.spacecode.smartserver.database.DbManager;
import com.spacecode.smartserver.database.dao.DaoAlert;
import com.spacecode.smartserver.database.entity.AlertEntity;
import io.netty.channel.ChannelHandlerContext;

/**
 * UpdateAlert command.
 */
@CommandContract(paramCount = 1, strictCount = true)
public class CmdUpdateAlert extends ClientCommand
{
    /**
     * Request to update an alert (in database). Return true (if operation succeeded) or false (if failure).
     * 
     * @param ctx           Channel between SmartServer and the client.
     * @param parameters    Serialized Alert.
     */
    @Override
    public synchronized void execute(ChannelHandlerContext ctx, String[] parameters)
    {
        Alert alert = Alert.deserialize(parameters[0]);

        if(alert == null)
        {
            SmartServer.sendMessage(ctx, RequestCode.UPDATE_ALERT, FALSE);
            return;
        }

        if(alert.getType() == AlertType.TEMPERATURE && !(alert instanceof AlertTemperature))
        {
            SmartServer.sendMessage(ctx, RequestCode.UPDATE_ALERT, FALSE);
            return;
        }

        DaoAlert daoAlert = (DaoAlert)DbManager.getDao(AlertEntity.class);
        if(!daoAlert.persist(alert))
        {
            SmartServer.sendMessage(ctx, RequestCode.UPDATE_ALERT, FALSE);
            return;
        }

        SmartServer.sendMessage(ctx, RequestCode.UPDATE_ALERT, TRUE);
    }
}
