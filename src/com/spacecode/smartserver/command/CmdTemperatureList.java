package com.spacecode.smartserver.command;

import com.spacecode.sdk.network.communication.RequestCode;
import com.spacecode.smartserver.SmartServer;
import com.spacecode.smartserver.database.DbManager;
import com.spacecode.smartserver.database.dao.DaoTemperatureMeasurement;
import com.spacecode.smartserver.database.entity.TemperatureMeasurementEntity;
import com.spacecode.smartserver.helper.SmartLogger;
import io.netty.channel.ChannelHandlerContext;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.logging.Level;

/**
 * "TemperatureList" command.
 *
 * Provide temperature measurements over a given period (start/end date provided), if any.
 */
@CommandContract(paramCount = 2, strictCount = true, deviceRequired = true, responseIfInvalid = "")
public class CmdTemperatureList extends ClientCommand
{
    /**
     * @param ctx           Channel between SmartServer and the client.
     * @param parameters    "Start" and "End" dates (period).
     */
    @Override
    public void execute(ChannelHandlerContext ctx, String[] parameters)
    {
        long timestampStart;
        long timestampEnd;

        try
        {
            timestampStart  = Long.parseLong(parameters[0]);
            timestampEnd    = Long.parseLong(parameters[1]);
        } catch(NumberFormatException nfe)
        {
            SmartLogger.getLogger().log(Level.WARNING,
                    "Invalid timestamp sent by client for TemperatureList.", nfe);
            SmartServer.sendMessage(ctx, RequestCode.TEMPERATURE_LIST);
            return;
        }

        if(timestampEnd <= timestampStart)
        {
            SmartServer.sendMessage(ctx, RequestCode.TEMPERATURE_LIST);
            return;
        }

        DaoTemperatureMeasurement repo =
                (DaoTemperatureMeasurement) DbManager.getDao(TemperatureMeasurementEntity.class);

        List<TemperatureMeasurementEntity> entities =
                repo.getTemperatureMeasures(new Date(timestampStart), new Date(timestampEnd));

        List<String> responsePackets = new ArrayList<>();
        responsePackets.add(RequestCode.TEMPERATURE_LIST);

        for(TemperatureMeasurementEntity entity : entities)
        {
            // add TIMESTAMP in seconds and temperature measurement value
            responsePackets.add(String.valueOf(entity.getCreatedAt().getTime()/1000));
            responsePackets.add(String.valueOf(entity.getValue()));
        }

        SmartServer.sendMessage(ctx, responsePackets.toArray(new String[responsePackets.size()]));
    }
}
