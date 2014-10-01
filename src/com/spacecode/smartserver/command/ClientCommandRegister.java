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
    // Visibility package-local in order to be accessible for JUnit tests.
    Map<String, ClientCommand> _commands = new HashMap<>();

    /**
     * Initialize command register.
     * Add an entry to "_commands" HashMap with request code & command instance for each known request.
     */
    public ClientCommandRegister()
    {
        _commands.put(RequestCode.ADD_ALERT,            new CommandAddAlert());
        _commands.put(RequestCode.ADD_USER,             new CommandAddUser());
        _commands.put(RequestCode.ALERTS_LIST,          new CommandAlertsList());
        _commands.put(RequestCode.DISCONNECT,           new CommandDisconnect());
        _commands.put(RequestCode.ENROLL_FINGER,        new CommandEnrollFinger());
        _commands.put(RequestCode.INITIALIZATION,       new CommandInitialization());
        _commands.put(RequestCode.LAST_INVENTORY,       new CommandLastInventory());
        _commands.put(RequestCode.REMOVE_ALERT,         new CommandRemoveAlert());
        _commands.put(RequestCode.REMOVE_FINGERPRINT,   new CommandRemoveFingerprint());
        _commands.put(RequestCode.REMOVE_USER,          new CommandRemoveUser());
        _commands.put(RequestCode.REWRITE_UID,          new CommandRewriteUid());
        _commands.put(RequestCode.SCAN,                 new CommandScan());
        _commands.put(RequestCode.SET_SMTP_SERVER,      new CommandSetSmtpServer());
        _commands.put(RequestCode.SET_THIEF_FINGER,     new CommandSetThiefFinger());
        _commands.put(RequestCode.SERIAL_BRIDGE,        new CommandSerialBridge());
        _commands.put(RequestCode.SMTP_SERVER,          new CommandSmtpServer());
        _commands.put(RequestCode.START_LIGHTING,       new CommandStartLighting());
        _commands.put(RequestCode.STOP_LIGHTING,        new CommandStoptLighting());
        _commands.put(RequestCode.UPDATE_PERMISSION,    new CommandUpdatePermission());
        _commands.put(RequestCode.UPDATE_ALERT,         new CommandUpdateAlert());
        _commands.put(RequestCode.UPDATE_BADGE,         new CommandUpdateBadge());
        _commands.put(RequestCode.USER_BY_NAME,         new CommandUserByName());
        _commands.put(RequestCode.USERS_LIST,           new CommandUsersList());
        _commands.put(RequestCode.TEMPERATURE,          new CommandTemperature());
    }

    /**
     * Allow dynamic (new) command registering.
     *
     * @param commandName       Command name. Should correspond to a RequestCode.
     * @param commandInstance   Command instance (implementing ClientCommand) which should be executed.
     */
    public void addCommand(String commandName, ClientCommand commandInstance)
    {
        if(commandName == null || commandInstance == null)
        {
            return;
        }

        String cmd = commandName.trim();

        if(cmd.isEmpty() || _commands.containsKey(cmd))
        {
            return;
        }

        _commands.put(cmd, commandInstance);
    }

    /**
     * Looks for a ClientCommand corresponding to the given request and execute it with given parameters.
     * Index 0 of the given string array contains the request. Other values (if any) contains the parameters sent by the client.
     *
     * @param ctx                       ChannelHandlerContext instance corresponding to the channel existing between SmartServer and the client.
     * @param parameters                String array containing parameters (if any) provided by the client.
     *
     * @throws ClientCommandException   Occurs if no command has been found for the given request code.
     */
    @Override
    public void execute(final ChannelHandlerContext ctx, final String[] parameters) throws ClientCommandException
    {
        final ClientCommand cmd = _commands.get(parameters[0]);

        if(cmd == null)
        {
            throw new ClientCommandException("Unknown Command: " + parameters[0]);
        }

        // TODO: make sure a request can't be handled twice in a row (ex: double adduser request)
        cmd.execute(ctx, Arrays.copyOfRange(parameters, 1, parameters.length));
    }
}
