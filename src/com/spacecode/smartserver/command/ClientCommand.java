package com.spacecode.smartserver.command;

import io.netty.channel.ChannelHandlerContext;

/**
 * See Command pattern: http://en.wikipedia.org/wiki/Command_pattern
 * To be implemented in order to create new command (executed when a request is received).
 */
public interface ClientCommand
{
    /**
     * To be overridden for each command behavior.
     * @param ctx                       ChannelHandlerContext instance corresponding to the channel existing between SmartServer and the client.
     * @param parameters                String array containing parameters (if any) provided by the client.
     * @throws ClientCommandException   If any error occurs during Command call & execution.
     */
    void execute(ChannelHandlerContext ctx, String[] parameters) throws ClientCommandException;
}
