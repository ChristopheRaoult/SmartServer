package com.spacecode.smartserver.command;

import com.spacecode.sdk.network.communication.RequestCode;
import io.netty.channel.ChannelHandlerContext;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

/**
 * ClientCommandRegister class allows SmartServerHandler to execute any appropriate Command object for a given request string.
 * Request string (sent by the Client) is passed as a String array to execute() method with the ChannelHandlerContext. If any corresponding command is found,
 * ClientCommandRegister instance executes execute() method from this command. Other parameters (if any) of the string array are passed with the ChannelHandlerContext.
 */
public final class ClientCommandRegister extends ClientCommand
{
    // Key:     Command code (RequestCode value).
    // Value:   ClientCommand instance.
    private Map<String, ClientCommand> _commands = new HashMap<>();

    // delay (ms) allowed between 2 executions of a same request (same request code, param count, and first param)
    private static final int DELAY_BETWEEN_EXEC = 500;
    private long _lastExecTimestamp;
    private String _lastExecRequestCode;
    private int _lastExecParamCount;
    private String _lastExecFirstParam;

    /**
     * Initialize command register.
     * Add an entry to "_commands" HashMap with request code & command instance for each known request.
     */
    public ClientCommandRegister()
    {
        _commands.put(RequestCode.ADD_ALERT,            new CmdAddAlert());
        _commands.put(RequestCode.ADD_USER,             new CmdAddUser());
        _commands.put(RequestCode.ALERTS_LIST,          new CmdAlertsList());
        _commands.put(RequestCode.ALERT_REPORTS,        new CmdAlertReports());
        _commands.put(RequestCode.AUTHENTICATIONS_LIST, new CmdAuthenticationsList());
        _commands.put(RequestCode.BR_SERIAL,            new CmdBrSerial());
        _commands.put(RequestCode.DB_SETTINGS,          new CmdDbSettings());
        _commands.put(RequestCode.DEVICE_STATUS,        new CmdDeviceStatus());
        _commands.put(RequestCode.DISCONNECT,           new CmdDisconnect());
        _commands.put(RequestCode.ENROLL_FINGER,        new CmdEnrollFinger());
        _commands.put(RequestCode.FLASH_FIRMWARE,       new CmdFlashFirmware());
        _commands.put(RequestCode.FPR_SERIAL,           new CmdFprSerial());
        _commands.put(RequestCode.INITIALIZATION,       new CmdInitialization());
        _commands.put(RequestCode.INVENTORIES_LIST,     new CmdInventoriesList());
        _commands.put(RequestCode.LAST_ALERT,           new CmdLastAlert());
        _commands.put(RequestCode.LAST_INVENTORY,       new CmdLastInventory());
        _commands.put(RequestCode.PROBE_SETTINGS,       new CmdProbeSettings());
        _commands.put(RequestCode.REMOVE_ALERT,         new CmdRemoveAlert());
        _commands.put(RequestCode.REMOVE_FINGERPRINT,   new CmdRemoveFingerprint());
        _commands.put(RequestCode.REMOVE_USER,          new CmdRemoveUser());
        _commands.put(RequestCode.REWRITE_UID,          new CmdRewriteUid());
        _commands.put(RequestCode.SCAN,                 new CmdScan());
        _commands.put(RequestCode.SET_BR_SERIAL,        new CmdSetBrSerial());
        _commands.put(RequestCode.SET_DB_SETTINGS,      new CmdSetDbSettings());
        _commands.put(RequestCode.SET_FPR_SERIAL,       new CmdSetFprSerial());
        _commands.put(RequestCode.SET_PROBE_SETTINGS,   new CmdSetProbeSettings());
        _commands.put(RequestCode.SET_SMTP_SERVER,      new CmdSetSmtpServer());
        _commands.put(RequestCode.SET_THIEF_FINGER,     new CmdSetThiefFinger());
        _commands.put(RequestCode.SERIAL_BRIDGE,        new CmdSerialBridge());
        _commands.put(RequestCode.SMTP_SERVER,          new CmdSmtpServer());
        _commands.put(RequestCode.START_LIGHTING,       new CmdStartLighting());
        _commands.put(RequestCode.STOP_LIGHTING,        new CmdStopLighting());
        _commands.put(RequestCode.STOP_SCAN,            new CmdStopScan());
        _commands.put(RequestCode.UPDATE_PERMISSION,    new CmdUpdatePermission());
        _commands.put(RequestCode.UPDATE_ALERT,         new CmdUpdateAlert());
        _commands.put(RequestCode.UPDATE_BADGE,         new CmdUpdateBadge());
        _commands.put(RequestCode.USER_BY_NAME,         new CmdUserByName());
        _commands.put(RequestCode.USERS_LIST,           new CmdUsersList());
        _commands.put(RequestCode.TEMPERATURE_CURRENT,  new CmdTemperatureCurrent());
        _commands.put(RequestCode.TEMPERATURE_LIST,     new CmdTemperatureList());
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
        int paramCount = parameters.length - 1;

        if(requestCode.equals(_lastExecRequestCode))
        {
            if(paramCount > 0 && _lastExecParamCount == paramCount)
            {
                if(parameters[1].equals(_lastExecFirstParam))
                {
                    if(currentTimestamp - _lastExecTimestamp < DELAY_BETWEEN_EXEC)
                    {
                        // if the last cmd executed had the same request code & first param,
                        // and was executed less than DELAY_BETWEEN_EXEC milliseconds, then skip the request
                        executeCommand = false;
                    }
                }

            }
        }

        _lastExecRequestCode = requestCode;
        _lastExecTimestamp = currentTimestamp;
        _lastExecParamCount = paramCount;
        _lastExecFirstParam = paramCount > 0 ? parameters[1] : null;

        if(executeCommand)
        {
            cmd.execute(ctx, Arrays.copyOfRange(parameters, 1, parameters.length));
        }
    }
}
