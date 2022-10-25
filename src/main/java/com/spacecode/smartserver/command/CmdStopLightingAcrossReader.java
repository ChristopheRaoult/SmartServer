package com.spacecode.smartserver.command;

import com.spacecode.sdk.network.communication.RequestCode;
import com.spacecode.smartserver.SmartServer;
import com.spacecode.smartserver.helper.DeviceHandler;
import io.netty.channel.ChannelHandlerContext;

/**
 * Created by admin on 27/08/2016.
 */
@CommandContract(deviceRequired = true)
public class CmdStopLightingAcrossReader extends ClientCommand {
    @Override
    public void execute(ChannelHandlerContext ctx, String[] parameters) throws ClientCommandException {
        boolean result1 = DeviceHandler.getDevice().stopLightingTagsLed();
        boolean result2 = DeviceHandler.getDevice().stopContinuosLightingAcross();
        SmartServer.sendMessage(ctx, RequestCode.STOP_LIGHTING_ACROSS, result1&&result2 ? TRUE : FALSE);
    }
}
