package com.spacecode.smartserver.command;

import com.spacecode.sdk.network.communication.RequestCode;
import com.spacecode.smartserver.SmartServer;
import com.spacecode.smartserver.helper.DeviceHandler;
import io.netty.channel.ChannelHandlerContext;

/**
 * LastInventory command.
 * Provide device's last inventory (serialized).
 */
public class CommandLastInventory extends ClientCommand
{
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

        SmartServer.sendMessage(ctx, RequestCode.LAST_INVENTORY, DeviceHandler.getDevice().getLastInventory().serialize());
    }
}
