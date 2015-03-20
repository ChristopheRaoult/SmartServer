package com.spacecode.smartserver.command;

import com.spacecode.smartserver.SmartServer;
import com.spacecode.smartserver.helper.DeviceHandler;
import com.spacecode.smartserver.helper.SmartLogger;
import io.netty.channel.ChannelHandlerContext;

import java.util.logging.Level;

/**
 * [ADMIN] Command ThresholdSampling (correlation measures of the RFID board).
 */
public class CmdRfidThresholdSampling extends ClientCommand
{
    private static short[] _presentSamples = new short[256];
    private static short[] _missingSamples = new short[256];
    
    /**
     * Send the device's board's correlation threshold (or "-1" in case of error).
     *
     * @param ctx                       Channel between SmartServer and the client.
     * @param parameters                String array containing parameters (if any) provided by the client.
     *
     * @throws ClientCommandException
     */
    @Override
    public void execute(ChannelHandlerContext ctx, String[] parameters) throws ClientCommandException
    {
        // waiting for 2 parameter: the samples count and the mode (cycling or refreshing)
        if(parameters.length != 2)
        {
            SmartServer.sendMessage(ctx, ClientCommandRegister.AppCode.RFID_THRESHOLD_SAMPLING, FALSE);
            throw new ClientCommandException("Invalid number of parameters [ThresholdSampling].");
        }
        
        if(!SmartServer.isAdministrator(ctx.channel().remoteAddress()))
        {
            SmartServer.sendMessage(ctx, ClientCommandRegister.AppCode.RFID_THRESHOLD_SAMPLING, FALSE);
            return;
        }

        if(DeviceHandler.getDevice() == null)
        {
            SmartServer.sendMessage(ctx, ClientCommandRegister.AppCode.RFID_THRESHOLD_SAMPLING, FALSE);
            return;
        }

        int samplesCount;
        boolean cycling = "true".equals(parameters[1]);
        
        try
        {
            samplesCount = Integer.parseInt(parameters[0]);

            if(samplesCount < 3 || samplesCount > 120)
            {
                throw new NumberFormatException("Samples count out of allowed range [3;120]");
            }
        } catch(NumberFormatException nfe)
        {
            SmartLogger.getLogger().log(Level.WARNING, "Invalid value provided for the sample count", nfe);
            SmartServer.sendMessage(ctx, ClientCommandRegister.AppCode.RFID_THRESHOLD_SAMPLING, FALSE);
            return;
        }
        
        if(cycling)
        {
            // do not keep old values
            _presentSamples = new short[256];
            _missingSamples = new short[256];
        }
        
        Object queryThreshold = DeviceHandler.getDevice().adminQuery("threshold_sampling", 
                samplesCount, _missingSamples, _presentSamples);

        if(!(queryThreshold instanceof Boolean))
        {
            SmartServer.sendMessage(ctx, ClientCommandRegister.AppCode.RFID_THRESHOLD_SAMPLING, FALSE);
            return;
        }

        SmartServer.sendMessage(ctx,
                ClientCommandRegister.AppCode.RFID_THRESHOLD_SAMPLING,
                (boolean) queryThreshold ? TRUE : FALSE);
    }
}
