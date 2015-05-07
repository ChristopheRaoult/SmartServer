package com.spacecode.smartserver.command;

import com.spacecode.sdk.device.data.RewriteUidResult;
import com.spacecode.sdk.network.communication.RequestCode;
import com.spacecode.smartserver.SmartServer;
import com.spacecode.smartserver.helper.DeviceHandler;
import io.netty.channel.ChannelHandlerContext;

/**
 * RewriteUid command.
 * "ERROR" (RewriteUidResult.ERROR.name()) is sent back if the contract is not respected.
 */
@CommandContract(paramCount = 2, strictCount = true, deviceRequired = true, responseWhenInvalid = "ERROR")
public class CmdRewriteUid extends ClientCommand
{
    /**
     * Get the two parameters provided by Client (old Uid, new Uid), try to rewrite it and send the RewriteUIDResult code to the context.
     * 
     * @param ctx           Channel between SmartServer and the client.
     * @param parameters    UID of the Tag to be rewritten, New UID.
     */
    @Override
    public void execute(ChannelHandlerContext ctx, String[] parameters)
    {
        RewriteUidResult result = DeviceHandler.getDevice().rewriteUid(parameters[0], parameters[1]);
        SmartServer.sendMessage(ctx, RequestCode.REWRITE_UID, result.name());
    }
}
