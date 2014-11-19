package com.spacecode.smartserver.command;

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
        // TODO: Implement me
    }
}
