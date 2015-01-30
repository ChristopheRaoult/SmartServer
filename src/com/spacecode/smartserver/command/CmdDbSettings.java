package com.spacecode.smartserver.command;

import com.spacecode.sdk.network.communication.RequestCode;
import com.spacecode.smartserver.SmartServer;
import com.spacecode.smartserver.helper.ConfManager;
import io.netty.channel.ChannelHandlerContext;

/**
 * DbSettings command.
 */
public class CmdDbSettings extends ClientCommand
{
    /**
     * Send Database settings for the current device.
     *
     * @param ctx                       Channel between SmartServer and the client.
     * @param parameters                String array containing parameters (if any) provided by the client.
     *
     * @throws ClientCommandException
     */
    @Override
    public void execute(ChannelHandlerContext ctx, String[] parameters) throws ClientCommandException
    {
        SmartServer.sendMessage(ctx, RequestCode.DB_SETTINGS, ConfManager.getDbHost(), ConfManager.getDbPort(),
                ConfManager.getDbName(), ConfManager.getDbUser(), ConfManager.getDbDbms());
    }
}
