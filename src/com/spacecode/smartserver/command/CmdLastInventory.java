package com.spacecode.smartserver.command;

import com.spacecode.sdk.device.data.Inventory;
import com.spacecode.sdk.network.communication.RequestCode;
import com.spacecode.smartserver.SmartServer;
import com.spacecode.smartserver.database.DbManager;
import com.spacecode.smartserver.database.entity.InventoryEntity;
import com.spacecode.smartserver.database.repository.InventoryRepository;
import com.spacecode.smartserver.helper.DeviceHandler;
import io.netty.channel.ChannelHandlerContext;

/**
 * LastInventory command.
 *
 * Provide device's last inventory (serialized).
 */
public class CmdLastInventory extends ClientCommand
{
    private Inventory _lastInventory = null;

    /**
     * Serialize device's last inventory and send it to current context.
     *
     * @param ctx                       Channel between SmartServer and the client.
     * @param parameters                String array containing parameters (if any) provided by the client.
     *
     * @throws ClientCommandException
     */
    @Override
    public void execute(ChannelHandlerContext ctx, String[] parameters) throws ClientCommandException
    {
        if(DeviceHandler.getDevice() == null)
        {
            return;
        }

        // no inventory in cache, try to get the last inventory from Database
        if(_lastInventory == null)
        {
            getAndSendLastInventory(ctx);
        }

        else
        {
            Inventory deviceInventory = DeviceHandler.getDevice().getLastInventory();

            // if the inventory in cache is already the last one: send it
            if(_lastInventory.getCreationDate().getTime() == deviceInventory.getCreationDate().getTime())
            {
                SmartServer.sendMessage(ctx, RequestCode.LAST_INVENTORY, _lastInventory.serialize());
            }

            // else, get the last one and send it
            else
            {
                getAndSendLastInventory(ctx);
            }
        }
    }

    private void getAndSendLastInventory(ChannelHandlerContext ctx)
    {
        _lastInventory = ((InventoryRepository) DbManager.getRepository(InventoryEntity.class)).getLastInventory();

        // if we got an inventory from DB, send it, otherwise, send an empty response
        SmartServer.sendMessage(ctx, RequestCode.LAST_INVENTORY,
                _lastInventory == null
                        ? ""
                        : _lastInventory.serialize());
    }
}
