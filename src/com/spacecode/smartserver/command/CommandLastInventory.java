package com.spacecode.smartserver.command;

import com.spacecode.sdk.network.communication.RequestCode;
import com.spacecode.smartserver.DeviceHandler;
import com.spacecode.smartserver.SmartServer;
import io.netty.channel.ChannelHandlerContext;

/**
 * LastInventory command.
 * Provide device's last inventory (serialized).
 */
public class CommandLastInventory extends ClientCommand
{
    /**
     * Serialize device's last inventory and send it to current context.
     * @param ctx                       ChannelHandlerContext instance corresponding to the channel existing between SmartServer and the client.
     * @param parameters                String array containing parameters (if any) provided by the client.
     * @throws ClientCommandException
     */
    @Override
    public void execute(ChannelHandlerContext ctx, String[] parameters) throws ClientCommandException
    {
        SmartServer.sendMessage(ctx, RequestCode.LAST_INVENTORY, DeviceHandler.getDevice().getLastInventory().serialize());
    }
}
