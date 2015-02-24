package com.spacecode.smartserver.command;

import com.spacecode.sdk.network.communication.RequestCode;
import com.spacecode.smartserver.SmartServer;
import com.spacecode.smartserver.database.DbManager;
import com.spacecode.smartserver.database.entity.InventoryEntity;
import com.spacecode.smartserver.database.repository.Repository;
import com.spacecode.smartserver.helper.DeviceHandler;
import io.netty.channel.ChannelHandlerContext;

/**
 * InventoryById command.
 */
public class CmdInventoryById extends ClientCommand
{
    /**
     * Request to get a copy-instance of an Inventory from its ID. Send the serialized Inventory instance (if any).
     * ID's should not be used by end-users but it has been done in SmartTracker, so we need to maintain it...
     *
     * @param ctx                       Channel between SmartServer and the client.
     * @param parameters                String array containing parameters (if any) provided by the client.
     *
     * @throws ClientCommandException
     */
    @Override
    public void execute(ChannelHandlerContext ctx, String[] parameters) throws ClientCommandException
    {
        // waiting for 1 parameter: inventory id.
        if(parameters.length != 1)
        {
            SmartServer.sendMessage(ctx, RequestCode.INVENTORY_BY_ID, "");
            throw new ClientCommandException("Invalid number of parameters [InventoryById].");
        }

        if(DeviceHandler.getDevice() == null)
        {
            SmartServer.sendMessage(ctx, RequestCode.INVENTORY_BY_ID, "");
            return;
        }

        String inventoryId = parameters[0];

        try
        {
            int id = Integer.parseInt(inventoryId);
            Repository<InventoryEntity> repoInventory = DbManager.getRepository(InventoryEntity.class);
            InventoryEntity invEntity = repoInventory.getEntityById(id);

            if(invEntity == null)
            {
                SmartServer.sendMessage(ctx, RequestCode.INVENTORY_BY_ID, "");
                return;
            }

            SmartServer.sendMessage(ctx, RequestCode.INVENTORY_BY_ID, invEntity.asInventory().serialize());
        } catch(NumberFormatException nfe)
        {
            SmartServer.sendMessage(ctx, RequestCode.INVENTORY_BY_ID, "");
        }
    }
}
