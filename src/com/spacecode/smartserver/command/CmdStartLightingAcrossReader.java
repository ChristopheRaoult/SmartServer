package com.spacecode.smartserver.command;

/**
 * Created by admin on 27/08/2016.
 */

import com.spacecode.sdk.device.data.DeviceStatus;
import com.spacecode.sdk.network.communication.RequestCode;
import com.spacecode.smartserver.SmartServer;
import com.spacecode.smartserver.helper.DeviceHandler;
import io.netty.channel.ChannelHandlerContext;

/**
 * StartLighting command.
 */
@CommandContract(deviceRequired = true)
public class CmdStartLightingAcrossReader extends ClientCommand {
    @Override
    public void execute(ChannelHandlerContext ctx, String[] parameters) throws ClientCommandException {

        if(DeviceHandler.getDevice().getStatus() != DeviceStatus.READY)
        {
            SmartServer.sendMessage(ctx, RequestCode.START_LIGHTING_ACROSS, FALSE);
            return;
        }

        // create a new ArrayList from given tags (in parameters)
        boolean result = DeviceHandler.getDevice().startContinuousLightingAcross();
        SmartServer.sendMessage(ctx, RequestCode.START_LIGHTING_ACROSS, result ? TRUE : FALSE);

    }
}
