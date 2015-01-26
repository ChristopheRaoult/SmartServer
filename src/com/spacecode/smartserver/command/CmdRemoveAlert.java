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
 * RemoveAlert command.
 */
public class CmdRemoveAlert extends ClientCommand
{
    /**
     * Request to remove an Alert from database. Send (string) "true" if succeed, "false" otherwise.
     * @param ctx                       Channel between SmartServer and the client.
     * @param parameters                String array containing parameters (if any) provided by the client.
     * @throws ClientCommandException
     */
    @Override
    public synchronized void execute(ChannelHandlerContext ctx, String[] parameters) throws ClientCommandException
    {
        // waiting for 1 parameter: serialized alert to be removed
        if(parameters.length != 1)
        {
            SmartServer.sendMessage(ctx, RequestCode.REMOVE_ALERT, FALSE);
            throw new ClientCommandException("Invalid number of parameters.");
        }

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

        if(!((AlertRepository) DbManager.getRepository(AlertEntity.class)).delete(alert))
        {
            SmartServer.sendMessage(ctx, RequestCode.REMOVE_ALERT, FALSE);
            return;
        }

        SmartServer.sendMessage(ctx, RequestCode.REMOVE_ALERT, TRUE);
    }
}
