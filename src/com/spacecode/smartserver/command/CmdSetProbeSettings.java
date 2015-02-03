package com.spacecode.smartserver.command;

import com.spacecode.sdk.device.module.TemperatureProbe;
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
public class CmdSetProbeSettings extends ClientCommand
{
    /**
     * Request to set/update the probe settings for the current device.
     * Return true (if operation succeeded) or false (if failure).
     *
     * @param ctx                       Channel between SmartServer and the client.
     * @param parameters                String array containing parameters (if any) provided by the client.
     *
     * @throws ClientCommandException   Invalid number of parameters received.
     */
    @Override
    public synchronized void execute(ChannelHandlerContext ctx, String[] parameters) throws ClientCommandException
    {
        // waiting for 3 parameters: delay, delta, state (enabled/disabled).
        if(parameters.length != 3)
        {
            SmartServer.sendMessage(ctx, RequestCode.SET_PROBE_SETTINGS, FALSE);
            throw new ClientCommandException("Invalid number of parameters [SetProbeSettings].");
        }

        if(DeviceHandler.getDevice() == null)
        {
            SmartServer.sendMessage(ctx, RequestCode.SET_PROBE_SETTINGS, FALSE);
            return;
        }

        try
        {
            int delay = Integer.parseInt(parameters[0]);
            double delta = Double.parseDouble(parameters[1]);
            boolean state = Boolean.parseBoolean(parameters[2]);

            TemperatureProbe.Settings settings = new TemperatureProbe.Settings(delay, delta, state);

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

