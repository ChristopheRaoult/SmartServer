package com.spacecode.smartserver.command;

import com.spacecode.sdk.network.alert.Alert;
import com.spacecode.sdk.network.alert.AlertTemperature;
import com.spacecode.sdk.network.alert.AlertType;
import com.spacecode.sdk.network.communication.RequestCode;
import com.spacecode.smartserver.SmartServer;
import com.spacecode.smartserver.database.DatabaseHandler;
import io.netty.channel.ChannelHandlerContext;

/**
 * AddAlert command.
 */
public class CommandAddAlert extends ClientCommand
{
    /**
     * Request to add a new Alert to database. Send (string) "true" if succeed, "false" otherwise.
     * @param ctx                       ChannelHandlerContext instance corresponding to the channel existing between SmartServer and the client.
     * @param parameters                String array containing parameters (if any) provided by the client.
     * @throws ClientCommandException
     */
    @Override
    public synchronized void execute(ChannelHandlerContext ctx, String[] parameters) throws ClientCommandException
    {
        // waiting for only 1 parameter: serialized alert
        if(parameters.length != 1)
        {
            SmartServer.sendMessage(ctx, RequestCode.ADD_ALERT, FALSE);
            throw new ClientCommandException("Invalid number of parameters.");
        }

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

        if(!DatabaseHandler.persistAlert(newAlert))
        {
            SmartServer.sendMessage(ctx, RequestCode.ADD_ALERT, FALSE);
            return;
        }

        SmartServer.sendMessage(ctx, RequestCode.ADD_ALERT, TRUE);
    }
}