package com.spacecode.smartserver.command;

import com.spacecode.sdk.device.data.Inventory;
import com.spacecode.sdk.network.communication.RequestCode;
import com.spacecode.smartserver.SmartServer;
import com.spacecode.smartserver.database.DatabaseHandler;
import com.spacecode.smartserver.database.entity.InventoryEntity;
import com.spacecode.smartserver.database.repository.InventoryRepository;
import com.spacecode.smartserver.helper.SmartLogger;
import io.netty.channel.ChannelHandlerContext;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.logging.Level;

/**
 * InventoryRange command.
 *
 * Provide inventories over a given period (start/end date provided), if any.
 */
public class CommandInventoryRange extends ClientCommand
{
    /**
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
            SmartServer.sendMessage(ctx, RequestCode.INVENTORY_RANGE);
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
            SmartLogger.getLogger().log(Level.WARNING, "Invalid timestamp sent by client for InventoryRange.", nfe);
            SmartServer.sendMessage(ctx, RequestCode.INVENTORY_RANGE);
            return;
        }

        if(timestampEnd <= timestampStart)
        {
            SmartServer.sendMessage(ctx, RequestCode.INVENTORY_RANGE);
            return;
        }

        InventoryRepository repo = (InventoryRepository) DatabaseHandler.getRepository(InventoryEntity.class);

        List<Inventory> inventories = repo.getInventories(new Date(timestampStart), new Date(timestampEnd));

        List<String> responsePackets = new ArrayList<>();
        responsePackets.add(RequestCode.INVENTORY_RANGE);

        for(Inventory inventory : inventories)
        {
            responsePackets.add(inventory.serialize());
        }

        SmartServer.sendMessage(ctx, responsePackets.toArray(new String[0]));
    }
}
