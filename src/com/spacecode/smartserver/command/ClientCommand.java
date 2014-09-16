package com.spacecode.smartserver.command;

import io.netty.channel.ChannelHandlerContext;

import java.util.concurrent.Executors;

/**
 * See Command pattern: http://en.wikipedia.org/wiki/Command_pattern
 * To be implemented in order to create new command (executed when a request is received).
 */
abstract class ClientCommand
{
    protected static String TRUE = "true";
    protected static String FALSE = "false";

    /**
     * To be overridden for each command behavior.
     *
     * @param ctx                       ChannelHandlerContext instance corresponding to the channel existing between SmartServer and the client.
     * @param parameters                String array containing parameters (if any) provided by the client.
     *
     * @throws ClientCommandException   If any error occurs during Command call & execution.
     */
    public abstract void execute(ChannelHandlerContext ctx, String[] parameters) throws ClientCommandException;

    /**
     * Allow paralleling an action when running a command (the method is protected, then inherited).
     * Simply prevent from instantiating/using a custom thread-pool etc.
     *
     * @param runnable  Runnable instance of the task to be paralleled.
     */
    protected static void parallelize(Runnable runnable)
    {
        Executors.newSingleThreadExecutor().submit(runnable);
    }
}
