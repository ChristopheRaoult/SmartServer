package com.spacecode.smartserver.command;

import com.spacecode.sdk.device.module.data.ProbeSettings;
import com.spacecode.sdk.network.communication.RequestCode;
import com.spacecode.smartserver.SmartServer;
import com.spacecode.smartserver.helper.ConfManager;
import com.spacecode.smartserver.helper.DeviceHandler;
import com.spacecode.smartserver.helper.SmartLogger;
import io.netty.channel.ChannelHandlerContext;

import java.util.logging.Level;

/**
 * Command SetProbeSettings.
 */
@CommandContract(paramCount = 3, strictCount = true, deviceRequired = true)
public class CmdSetProbeSettings extends ClientCommand
{
    /**
     * Request to set/update the probe settings for the current device.
     * Return true (if operation succeeded) or false (if failure).
     *
     * @param ctx                       Channel between SmartServer and the client.
     * @param parameters                Delay, Delta, Enabled.
     */
    @Override
    public synchronized void execute(ChannelHandlerContext ctx, String[] parameters)
    {
        try
        {
            int delay = Integer.parseInt(parameters[0]);
            double delta = Double.parseDouble(parameters[1]);
            boolean state = Boolean.parseBoolean(parameters[2]);

            ProbeSettings settings = new ProbeSettings(delay, delta, state);

            if(!ConfManager.setProbeConfiguration(settings))
            {
                SmartServer.sendMessage(ctx, RequestCode.SET_PROBE_SETTINGS, FALSE);
                return;
            }

            SmartServer.sendMessage(ctx, RequestCode.SET_PROBE_SETTINGS, TRUE);

            SmartLogger.getLogger().info("Probe Settings have changed... Applying changes...");
            DeviceHandler.reloadTemperatureProbe();
            return;
        } catch(NumberFormatException nfe)
        {
            SmartLogger.getLogger().log(Level.SEVERE, "Delay or Delta invalid, unable to set Probe Settings.", nfe);
        }

        SmartServer.sendMessage(ctx, RequestCode.SET_PROBE_SETTINGS, FALSE);
    }
}

