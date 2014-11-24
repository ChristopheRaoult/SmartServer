package com.spacecode.smartserver.command;

import com.spacecode.sdk.network.communication.RequestCode;
import com.spacecode.smartserver.SmartServer;
import com.spacecode.smartserver.database.DatabaseHandler;
import com.spacecode.smartserver.database.entity.AlertEntity;
import com.spacecode.smartserver.database.entity.AlertHistoryEntity;
import com.spacecode.smartserver.database.entity.AlertTemperatureEntity;
import com.spacecode.smartserver.database.repository.Repository;
import io.netty.channel.ChannelHandlerContext;

/**
 * LastAlert command.
 * Provide device's last alert raised (serialized).
 */
public class CommandLastAlert extends ClientCommand
{
    /**
     * Serialize the last alert raised and send it to current context.
     *
     * @param ctx           Channel between SmartServer and the client.
     * @param parameters    String array containing parameters (if any) provided by the client.
     *
     * @throws ClientCommandException
     */
    @Override
    public void execute(ChannelHandlerContext ctx, String[] parameters) throws ClientCommandException
    {
        Repository<AlertHistoryEntity> histoRepo = DatabaseHandler.getRepository(AlertHistoryEntity.class);
        Repository<AlertTemperatureEntity> alertTempRepo =
                DatabaseHandler.getRepository(AlertTemperatureEntity.class);

        AlertHistoryEntity ent = histoRepo.getEntityByMax(AlertHistoryEntity.CREATED_AT);

        if(ent != null)
        {
            AlertEntity lastAlert = ent.getAlert();

            if(lastAlert == null)
            {
                SmartServer.sendMessage(ctx, RequestCode.LAST_ALERT, "");
                return;
            }

            AlertTemperatureEntity ate =
                    alertTempRepo.getEntityBy(AlertTemperatureEntity.ALERT_ID, lastAlert.getId());

            if(ate != null)
            {
                SmartServer.sendMessage(ctx, RequestCode.LAST_ALERT, AlertEntity.toAlert(ate).serialize());
                return;
            }

            SmartServer.sendMessage(ctx, RequestCode.LAST_ALERT, AlertEntity.toAlert(lastAlert).serialize());
            return;
        }

        SmartServer.sendMessage(ctx, RequestCode.LAST_ALERT, "");
    }
}
