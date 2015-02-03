package com.spacecode.smartserver.command;

import com.spacecode.sdk.network.communication.RequestCode;
import com.spacecode.smartserver.SmartServer;
import com.spacecode.smartserver.helper.ConfManager;
import io.netty.channel.ChannelHandlerContext;

/**
 * Command ProbeSettings.
 */
public class CmdProbeSettings extends ClientCommand
{
    /**
     * Send Probe settings for the current device.
     *
     * @param ctx                       Channel between SmartServer and the client.
     * @param parameters                String array containing parameters (if any) provided by the client.
     *
     * @throws ClientCommandException
     */
    @Override
    public void execute(ChannelHandlerContext ctx, String[] parameters) throws ClientCommandException
    {
        SmartServer.sendMessage(ctx, RequestCode.PROBE_SETTINGS,
                String.valueOf(ConfManager.getDevTemperatureDelay()),
                String.valueOf(ConfManager.getDevTemperatureDelta()),
                String.valueOf(ConfManager.isDevTemperature())
        );
    }
}
