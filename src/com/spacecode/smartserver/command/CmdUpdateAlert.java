package com.spacecode.smartserver.command;

import com.spacecode.sdk.network.alert.Alert;
import com.spacecode.sdk.network.alert.AlertTemperature;
import com.spacecode.sdk.network.alert.AlertType;
import com.spacecode.sdk.network.communication.RequestCode;
import com.spacecode.smartserver.SmartServer;
import com.spacecode.smartserver.database.DbManager;
import com.spacecode.smartserver.database.entity.AlertEntity;
import com.spacecode.smartserver.database.repository.AlertRepository;
import io.netty.channel.ChannelHandlerContext;

/**
 * UpdateAlert command.
 */
public class CmdUpdateAlert extends ClientCommand
{
    /**
     * Request to update an alert (in database). Return true (if operation succeeded) or false (if failure).
     * @param ctx                       Channel between SmartServer and the client.
     * @param parameters                String array containing parameters (if any) provided by the client.
     * @throws ClientCommandException
     */
    @Override
    public synchronized void execute(ChannelHandlerContext ctx, String[] parameters) throws ClientCommandException
    {
        // waiting for 1 parameters: serialized Alert.
        if(parameters.length != 1)
        {
            SmartServer.sendMessage(ctx, RequestCode.UPDATE_ALERT, FALSE);
            throw new ClientCommandException("Invalid number of parameters.");
        }

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

        if(!((AlertRepository)DbManager.getRepository(AlertEntity.class)).persist(alert))
        {
            SmartServer.sendMessage(ctx, RequestCode.UPDATE_ALERT, FALSE);
            return;
        }

        SmartServer.sendMessage(ctx, RequestCode.UPDATE_ALERT, TRUE);
    }
}
