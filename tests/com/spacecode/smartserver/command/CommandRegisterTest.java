package com.spacecode.smartserver.command;

import com.spacecode.sdk.network.communication.RequestCode;
import io.netty.channel.ChannelHandlerContext;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;
import org.powermock.reflect.Whitebox;

import java.util.HashMap;
import java.util.Map;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.*;

/**
 * JUnit "ClientCommandRegister" testing class.
 */
@RunWith(PowerMockRunner.class)
@PrepareForTest(ClientCommandRegister.class)
public class CommandRegisterTest
{
    private ClientCommandRegister _commandRegister;
    private Map<String, ClientCommand> _commands;

    @Before
    public void setUpbeforeTest()
    {
        _commandRegister = PowerMockito.mock(ClientCommandRegister.class, CALLS_REAL_METHODS);
        _commands = new HashMap<>();

        Whitebox.setInternalState(_commandRegister, "_commands", _commands);
        Whitebox.setInternalState(_commandRegister, "_lastExecPackets", new String[] { "" });
    }

    @Test
    public void testAddNullCommandFails()
    {
        assertFalse(_commandRegister.addCommand(null, null));
        assertFalse(_commandRegister.addCommand("nullCommand", null));
    }

    @Test
    public void testAddCommandAlreadyExistingFails()
    {
        _commands.put(RequestCode.DISCONNECT, new CmdDisconnect());

        // operation must fail as a previous entry already exist
        assertFalse(_commandRegister.addCommand(RequestCode.DISCONNECT, new CmdDisconnect()));
    }

    @Test
    public void testAddValidUnknownCommandSucceeds()
    {
        // add a new command (not existing)
        assertTrue(_commandRegister.addCommand(RequestCode.DISCONNECT + "bis", new CmdDisconnect()));
    }

    @Test(expected = ClientCommandException.class)
    public void testExecuteUnknownRequestCode() throws ClientCommandException
    {
        String[] params = new String[] { "not_existing_rule" };

        _commandRegister.execute(null, params);
    }

    @Test
    public void testExecuteKnownRequestCode() throws ClientCommandException
    {
        ClientCommand command = PowerMockito.mock(ClientCommand.class);
        String requestCode = "FakeRequest";
        String[] params = new String[] { requestCode };

        // register the command
        _commandRegister.addCommand(requestCode, command);

        // execute the command
        _commandRegister.execute(null, params);
        // verify the command has been executed [without the request code, obviously]
        verify(command).execute(null, new String[0]);
    }

    @Test
    public void testExecuteAntiFloodNotPassing() throws ClientCommandException, InterruptedException
    {
        CmdAddAlert cmd = PowerMockito.mock(CmdAddAlert.class);
        _commands.put(RequestCode.ADD_ALERT, cmd);

        doNothing().when(cmd).execute(any(ChannelHandlerContext.class), any(String[].class));

        // execute the same request twice in a row to trigger the "anti-flood" delay
        _commandRegister.execute(null, new String[]{RequestCode.ADD_ALERT, "fake_serialized_alert"});
        _commandRegister.execute(null, new String[]{RequestCode.ADD_ALERT, "fake_serialized_alert"});

        // must be called once, not twice
        verify(cmd).execute(null, new String[]{"fake_serialized_alert"});

        // with more than 1 parameter
        _commandRegister.execute(null, new String[]{RequestCode.ADD_ALERT, "a", "b", "c"});
        _commandRegister.execute(null, new String[]{RequestCode.ADD_ALERT, "a", "b", "c"});
        _commandRegister.execute(null, new String[]{RequestCode.ADD_ALERT, "a", "b", "z"});

        // must be called once, not twice
        verify(cmd).execute(null, new String[]{"a", "b", "c"});
        // called only once: should be called once
        verify(cmd).execute(null, new String[]{"a", "b", "z"});        

        // repeat the same request twice, but wait for the anti-flood delay before
        _commandRegister.execute(null, new String[]{RequestCode.ADD_ALERT, "1", "2"});
        Thread.sleep(ClientCommandRegister.DELAY_BETWEEN_EXEC);
        _commandRegister.execute(null, new String[]{RequestCode.ADD_ALERT, "1", "2"});
        verify(cmd, times(2)).execute(null, new String[]{"1", "2"});
    }

    @Test
    public void testExecuteKnownRequestCodeWithParameters() throws ClientCommandException
    {
        ClientCommand command = PowerMockito.mock(ClientCommand.class);
        String requestCode = "FakeRequest";
        String[] params = new String[] { requestCode, "param1", "param2" };

        // register the command
        _commandRegister.addCommand(requestCode, command);

        // execute the command
        _commandRegister.execute(null, params);
        // verify the command has been executed [without the request code, obviously]
        verify(command).execute(null, new String[] { "param1", "param2"});
    }
}
