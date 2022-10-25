package com.spacecode.smartserver.command;

import com.spacecode.sdk.device.Device;
import com.spacecode.sdk.network.communication.RequestCode;
import com.spacecode.smartserver.SmartServer;
import com.spacecode.smartserver.database.DbManager;
import com.spacecode.smartserver.database.entity.InventoryEntity;
import com.spacecode.smartserver.database.entity.InventoryRfidTag;
import io.netty.channel.ChannelHandlerContext;

import java.util.ArrayList;
import java.util.List;

/**
 * "TagToDrawer" command.
 *
 * Provide the current "Tag To Axis" map of the {@link Device} instance from Id of the inventory
 */
@CommandContract(paramCount = 1, strictCount = true, deviceRequired = true, responseIfInvalid = "")
public class CmdTagToDrawerById extends ClientCommand
{
    /**
     * Request to get a Tag To drawer  of an Inventory from its ID. Send the serialized list in Packet format.
     * ID's should not be used by end-users but it has been done in SmartTracker, so we need to maintain it...
     *
     * @param ctx           Channel between SmartServer and the client.
     * @param parameters    Inventory ID.
     */
    @Override
    public void execute(ChannelHandlerContext ctx, String[] parameters) {

        String inventoryId = parameters[0];
        try
        {
            int id = Integer.parseInt(inventoryId);
            InventoryEntity invEntity = DbManager.getDao(InventoryEntity.class).getEntityById(id);

            if(invEntity == null)
            {
                SmartServer.sendMessage(ctx, RequestCode.TAG_TO_DRAWER_BY_ID, "");
                return;
            }
            List<String> responsePackets = new ArrayList<>();
            responsePackets.add(RequestCode.TAG_TO_DRAWER_BY_ID);
            for ( InventoryRfidTag tmpRfid : invEntity.getRfidTags())
            {
                responsePackets.add(tmpRfid.getRfidTag().getUid());
                responsePackets.add(Integer.toString(tmpRfid.getShelveNumber()));
            }

            SmartServer.sendMessage(ctx, responsePackets.toArray(new String[responsePackets.size()]));
        } catch(NumberFormatException nfe)
        {
            SmartServer.sendMessage(ctx, RequestCode.TAG_TO_DRAWER_BY_ID, "");
        }
    }
}



