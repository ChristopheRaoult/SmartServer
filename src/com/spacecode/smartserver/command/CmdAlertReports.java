package com.spacecode.smartserver.command;

import com.spacecode.sdk.network.communication.RequestCode;
import com.spacecode.smartserver.SmartServer;
import com.spacecode.smartserver.database.DbManager;
import com.spacecode.smartserver.database.dao.DaoAlertHistory;
import com.spacecode.smartserver.database.entity.AlertHistoryEntity;
import com.spacecode.smartserver.helper.SmartLogger;
import io.netty.channel.ChannelHandlerContext;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.logging.Level;

/**
 * "AlertReports" command.
 *
 * Provide reports (if any) for alert raised during a certain period (start/end date provided).
 * Sends Alert IDs instead of sending serialized alerts, in order to minimize risk to exceed the TCP frame size.
 */
public class CmdAlertReports extends ClientCommand
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
            SmartServer.sendMessage(ctx, RequestCode.ALERT_REPORTS);
            throw new ClientCommandException("Invalid number of parameters [AlertReports].");
        }

        DaoAlertHistory daoAlertHistory =
                (DaoAlertHistory) DbManager.getDao(AlertHistoryEntity.class);
        
        if(daoAlertHistory == null)
        {
            SmartServer.sendMessage(ctx, RequestCode.ALERT_REPORTS);
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
            SmartLogger.getLogger().log(Level.WARNING,
                    "Invalid timestamp sent by client for AlertReports.", nfe);
            SmartServer.sendMessage(ctx, RequestCode.ALERT_REPORTS);
            return;
        }

        if(timestampEnd <= timestampStart)
        {
            SmartServer.sendMessage(ctx, RequestCode.ALERT_REPORTS);
            return;
        }

        List<AlertHistoryEntity> entities = 
                daoAlertHistory.getAlertsHistory(new Date(timestampStart), new Date(timestampEnd));

        List<String> responsePackets = new ArrayList<>();
        responsePackets.add(RequestCode.ALERT_REPORTS);

        for(AlertHistoryEntity entity : entities)
        {
            // add: [alert id, timestamp (seconds), extra data]
            responsePackets.add(String.valueOf(entity.getAlert().getId()));
            responsePackets.add(String.valueOf(entity.getCreatedAt().getTime()/1000));
            responsePackets.add("".equals(entity.getExtraData()) ? " " : entity.getExtraData());
        }

        SmartServer.sendMessage(ctx, responsePackets.toArray(new String[responsePackets.size()]));
    }
}
