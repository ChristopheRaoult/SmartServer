package com.spacecode.smartserver.command;

import com.spacecode.sdk.device.data.DeviceStatus;
import com.spacecode.sdk.device.data.Inventory;
import com.spacecode.sdk.network.communication.RequestCode;
import com.spacecode.smartserver.SmartServer;
import com.spacecode.smartserver.database.DbManager;
import com.spacecode.smartserver.database.dao.DaoInventory;
import com.spacecode.smartserver.database.entity.InventoryEntity;
import com.spacecode.smartserver.helper.DeviceHandler;
import io.netty.channel.ChannelHandlerContext;

/**
 * LastInventory command.
 *
 * Provide device's last inventory (serialized).
 */
@CommandContract(deviceRequired = true, responseIfInvalid = "")
public class CmdLastInventory extends ClientCommand
{
    private Inventory _lastInventory = null;

    /**
     * Serialize device's last inventory and send it to current context.
     *
     * @param ctx           Channel between SmartServer and the client.
     * @param parameters    None expected.
     */
    @Override
    public void execute(ChannelHandlerContext ctx, String[] parameters)
    {
        // if the user asked NOT TO save the last inventory in the DB, we have to provide an inventory "from memory":
        // an inventory which has not been saved into the DB, and which has not been selected from the DB with an ID
        if(!DeviceHandler.getRecordInventory())
        {
            // if the device is not scanning, otherwise it means that a new inventory is coming and that
            // "Record Inventory" may have been set to "false" with the current scan operation, not the previous one
            if(DeviceHandler.getDevice().getStatus() != DeviceStatus.SCANNING)
            {
                sendInventory(ctx, DeviceHandler.getDevice().getLastInventory());
                return;
            }
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

            // else, get the last one (from the DB, to also get its ID...) and send it
            else
            {
                getAndSendLastInventory(ctx);
            }
        }
    }

    private void getAndSendLastInventory(ChannelHandlerContext ctx)
    {
        _lastInventory = ((DaoInventory) DbManager.getDao(InventoryEntity.class)).getLastInventory();
        sendInventory(ctx, _lastInventory);
    }

    private void sendInventory(ChannelHandlerContext ctx, Inventory inventory)
    {
        // inventory may be null (for instance if the reference is given by a request from the DAO)
        SmartServer.sendMessage(ctx, RequestCode.LAST_INVENTORY, inventory == null ? "" : inventory.serialize());
    }
}
