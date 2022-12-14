package com.spacecode.smartserver.command;

import com.spacecode.sdk.network.communication.RequestCode;
import com.spacecode.smartserver.SmartServer;
import com.spacecode.smartserver.database.DbManager;
import com.spacecode.smartserver.database.dao.DaoAlertHistory;
import com.spacecode.smartserver.database.dao.DaoAlertTemperature;
import com.spacecode.smartserver.database.entity.AlertEntity;
import com.spacecode.smartserver.database.entity.AlertHistoryEntity;
import com.spacecode.smartserver.database.entity.AlertTemperatureEntity;
import io.netty.channel.ChannelHandlerContext;

/**
 * LastAlert command.
 * Provide device's last alert raised (serialized).
 */
@CommandContract(deviceRequired = true, responseIfInvalid = "")
public class CmdLastAlert extends ClientCommand
{
    /**
     * Serialize the last alert raised and send it to current context.
     *
     * @param ctx           Channel between SmartServer and the client.
     * @param parameters    None expected.
     */
    @Override
    public void execute(ChannelHandlerContext ctx, String[] parameters)
    {
        DaoAlertHistory daoAlertHistory = (DaoAlertHistory)
                DbManager.getDao(AlertHistoryEntity.class);
        DaoAlertTemperature daoAlertTemp = (DaoAlertTemperature)
                DbManager.getDao(AlertTemperatureEntity.class);

        AlertHistoryEntity alertHisto = daoAlertHistory.getLastAlertHistory();

        if(alertHisto != null)
        {
            AlertEntity lastAlert = alertHisto.getAlert();

            if(lastAlert == null)
            {
                SmartServer.sendMessage(ctx, RequestCode.LAST_ALERT, "");
                return;
            }

            // is the alert an AlertTemperature? let's seek a matching one
            AlertTemperatureEntity ate =
                    daoAlertTemp.getEntityBy(AlertTemperatureEntity.ALERT_ID, lastAlert.getId());

            if(ate != null)
            {
                // send the alert as an AlertTemperature [by passing an AlertTemperatureEntity]
                SmartServer.sendMessage(ctx,
                        RequestCode.LAST_ALERT,
                        AlertEntity.toAlert(ate).serialize(),
                        String.valueOf(alertHisto.getCreatedAt().getTime()/1000),
                        alertHisto.getExtraData());
                return;
            }

            // send the alert as an Alert
            SmartServer.sendMessage(ctx,
                    RequestCode.LAST_ALERT,
                    AlertEntity.toAlert(lastAlert).serialize(),
                    String.valueOf(alertHisto.getCreatedAt().getTime()/1000),
                    alertHisto.getExtraData());
            return;
        }

        SmartServer.sendMessage(ctx, RequestCode.LAST_ALERT, "");
    }
}
