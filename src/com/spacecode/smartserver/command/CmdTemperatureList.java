package com.spacecode.smartserver.command;

import com.spacecode.sdk.network.communication.RequestCode;
import com.spacecode.smartserver.SmartServer;
import com.spacecode.smartserver.database.DbManager;
import com.spacecode.smartserver.database.entity.TemperatureMeasurementEntity;
import com.spacecode.smartserver.database.repository.TemperatureMeasurementRepository;
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
public class CmdTemperatureList extends ClientCommand
{
    /**
     * @param ctx           Channel between SmartServer and the client.
     * @param parameters    String array containing parameters (if any) provided by the client.
     *
     * @throws ClientCommandException
     */
    @Override
    public void execute(ChannelHandlerContext ctx, String[] parameters) throws ClientCommandException
    {
        // waiting for 2 parameters: start date, end date.
        if(parameters.length != 2)
        {
            SmartServer.sendMessage(ctx, RequestCode.TEMPERATURE_LIST);
            throw new ClientCommandException("Invalid number of parameters.");
        }

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

        TemperatureMeasurementRepository repo =
                (TemperatureMeasurementRepository) DbManager.getRepository(TemperatureMeasurementEntity.class);

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
