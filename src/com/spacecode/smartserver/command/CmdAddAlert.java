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
 * AddAlert command.
 */
@CommandContract(paramCount = 1, strictCount = true)
public class CmdAddAlert extends ClientCommand
{
    /**
     * Request to add a new Alert to database. Sends back "true" if succeed, "false" otherwise.
     * 
     * @param ctx           Channel between SmartServer and the client.
     * @param parameters    Serialized Alert.
     */
    @Override
    public synchronized void execute(ChannelHandlerContext ctx, String[] parameters)
    {
        Alert newAlert = Alert.deserialize(parameters[0]);

        if(newAlert == null || newAlert.getId() != 0)
        {
            // alert couldn't be deserialized or already has an id (it's coming from db: can't be re-created)
            SmartServer.sendMessage(ctx, RequestCode.ADD_ALERT, FALSE);
            return;
        }

        if(newAlert.getType() == AlertType.TEMPERATURE && !(newAlert instanceof AlertTemperature))
        {
            // let's make sure the alert is really an AlertTemperature if it's declared as one.
            SmartServer.sendMessage(ctx, RequestCode.ADD_ALERT, FALSE);
            return;
        }

        // persist the new alert in DB
        DaoAlert daoAlert = (DaoAlert) DbManager.getDao(AlertEntity.class);
        if(!daoAlert.persist(newAlert))
        {
            SmartServer.sendMessage(ctx, RequestCode.ADD_ALERT, FALSE);
            return;
        }

        SmartServer.sendMessage(ctx, RequestCode.ADD_ALERT, TRUE);
    }
}
