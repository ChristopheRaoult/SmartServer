package com.spacecode.smartserver.command;

import com.spacecode.sdk.network.alert.Alert;
import com.spacecode.sdk.network.alert.AlertType;
import com.spacecode.sdk.network.communication.RequestCode;
import com.spacecode.smartserver.SmartServer;
import com.spacecode.smartserver.database.DatabaseHandler;
import com.spacecode.smartserver.database.entity.AlertEntity;
import com.spacecode.smartserver.database.entity.AlertTemperatureEntity;
import com.spacecode.smartserver.database.entity.AlertTypeEntity;
import com.spacecode.smartserver.database.repository.AlertTypeRepository;
import com.spacecode.smartserver.helper.SmartLogger;
import io.netty.channel.ChannelHandlerContext;

import java.util.ArrayList;
import java.util.List;

/**
 * AlertsList command.
 */
public class CommandAlertsList extends ClientCommand
{
    /**
     * Request to get the alerts list. Send the list of Alerts as serialized alerts (strings).
     * @param ctx                       ChannelHandlerContext instance corresponding to the channel existing between SmartServer and the client.
     * @param parameters                String array containing parameters (if any) provided by the client.
     * @throws ClientCommandException
     */
    @Override
    public void execute(ChannelHandlerContext ctx, String[] parameters) throws ClientCommandException
    {
        AlertTypeEntity alertTypeTemperature =
                ((AlertTypeRepository) DatabaseHandler.getRepository(AlertTypeEntity.class))
                        .fromAlertType(AlertType.TEMPERATURE);

        List<String> responsePackets = new ArrayList<>();

        if(alertTypeTemperature == null)
        {
            SmartServer.sendMessage(ctx, responsePackets.toArray(new String[0]));
            SmartLogger.getLogger().severe("Unable to get AlertTypeEntity Temperature from DB.");
            return;
        }

        // first, get the simple alerts (made with only an AlertEntity instance)
        List<AlertEntity> simpleAlertsFromDb =
                DatabaseHandler.getRepository(AlertEntity.class)
                        .getEntitiesWhereNotEqual(AlertEntity.ALERT_TYPE_ID, alertTypeTemperature.getId());
        // then get all AlertTemperature ~
        List<AlertTemperatureEntity> alertsTemperatureFromDb =
                DatabaseHandler.getRepository(AlertTemperatureEntity.class).getAll();


        List<Alert> serializableAlerts = new ArrayList<>();

        for(AlertEntity ae : simpleAlertsFromDb)
        {
            serializableAlerts.add(AlertEntity.toAlert(ae));
        }

        for(AlertTemperatureEntity alertTempE : alertsTemperatureFromDb)
        {
            serializableAlerts.add(AlertEntity.toAlert(alertTempE));
        }

        // add the request code first
        responsePackets.add(RequestCode.ALERTS_LIST);

        // then all the serialized alerts
        for(Alert alert : serializableAlerts)
        {
            responsePackets.add(alert.serialize());
        }

        // then all the serialized alerts

        SmartServer.sendMessage(ctx, responsePackets.toArray(new String[0]));
    }
}
