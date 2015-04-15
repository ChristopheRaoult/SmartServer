package com.spacecode.smartserver.command;

import com.spacecode.sdk.device.data.RewriteUidResult;
import com.spacecode.sdk.network.communication.RequestCode;
import com.spacecode.smartserver.SmartServer;
import com.spacecode.smartserver.helper.DeviceHandler;
import io.netty.channel.ChannelHandlerContext;

/**
 * RewriteUid command.
 */
public class CmdRewriteUid extends ClientCommand
{
    /**
     * Get the two parameters provided by Client (old Uid, new Uid), try to rewrite it and send the RewriteUIDResult code to the context.
     * @param ctx                       Channel between SmartServer and the client.
     * @param parameters                String array containing parameters (if any) provided by the client.
     * @throws ClientCommandException
     */
    @Override
    public void execute(ChannelHandlerContext ctx, String[] parameters) throws ClientCommandException
    {
        // waiting for two parameters: old UID and new UID of the tag to be rewritten
        if(parameters.length != 2)
        {
            SmartServer.sendMessage(ctx, RequestCode.REWRITE_UID, RewriteUidResult.ERROR.name());
            throw new ClientCommandException("Invalid number of parameters [RewriteUid].");
        }

        if(!DeviceHandler.isAvailable())
        {
            SmartServer.sendMessage(ctx, RequestCode.REWRITE_UID, RewriteUidResult.ERROR.name());
            return;
        }

        RewriteUidResult result = DeviceHandler.getDevice().rewriteUid(parameters[0], parameters[1]);
        SmartServer.sendMessage(ctx, RequestCode.REWRITE_UID, result.name());
    }
}
