package com.spacecode.smartserver.command.commands;

import com.spacecode.sdk.device.data.RewriteUIDResult;
import com.spacecode.sdk.network.communication.RequestCode;
import com.spacecode.smartserver.DeviceHandler;
import com.spacecode.smartserver.SmartServer;
import com.spacecode.smartserver.command.ClientCommand;
import com.spacecode.smartserver.command.ClientCommandException;
import io.netty.channel.ChannelHandlerContext;

/**
 * RewriteUid command.
 */
public class CommandRewriteUid implements ClientCommand
{
    /**
     * Get the two parameters provided by Client (old Uid, new Uid), try to rewrite it and send the RewriteUIDResult code to the context.
     * @param ctx                       ChannelHandlerContext instance corresponding to the channel existing between SmartServer and the client.
     * @param parameters                String array containing parameters (if any) provided by the client.
     * @throws ClientCommandException
     */
    @Override
    public void execute(ChannelHandlerContext ctx, String[] parameters) throws ClientCommandException
    {
        // waiting for two parameters: old UID and new UID of the tag to be rewritten
        if(parameters.length != 2)
        {
            SmartServer.sendMessage(ctx, RequestCode.REWRITE_UID, String.valueOf(RewriteUIDResult.ERROR));
            return;
        }

        byte result = DeviceHandler.getDevice().rewriteUID(parameters[0], parameters[1]);
        SmartServer.sendMessage(ctx, RequestCode.REWRITE_UID, String.valueOf(result));
    }
}
