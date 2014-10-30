package com.spacecode.smartserver.command;

import com.spacecode.sdk.network.communication.RequestCode;
import com.spacecode.smartserver.SmartServer;
import com.spacecode.smartserver.database.DatabaseHandler;
import com.spacecode.smartserver.database.entity.TemperatureMeasurementEntity;
import com.spacecode.smartserver.database.repository.TemperatureMeasurementRepository;
import com.spacecode.smartserver.helper.DeviceHandler;
import com.spacecode.smartserver.helper.SmartLogger;
import io.netty.channel.ChannelHandlerContext;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.logging.Level;

/**
 * "TemperatureRange" command.
 *
 * Provide temperature measurements over a given period (start/end date provided), if any.
 */
public class CommandTempRange extends ClientCommand
{
    /**
     * Serialize device's last inventory and send it to current context.
     *
     * @param ctx           ChannelHandlerContext instance corresponding to the channel existing between SmartServer and the client.
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
            SmartServer.sendMessage(ctx, RequestCode.TEMP_RANGE);
            throw new ClientCommandException("Invalid number of parameters.");
        }

        if(DeviceHandler.getDevice() == null)
        {
            SmartServer.sendMessage(ctx, RequestCode.TEMP_RANGE);
            return;
        }

        long timestampStart;
        long timestampEnd;

        try
        {
            timestampStart  = Long.parseLong(parameters[0]);
            timestampEnd    = Long.parseLong(parameters[1]);
        } catch(NumberFormatException nfe)
        {
            SmartLogger.getLogger().log(Level.WARNING, "Invalid timestamp sent by client for TemperatureRange.", nfe);
            SmartServer.sendMessage(ctx, RequestCode.TEMP_RANGE);
            return;
        }

        TemperatureMeasurementRepository repo = (TemperatureMeasurementRepository) DatabaseHandler.getRepository(TemperatureMeasurementEntity.class);

        List<TemperatureMeasurementEntity> entities = repo.getTemperatureMeasures(new Date(timestampStart), new Date(timestampEnd));

        List<String> responsePackets = new ArrayList<>();
        responsePackets.add(RequestCode.TEMP_RANGE);

        for(TemperatureMeasurementEntity entity : entities)
        {
            // add TIMESTAMP in seconds and temperature measurement value
            responsePackets.add(String.valueOf(entity.getCreatedAt().getTime()/1000));
            responsePackets.add(String.valueOf(entity.getValue()));
        }

        SmartServer.sendMessage(ctx, responsePackets.toArray(new String[0]));
    }
}
