package com.spacecode.smartserver.command;

import com.spacecode.sdk.network.communication.RequestCode;
import com.spacecode.smartserver.SmartServer;
import com.spacecode.smartserver.helper.DeviceHandler;
import com.spacecode.smartserver.helper.SmartLogger;
import io.netty.channel.ChannelHandlerContext;

import java.lang.annotation.Annotation;
import java.net.SocketAddress;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.logging.Level;

/**
 * ClientCommandRegister contains the mapping of Request Codes to Commands.<br/> 
 * This class handles the execution of each command by providing an anti-flood system (a request coming from the same
 * socket with the same code/parameters in a given delay will be refused).<br/>
 * Few "secret" commands are also provided for internal purposes like "TestRFID" integrated into a web app (SmartApp).
 */
public final class ClientCommandRegister extends ClientCommand
{
    // Key:     Command code (RequestCode value).
    // Value:   ClientCommand instance.
    private final Map<String, ClientCommand> _commands = new HashMap<>();

    // delay (ms) allowed between 2 executions of a same request (same request code, param count, and first param)
    static final int DELAY_BETWEEN_EXEC = 500;
    private long _lastExecTimestamp;
    private String[] _lastExecPackets = new String[] { "" };
    private SocketAddress _lastSender = null;

    /**
     * Initialize a command register: Build the map "RequestCode to Command".
     */
    public ClientCommandRegister()
    {
        _commands.put(RequestCode.ADD_ALERT,            new CmdAddAlert());
        _commands.put(RequestCode.ADD_USER,             new CmdAddUser());
        _commands.put(RequestCode.ALERTS_LIST,          new CmdAlertsList());
        _commands.put(RequestCode.ALERT_REPORTS,        new CmdAlertReports());
        _commands.put(RequestCode.AUTHENTICATIONS_LIST, new CmdAuthenticationsList());
        _commands.put(RequestCode.DB_SETTINGS,          new CmdDbSettings());
        _commands.put(RequestCode.DEVICE_STATUS,        new CmdDeviceStatus());
        _commands.put(RequestCode.DISCONNECT,           new CmdDisconnect());
        _commands.put(RequestCode.ENROLL_FINGER,        new CmdEnrollFinger());
        _commands.put(RequestCode.INITIALIZATION,       new CmdInitialization());
        _commands.put(RequestCode.INVENTORIES_LIST,     new CmdInventoriesList());
        _commands.put(RequestCode.INVENTORY_BY_ID,      new CmdInventoryById());
        _commands.put(RequestCode.LAST_ALERT,           new CmdLastAlert());
        _commands.put(RequestCode.LAST_INVENTORY,       new CmdLastInventory());
        _commands.put(RequestCode.PROBE_SETTINGS,       new CmdProbeSettings());
        _commands.put(RequestCode.REMOVE_ALERT,         new CmdRemoveAlert());
        _commands.put(RequestCode.REMOVE_FINGERPRINT,   new CmdRemoveFingerprint());
        _commands.put(RequestCode.REMOVE_USER,          new CmdRemoveUser());
        _commands.put(RequestCode.REWRITE_UID,          new CmdRewriteUid());
        _commands.put(RequestCode.SCAN,                 new CmdScan());
        _commands.put(RequestCode.SET_DB_SETTINGS,      new CmdSetDbSettings());
        _commands.put(RequestCode.SET_LIGHT_INTENSITY,  new CmdSetLightIntensity());
        _commands.put(RequestCode.SET_PROBE_SETTINGS,   new CmdSetProbeSettings());
        _commands.put(RequestCode.SET_SMTP_SERVER,      new CmdSetSmtpServer());
        _commands.put(RequestCode.SET_THIEF_FINGER,     new CmdSetThiefFinger());
        _commands.put(RequestCode.SMTP_SERVER,          new CmdSmtpServer());
        _commands.put(RequestCode.START_LIGHTING,       new CmdStartLighting());
        _commands.put(RequestCode.STOP_LIGHTING,        new CmdStopLighting());
        _commands.put(RequestCode.STOP_SCAN,            new CmdStopScan());
        _commands.put(RequestCode.UPDATE_PERMISSION,    new CmdUpdatePermission());
        _commands.put(RequestCode.UPDATE_ALERT,         new CmdUpdateAlert());
        _commands.put(RequestCode.UPDATE_BADGE,         new CmdUpdateBadge());
        _commands.put(RequestCode.USER_BY_NAME,         new CmdUserByName());
        _commands.put(RequestCode.USERS_LIST,           new CmdUsersList());
        _commands.put(RequestCode.USERS_UNREGISTERED,   new CmdUnregisteredUsers());
        _commands.put(RequestCode.TAG_TO_DRAWER,        new CmdTagToDrawer());
        _commands.put(RequestCode.TAG_TO_DRAWER_BY_ID,  new CmdTagToDrawerById());
        _commands.put(RequestCode.TEMPERATURE_CURRENT,  new CmdTemperatureCurrent());
        _commands.put(RequestCode.TEMPERATURE_LIST,     new CmdTemperatureList());
        _commands.put(RequestCode.START_LIGHTING_ACROSS, new CmdStartLightingAcrossReader());
        _commands.put(RequestCode.STOP_LIGHTING_ACROSS, new CmdStopLightingAcrossReader());

        // RequestCodes not open to SDK/API users: Spacecode's internal usage only
        _commands.put(AppCode.BR_SERIAL,                new ScAdmin.CmdBrSerial());
        _commands.put(AppCode.FLASH_FIRMWARE,           new ScAdmin.CmdFlashFirmware());
        _commands.put(AppCode.HOSTNAME,                 new ScAdmin.CmdHostname());
        _commands.put(AppCode.FPR_SERIAL,               new ScAdmin.CmdFprSerial());
        _commands.put(AppCode.NETWORK_SETTINGS,         new ScAdmin.CmdNetworkSettings());
        _commands.put(AppCode.SIGN_IN_ADMIN,            new ScAdmin.CmdSignInAdmin());
        _commands.put(AppCode.SERIAL_BRIDGE,            new ScAdmin.CmdSerialBridge());
        _commands.put(AppCode.SET_BR_SERIAL,            new ScAdmin.CmdSetBrSerial());
        _commands.put(AppCode.SET_FPR_SERIAL,           new ScAdmin.CmdSetFprSerial());
        _commands.put(AppCode.SET_NETWORK,              new ScAdmin.CmdSetNetworkSettings());
        _commands.put(AppCode.START_UPDATE,             new ScAdmin.CmdStartUpdate());
        _commands.put(AppCode.UPDATE_REPORT,            new ScAdmin.CmdUpdateReport());
        // Requires the User to be authenticated, "TestRFID" part
        _commands.put(AppCode.RFID_AXIS_COUNT,          new ScRfid.CmdRfidAxisCount());
        _commands.put(AppCode.RFID_CALIBRATE,           new ScRfid.CmdRfidCalibrate());
        _commands.put(AppCode.RFID_DEC_FREQUENCY,       new ScRfid.CmdRfidDecFrequency());
        _commands.put(AppCode.RFID_DUTY_CYCLE,          new ScRfid.CmdRfidDutyCycle());
        _commands.put(AppCode.RFID_FREQUENCY,           new ScRfid.CmdRfidFrequency());
        _commands.put(AppCode.RFID_INC_FREQUENCY,       new ScRfid.CmdRfidIncFrequency());
        _commands.put(AppCode.RFID_SAVE_DUTY_CYCLE,     new ScRfid.CmdRfidSaveDutyCycle());
        _commands.put(AppCode.RFID_SELECT_AXIS,         new ScRfid.CmdRfidSelectAxis());
        _commands.put(AppCode.RFID_SET_DOOR_STATE,      new ScRfid.CmdRfidSetDoorState());
        _commands.put(AppCode.RFID_SET_DUTY_CYCLE,      new ScRfid.CmdRfidSetDutyCycle());
        _commands.put(AppCode.RFID_SET_THRESHOLD,       new ScRfid.CmdRfidSetThreshold());
        _commands.put(AppCode.RFID_THRESHOLD,           new ScRfid.CmdRfidThreshold());
        _commands.put(AppCode.RFID_THRESHOLD_SAMPLING,  new ScRfid.CmdRfidThresholdSampling());
    }

    /**
     * Allow dynamic (new) command registering.
     *
     * @param name      Command name. Should correspond to a RequestCode.
     * @param command   Command instance (implementing ClientCommand) which should be executed.
     *
     * @return True if the operation succeeded, false otherwise (invalid command and/or name, or name already in use).
     */
    public boolean addCommand(String name, ClientCommand command)
    {
        if(name == null || command == null || name.trim().isEmpty())
        {
            return false;
        }

        if(_commands.containsKey(name))
        {
            return false;
        }

        _commands.put(name, command);
        return true;
    }

    /**
     * Looks for a ClientCommand corresponding to the given request and execute it with given parameters.
     * First entry of the "parameters" array contains the RequestCode. Others (if any) are extra parameters.
     *
     * @param ctx                       Channel between SmartServer and the client.
     * @param parameters                String array containing parameters (if any) provided by the client.
     *
     * @throws ClientCommandException   Occurs if no command has been found for the given request code.
     */
    @Override
    public void execute(final ChannelHandlerContext ctx, final String[] parameters) throws ClientCommandException
    {
        String requestCode = parameters[0];
        final ClientCommand cmd = _commands.get(requestCode);

        if(cmd == null)
        {
            throw new ClientCommandException("Unknown Command: " + requestCode);
        }

        boolean executeCommand = true;
        long currentTimestamp = System.currentTimeMillis();

        // if the previous request code was the same & came from the same sender
        if(requestCode.equals(_lastExecPackets[0]) && _lastSender == ctx.channel().remoteAddress())
        {
            // if the number of parameters is the same
            if(parameters.length > 0 && _lastExecPackets.length == parameters.length)
            { 
                executeCommand = false;

                // if any packet is different: consider this is not the same request: execute it
                for(int i = 1; i < parameters.length; ++i)
                {
                    if(!parameters[i].equals(_lastExecPackets[i]))
                    {
                        executeCommand = true;
                        break;
                    }
                }
                
                // if the packets are the same BUT the anti-flood delay has passed, execute it 
                if(!executeCommand)
                {
                    executeCommand = currentTimestamp - _lastExecTimestamp > DELAY_BETWEEN_EXEC;    
                }                
            }
        }

        _lastExecTimestamp = currentTimestamp;
        _lastExecPackets = Arrays.copyOfRange(parameters, 0, parameters.length);
        _lastSender = ctx.channel().remoteAddress();

        if(executeCommand)
        {
            // remove the RequestCode and keep the other packets: parameters for the command
            executeOrFail(cmd, ctx, requestCode, Arrays.copyOfRange(parameters, 1, parameters.length));
        }
    }

    /**
     * Check the command CommandContract: 
     * If invalid, the {@link CommandContract#responseIfInvalid()} will be sent back. Otherwise, cmd is executed.
     *
     * @param cmd           Command to be executed.
     * @param ctx           Channel of the client used to send the request.
     * @param requestCode   RequestCode of the request.
     * @param cmdParams     Parameters given with the request.
     */
    public void executeOrFail(ClientCommand cmd, ChannelHandlerContext ctx, String requestCode, String[] cmdParams) 
            throws ClientCommandException
    {
        for(Annotation annotation : cmd.getClass().getAnnotations())
        {
            if(!(annotation instanceof CommandContract))
            {
                continue;
            }

            CommandContract contract = (CommandContract) annotation;

            boolean logException = true;

            try
            {
                // either the number of param is "enough" (not-strict mode), either it needs to be exactly the same 
                if (cmdParams.length < contract.paramCount() ||
                        (contract.strictCount() && cmdParams.length != contract.paramCount()))
                {
                    throw new ClientCommandException("Invalid number of parameters [" + requestCode + "]");
                }

                if(contract.deviceRequired() && !DeviceHandler.isAvailable())
                {
                    logException = false;
                    throw new ClientCommandException("Device not available [" + requestCode + "]");
                }

                if (contract.adminRequired() && !SmartServer.isAdministrator(ctx.channel().remoteAddress()))
                {
                    logException = false;
                    throw new ClientCommandException("User is not an Administrator [" + requestCode + "]");
                }
            } catch(ClientCommandException cce)
            {
                SmartLogger.getLogger().log(logException ? Level.WARNING : Level.INFO,
                        "An error occurred while executing a command [" + requestCode + "]",
                        cce);

                if(!contract.noResponseWhenInvalid())
                {
                    if(contract.respondToAllIfInvalid())
                    {
                        SmartServer.sendAllClients(contract.responseIfInvalid());
                    }
                    
                    else
                    {
                        SmartServer.sendMessage(ctx, requestCode, contract.responseIfInvalid());
                    }
                }

                return;
            }
        }

        cmd.execute(ctx, cmdParams);
    }

    /** Internal RequestCode, used by the SmartApp or the embedded shell scripts. */
    static class AppCode
    {
        /** Get the badge reader serial port name */
        static final String BR_SERIAL = "brserial";
        
        /** Get the fingerprint reader serial number */
        static final String FPR_SERIAL = "fprserial";
        
        /** Flash the Firmware */
        static final String FLASH_FIRMWARE = "flashfirmware";
        
        /** Get the device's Hostname */
        static final String HOSTNAME = "hostname";
        
        /** Get the device's network config */
        static final String NETWORK_SETTINGS = "networksettings";
        
        /** Set the badge reader serial port name */
        static final String SET_BR_SERIAL   = "setbrserial";
        
        /** Set the fingerprint reader serial number */
        static final String SET_FPR_SERIAL  = "setfprserial";
        
        /** Set the network settings */
        static final String SET_NETWORK     = "setnetworksettings";
        
        /** Switch ON/OFF the "Serial Bridge" */
        static final String SERIAL_BRIDGE   = "serialbridge";        
        
        /** Authenticate a user as an administrator */
        static final String SIGN_IN_ADMIN   = "signinadmin";
        
        /** Start the update script */
        static final String START_UPDATE    = "startupdate";
        
        /** Inform SmartServer of the progress of Updates (update script) */
        static final String UPDATE_REPORT   = "updatereport";

        /** Test RFID: Provide the number of axis used (known) by the Device */
        static final String RFID_AXIS_COUNT = "rfidaxiscount";

        /** Test RFID: Get a full "image" (256 values) of the Carrier Signal (users want to know Min/Max/PeakToPeak) */
        static final String RFID_CALIBRATE = "rfidcalibrate";

        /** Test RFID: Decrease the period of the carrier signal (results in decreasing the frequency) */
        static final String RFID_DEC_FREQUENCY = "rfiddecfrequency";

        /** Test RFID: Get the value of the Duty Cycle for both types of bridge, and the current type of bridge. */
        static final String RFID_DUTY_CYCLE = "rfiddutycycle";
        
        /** Test RFID: Get the value of the current carrier period and antenna voltage. */
        static final String RFID_FREQUENCY  = "rfidfrequency";

        /** Test RFID: Increase the period of the carrier signal (results in increasing the frequency) */
        static final String RFID_INC_FREQUENCY = "rfidincfrequency";
        
        /** Test RFID: Save the current Duty Cycle "bridge type" and values in the ROM of the RFID board. */
        static final String RFID_SAVE_DUTY_CYCLE = "rfidsavedutycycle";
        
        /** Test RFID: Select another axis */
        static final String RFID_SELECT_AXIS  = "rfidselectaxis";
        
        /** Test RFID: Set Door State ("true": master door / "false": salve door, "true": open it / "false": close it. */
        static final String RFID_SET_DOOR_STATE  = "rfidsetdoorstate";
                
        /** Test RFID: Set Duty Cycle "bridge type" and values */
        static final String RFID_SET_DUTY_CYCLE  = "rfidsetdutycycle";
        
        /** Test RFID: Set Correlation Threshold */
        static final String RFID_SET_THRESHOLD   = "rfidsetthreshold";

        /** Test RFID: Get Correlation Threshold */
        static final String RFID_THRESHOLD       = "rfidthreshold";

        /** Test RFID: Start the threshold sampling. An Event will be raised to by the RFID board when data is ready. */
        static final String RFID_THRESHOLD_SAMPLING  = "rfidthresholdsampling";
    }
}
