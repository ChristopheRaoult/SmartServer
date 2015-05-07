package com.spacecode.smartserver.command;

import com.spacecode.smartserver.SmartServer;
import com.spacecode.smartserver.helper.DeviceHandler;
import com.spacecode.smartserver.helper.SmartLogger;
import io.netty.channel.ChannelHandlerContext;

import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;

/** Class "container" for all ClientCommands dedicated to the "TestRFID" part of the SmartApp. */
class ScRfid
{
    /**
     * [ADMIN] Command AxisCount (number of axis known/used by the device).
     */
    @CommandContract(deviceRequired = true, adminRequired = true, responseWhenInvalid = "-1")
    static class CmdRfidAxisCount extends ClientCommand
    {
        /**
         * Send the device's board's number of axis in use.
         *
         * @param ctx           Channel between SmartServer and the client.
         * @param parameters    None expected.
         */
        @Override
        public void execute(ChannelHandlerContext ctx, String[] parameters)
        {
            Object queryAxisCount = DeviceHandler.getDevice().adminQuery("axis_count");

            if(!(queryAxisCount instanceof Byte))
            {
                SmartServer.sendMessage(ctx, ClientCommandRegister.AppCode.RFID_AXIS_COUNT, "-1");
                return;
            }

            byte axisCount = (byte) queryAxisCount;

            SmartServer.sendMessage(ctx, ClientCommandRegister.AppCode.RFID_AXIS_COUNT,
                    String.valueOf(axisCount == 0 ? 1 : axisCount));
        }
    }

    /**
     * [ADMIN] Command Calibrate (get and send 256 values of the "full image" of the carrier signal).
     */
    @CommandContract(deviceRequired = true, adminRequired = true, responseWhenInvalid = "")
    static class CmdRfidCalibrate extends ClientCommand
    {
        /**
         * Send the device's board's "full image" (256 values) of the carrier signal.
         *
         * @param ctx           Channel between SmartServer and the client.
         * @param parameters    None expected.
         */
        @Override
        public void execute(ChannelHandlerContext ctx, String[] parameters)
        {
            Object queryCalibrationValues = DeviceHandler.getDevice().adminQuery("calibration");

            if(!(queryCalibrationValues instanceof byte[]))
            {
                SmartServer.sendMessage(ctx, ClientCommandRegister.AppCode.RFID_CALIBRATE);
                return;
            }

            List<String> packets = new ArrayList<>();
            packets.add(ClientCommandRegister.AppCode.RFID_CALIBRATE);
            
            for(byte value : (byte[]) queryCalibrationValues)
            {
                packets.add(String.valueOf(value));
            }
            
            SmartServer.sendMessage(ctx, packets.toArray(new String[packets.size()]));
        }
    }
    
    /**
     * [ADMIN] Command DecFrequency (allow decreasing the period of the carrier signal).
     */
    @CommandContract(deviceRequired = true, adminRequired = true)
    static class CmdRfidDecFrequency extends ClientCommand
    {
        /**
         * Send true if the correlation threshold was updated, false otherwise.
         *
         * @param ctx           Channel between SmartServer and the client.
         * @param parameters    None expected.
         */
        @Override
        public void execute(ChannelHandlerContext ctx, String[] parameters)
        {
            Object queryFrequency = DeviceHandler.getDevice().adminQuery("decrease_frequency");

            if(!(queryFrequency instanceof Boolean))
            {
                SmartServer.sendMessage(ctx, ClientCommandRegister.AppCode.RFID_DEC_FREQUENCY, FALSE);
                return;
            }

            SmartServer.sendMessage(ctx,
                    ClientCommandRegister.AppCode.RFID_DEC_FREQUENCY, (boolean) queryFrequency ? TRUE : FALSE);
        }
    }

    /**
     * [ADMIN] Command DutyCycle ("bridge type" and duty cycle values, in the RFID board memory).
     */
    @CommandContract(deviceRequired = true, adminRequired = true, responseWhenInvalid = "")
    static class CmdRfidDutyCycle extends ClientCommand
    {
        /**
         * Send the device's board's duty cycle "bridge type" (half/full) and values (for both types).
         *
         * @param ctx           Channel between SmartServer and the client.
         * @param parameters    None expected.
         */
        @Override
        public void execute(ChannelHandlerContext ctx, String[] parameters)
        {
            Object queryDcu = DeviceHandler.getDevice().adminQuery("duty_cycle");

            if(!(queryDcu instanceof short[]))
            {
                SmartServer.sendMessage(ctx, ClientCommandRegister.AppCode.RFID_DUTY_CYCLE);
                return;
            }

            short[] dcuInfo = (short[]) queryDcu;

            if(dcuInfo.length < 3)
            {
                SmartServer.sendMessage(ctx, ClientCommandRegister.AppCode.RFID_DUTY_CYCLE);
                return;
            }

            SmartServer.sendMessage(ctx,
                    ClientCommandRegister.AppCode.RFID_DUTY_CYCLE, String.valueOf(dcuInfo[0]), String.valueOf(dcuInfo[1]),
                    String.valueOf(dcuInfo[2]));
        }
    }

    /**
     * [ADMIN] Command Frequency (Carrier Period and Antenna Voltage).
     */
    @CommandContract(deviceRequired = true, adminRequired = true, responseWhenInvalid = "")
    static class CmdRfidFrequency extends ClientCommand
    {
        /**
         * Send the device's board's Carrier Period and Antenna Voltage
         *
         * @param ctx           Channel between SmartServer and the client.
         * @param parameters    None expected.
         */
        @Override
        public void execute(ChannelHandlerContext ctx, String[] parameters)
        {
            Object queryCarrier = DeviceHandler.getDevice().adminQuery("carrier_period");

            if(!(queryCarrier instanceof int[]))
            {
                SmartServer.sendMessage(ctx, ClientCommandRegister.AppCode.RFID_FREQUENCY);
                return;
            }

            int[] carrierInfo = (int[]) queryCarrier;

            if(carrierInfo.length < 2)
            {
                SmartServer.sendMessage(ctx, ClientCommandRegister.AppCode.RFID_FREQUENCY);
                return;
            }

            SmartServer.sendMessage(ctx,
                    ClientCommandRegister.AppCode.RFID_FREQUENCY, String.valueOf(carrierInfo[0]),
                    String.valueOf(carrierInfo[1]));
        }
    }

    /**
     * [ADMIN] Command IncFrequency (allow increasing the period of the carrier signal).
     */
    @CommandContract(deviceRequired = true, adminRequired = true)
    static class CmdRfidIncFrequency extends ClientCommand
    {
        /**
         * Send true if the correlation threshold was updated, false otherwise.
         *
         * @param ctx           Channel between SmartServer and the client.
         * @param parameters    None expected.
         */
        @Override
        public void execute(ChannelHandlerContext ctx, String[] parameters)
        {
            Object queryFrequency = DeviceHandler.getDevice().adminQuery("increase_frequency");

            if(!(queryFrequency instanceof Boolean))
            {
                SmartServer.sendMessage(ctx, ClientCommandRegister.AppCode.RFID_INC_FREQUENCY, FALSE);
                return;
            }

            SmartServer.sendMessage(ctx,
                    ClientCommandRegister.AppCode.RFID_INC_FREQUENCY, (boolean) queryFrequency ? TRUE : FALSE);
        }
    }
    
    /**
     * [ADMIN] Command SaveDutyCycle ("bridge type" and duty cycle values, in the RFID board memory).
     */
    @CommandContract(deviceRequired = true, adminRequired = true)
    static class CmdRfidSaveDutyCycle extends ClientCommand
    {
        /**
         * Send TRUE if the current settings could be applied, FALSE otherwise.
         *
         * @param ctx           Channel between SmartServer and the client.
         * @param parameters    None expected.
         */
        @Override
        public void execute(ChannelHandlerContext ctx, String[] parameters) throws ClientCommandException
        {
            Object querySaveDcu = DeviceHandler.getDevice().adminQuery("save_duty_cycle");

            if(!(querySaveDcu instanceof Boolean))
            {
                SmartServer.sendMessage(ctx, ClientCommandRegister.AppCode.RFID_SAVE_DUTY_CYCLE, FALSE);
                return;
            }

            SmartServer.sendMessage(ctx,
                    ClientCommandRegister.AppCode.RFID_SAVE_DUTY_CYCLE, (boolean) querySaveDcu ? TRUE : FALSE);
        }
    }

    /** [ADMIN] Command SelectAxis (ask the RFID board to Switch Axis) */
    @CommandContract(paramCount = 1, strictCount = true, deviceRequired = true, adminRequired = true)
    static class CmdRfidSelectAxis extends ClientCommand
    {
        /**
         * Send true if the "Switch Axis" order was sent to the board, false otherwise.
         *
         * @param ctx           Channel between SmartServer and the client.
         * @param parameters    Selected Axis.
         */
        @Override
        public void execute(ChannelHandlerContext ctx, String[] parameters)
        {
            byte axis;

            try
            {
                axis = Byte.parseByte(parameters[0]);

                if(axis < 1 || axis > 126)
                {
                    throw new NumberFormatException("Axis value out of allowed range [0;126]");
                }
            } catch(NumberFormatException nfe)
            {
                SmartLogger.getLogger().log(Level.WARNING, "Invalid value provided for the axis", nfe);
                SmartServer.sendMessage(ctx, ClientCommandRegister.AppCode.RFID_SELECT_AXIS, FALSE);
                return;
            }

            Object querySelectAxis = DeviceHandler.getDevice().adminQuery("select_axis", (byte) axis);

            if(!(querySelectAxis instanceof Boolean))
            {
                SmartServer.sendMessage(ctx, ClientCommandRegister.AppCode.RFID_SELECT_AXIS, FALSE);
                return;
            }

            SmartServer.sendMessage(ctx, ClientCommandRegister.AppCode.RFID_SELECT_AXIS, 
                    (boolean) querySelectAxis ? TRUE : FALSE);
        }
    }
    
    /**
     * [ADMIN] Command SetDoorState (Allows opening/closing the master or the slave door(s)).
     */
    @CommandContract(paramCount = 2, strictCount = true, deviceRequired = true, adminRequired = true)
    static class CmdRfidSetDoorState extends ClientCommand
    {
        /**
         * Send true if the order was successfully sent to the RFID board. False otherwise.
         *
         * @param ctx           Channel between SmartServer and the client.
         * @param parameters    Master (true) or Slave (false) door, and State (true: open, false: closed).
         */
        @Override
        public void execute(ChannelHandlerContext ctx, String[] parameters)
        {
            boolean isMaster = Boolean.parseBoolean(parameters[0]);
            boolean state = Boolean.parseBoolean(parameters[1]);

            Object queryDoor = DeviceHandler.getDevice().adminQuery("set_door_state", isMaster, state);

            if(!(queryDoor instanceof Boolean))
            {
                SmartServer.sendMessage(ctx, ClientCommandRegister.AppCode.RFID_SET_DOOR_STATE, FALSE);
                return;
            }

            SmartServer.sendMessage(ctx,
                    ClientCommandRegister.AppCode.RFID_SET_DOOR_STATE, (boolean) queryDoor ? TRUE : FALSE);
        }
    }

    /**
     * [ADMIN] Command SetDutyCycle ("bridge type" and duty cycle values, in the RFID board memory).
     */
    @CommandContract(paramCount = 3, strictCount = true, deviceRequired = true, adminRequired = true)
    static class CmdRfidSetDutyCycle extends ClientCommand
    {
        /**
         * Send true if the duty cycle was updated (values and bridge type), false otherwise.
         *
         * @param ctx           Channel between SmartServer and the client.
         * @param parameters    Bridge type (full / half), and a numeric value for both bridge types.
         */
        @Override
        public void execute(ChannelHandlerContext ctx, String[] parameters)
        {
            short bridgeType, dcuFull, dcuHalf;

            try
            {
                bridgeType = Short.parseShort(parameters[0]);
                dcuFull = Short.parseShort(parameters[1]);
                dcuHalf = Short.parseShort(parameters[2]);

                if(bridgeType < 0 || bridgeType > 1)
                {
                    throw new NumberFormatException("Invalid bridge type");
                }

                if(dcuFull < 0 || dcuFull > 167)
                {
                    throw new NumberFormatException("Duty Cycle for Full Bridge out of range [0;167]");
                }
                if(dcuHalf < 0 || dcuHalf> 167)
                {
                    throw new NumberFormatException("Duty Cycle for Full Bridge out of range [0;167]");
                }
            } catch(NumberFormatException nfe)
            {
                SmartLogger.getLogger().log(Level.WARNING, "Invalid value provided for the duty cycle...", nfe);
                SmartServer.sendMessage(ctx, ClientCommandRegister.AppCode.RFID_SET_DUTY_CYCLE, FALSE);
                return;
            }

            Object querySetDcu = DeviceHandler.getDevice().adminQuery("set_duty_cycle", bridgeType, dcuFull, dcuHalf);

            if(!(querySetDcu instanceof Boolean))
            {
                SmartServer.sendMessage(ctx, ClientCommandRegister.AppCode.RFID_SET_DUTY_CYCLE, FALSE);
                return;
            }

            SmartServer.sendMessage(ctx,
                    ClientCommandRegister.AppCode.RFID_SET_DUTY_CYCLE, (boolean) querySetDcu ? TRUE : FALSE);
        }
    }
    
    /**
     * [ADMIN] Command SetThreshold (correlation threshold of the RFID board).
     */
    @CommandContract(paramCount = 1, strictCount = true, deviceRequired = true, adminRequired = true)
    static class CmdRfidSetThreshold extends ClientCommand
    {
        /**
         * Send true if the correlation threshold was updated, false otherwise.
         *
         * @param ctx           Channel between SmartServer and the client.
         * @param parameters    New Threshold.
         */
        @Override
        public void execute(ChannelHandlerContext ctx, String[] parameters)
        {
            short threshold;

            try
            {
                threshold = Short.parseShort(parameters[0]);

                if(threshold < 3 || threshold > 250)
                {
                    throw new NumberFormatException("Threshold value out of allowed range [3;250]");
                }
            } catch(NumberFormatException nfe)
            {
                SmartLogger.getLogger().log(Level.WARNING, "Invalid value provided for the threshold", nfe);
                SmartServer.sendMessage(ctx, ClientCommandRegister.AppCode.RFID_SET_THRESHOLD, FALSE);
                return;
            }

            Object queryThreshold = DeviceHandler.getDevice().adminQuery("set_threshold", threshold);

            if(!(queryThreshold instanceof Boolean))
            {
                SmartServer.sendMessage(ctx, ClientCommandRegister.AppCode.RFID_SET_THRESHOLD, FALSE);
                return;
            }

            SmartServer.sendMessage(ctx,
                    ClientCommandRegister.AppCode.RFID_SET_THRESHOLD,
                    (boolean) queryThreshold ? TRUE : FALSE);
        }
    }

    /**
     * [ADMIN] Command Threshold (correlation threshold of the RFID board).
     */
    @CommandContract(deviceRequired = true, adminRequired = true)
    static class CmdRfidThreshold extends ClientCommand
    {
        /**
         * Send the device's board's correlation threshold (or "-1" in case of error).
         *
         * @param ctx           Channel between SmartServer and the client.
         * @param parameters    None expected.
         */
        @Override
        public void execute(ChannelHandlerContext ctx, String[] parameters)
        {
            Object queryThreshold = DeviceHandler.getDevice().adminQuery("threshold");

            if(!(queryThreshold instanceof Integer) && !(queryThreshold instanceof Short))
            {
                SmartServer.sendMessage(ctx, ClientCommandRegister.AppCode.RFID_THRESHOLD, "-1");
                return;
            }

            SmartServer.sendMessage(ctx,
                    ClientCommandRegister.AppCode.RFID_THRESHOLD,
                    String.valueOf((short) queryThreshold));
        }
    }

    /**
     * [ADMIN] Command ThresholdSampling (correlation measures of the RFID board).
     */
    @CommandContract(paramCount = 2, strictCount = true, deviceRequired = true, adminRequired = true)
    static class CmdRfidThresholdSampling extends ClientCommand
    {
        private static short[] _presentSamples = new short[256];
        private static short[] _missingSamples = new short[256];

        /**
         * Send the device's board's correlation threshold (or "-1" in case of error).
         *
         * @param ctx           Channel between SmartServer and the client.
         * @param parameters    Samples count, Mode (cycling or refreshing).
         */
        @Override
        public void execute(ChannelHandlerContext ctx, String[] parameters)
        {
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
                    ClientCommandRegister.AppCode.RFID_THRESHOLD_SAMPLING, (boolean) queryThreshold ? TRUE : FALSE);
        }
    }
}
