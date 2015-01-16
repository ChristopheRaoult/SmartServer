package com.spacecode.smartserver.command;

import com.spacecode.sdk.device.data.Inventory;
import com.spacecode.sdk.network.communication.RequestCode;
import com.spacecode.smartserver.SmartServer;
import com.spacecode.smartserver.database.DbManager;
import com.spacecode.smartserver.database.entity.InventoryEntity;
import com.spacecode.smartserver.database.repository.InventoryRepository;
import com.spacecode.smartserver.helper.SmartLogger;
import io.netty.channel.ChannelHandlerContext;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.logging.Level;

/**
 * InventoriesList command.
 *
 * Provide inventories over a given period (start/end date provided), if any.
 */
public class CommandInventoriesList extends ClientCommand
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
            SmartServer.sendMessage(ctx, RequestCode.INVENTORIES_LIST);
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
                    "Invalid timestamp sent by client for Inventories.", nfe);
            SmartServer.sendMessage(ctx, RequestCode.INVENTORIES_LIST);
            return;
        }

        if(timestampEnd <= timestampStart)
        {
            SmartServer.sendMessage(ctx, RequestCode.INVENTORIES_LIST);
            return;
        }

        InventoryRepository repo = (InventoryRepository) DbManager.getRepository(InventoryEntity.class);

        List<Inventory> inventories = repo.getInventories(new Date(timestampStart), new Date(timestampEnd),
                DbManager.getDeviceConfiguration());

        List<String> responsePackets = new ArrayList<>();
        responsePackets.add(RequestCode.INVENTORIES_LIST);

        for(Inventory inventory : inventories)
        {
            responsePackets.add(inventory.serialize());
        }

        SmartServer.sendMessage(ctx, responsePackets.toArray(new String[0]));
    }
}
