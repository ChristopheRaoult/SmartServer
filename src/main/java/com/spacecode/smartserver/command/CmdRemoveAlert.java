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
 * RemoveAlert command.
 */
@CommandContract(paramCount = 1, strictCount = true)
public class CmdRemoveAlert extends ClientCommand
{
    /**
     * Request to remove an Alert from database. Send (string) "true" if succeed, "false" otherwise.
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
            SmartServer.sendMessage(ctx, RequestCode.REMOVE_ALERT, FALSE);
            return;
        }

        if(alert.getType() == AlertType.TEMPERATURE && !(alert instanceof AlertTemperature))
        {
            SmartServer.sendMessage(ctx, RequestCode.REMOVE_ALERT, FALSE);
            return;
        }

        if(!((DaoAlert) DbManager.getDao(AlertEntity.class)).deleteFromAlert(alert))
        {
            SmartServer.sendMessage(ctx, RequestCode.REMOVE_ALERT, FALSE);
            return;
        }

        SmartServer.sendMessage(ctx, RequestCode.REMOVE_ALERT, TRUE);
    }
}
