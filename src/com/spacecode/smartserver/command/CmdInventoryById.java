package com.spacecode.smartserver.command;

import com.spacecode.sdk.network.communication.RequestCode;
import com.spacecode.smartserver.SmartServer;
import com.spacecode.smartserver.database.DbManager;
import com.spacecode.smartserver.database.entity.InventoryEntity;
import io.netty.channel.ChannelHandlerContext;

/**
 * InventoryById command.
 */
@CommandContract(paramCount = 1, strictCount = true, deviceRequired = true, responseWhenInvalid = "")
public class CmdInventoryById extends ClientCommand
{
    /**
     * Request to get a copy-instance of an Inventory from its ID. Send the serialized Inventory instance (if any).
     * ID's should not be used by end-users but it has been done in SmartTracker, so we need to maintain it...
     *
     * @param ctx           Channel between SmartServer and the client.
     * @param parameters    Inventory ID.
     */
    @Override
    public void execute(ChannelHandlerContext ctx, String[] parameters)
    {
        String inventoryId = parameters[0];

        try
        {
            int id = Integer.parseInt(inventoryId);
            InventoryEntity invEntity = DbManager.getDao(InventoryEntity.class).getEntityById(id);

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
