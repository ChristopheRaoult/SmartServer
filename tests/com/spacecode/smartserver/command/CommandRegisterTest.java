package com.spacecode.smartserver.command;

import com.spacecode.sdk.network.communication.RequestCode;
import com.spacecode.smartserver.SmartServer;
import com.spacecode.smartserver.helper.SmartLogger;
import io.netty.channel.Channel;
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
@PrepareForTest({ ClientCommandRegister.class, SmartLogger.class, SmartServer.class})
public class CommandRegisterTest
{
    private ClientCommandRegister _commandRegister;
    private Map<String, ClientCommand> _commands;
    
    private ChannelHandlerContext _ctx;
    private Channel _channel;

    @Before
    public void setUpbeforeTest()
    {
        _commands = new HashMap<>();
        
        _commandRegister = PowerMockito.mock(ClientCommandRegister.class, CALLS_REAL_METHODS);
        _ctx = PowerMockito.mock(ChannelHandlerContext.class);
        _channel = PowerMockito.mock(Channel.class);

        PowerMockito.doReturn(_channel).when(_ctx).channel();
        
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

        _commandRegister.execute(_ctx, params);
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
        _commandRegister.execute(_ctx, params);
        // verify the command has been executed [without the request code, obviously]
        verify(command).execute(_ctx, new String[0]);
    }

    @Test
    public void testExecuteAntiFloodNotPassing() throws ClientCommandException, InterruptedException
    {
        // Required as SmartLogger is used in this test
        PowerMockito.mockStatic(SmartServer.class);
        PowerMockito.mockStatic(SmartLogger.class);
        /*SmartLogger logger = PowerMockito.mock(SmartLogger.class);
        doReturn(logger).when(SmartLogger.getLogger());*/
        
        CmdAddAlert cmd = PowerMockito.mock(CmdAddAlert.class);
        _commands.put(RequestCode.ADD_ALERT, cmd);

        doNothing().when(cmd).execute(any(ChannelHandlerContext.class), any(String[].class));

        // execute the same request twice in a row to trigger the "anti-flood" delay
        _commandRegister.execute(_ctx, new String[]{RequestCode.ADD_ALERT, "fake_serialized_alert"});
        _commandRegister.execute(_ctx, new String[]{RequestCode.ADD_ALERT, "fake_serialized_alert"});

        // must be called once, not twice
        verify(cmd).execute(_ctx, new String[]{"fake_serialized_alert"});

        // with more than 1 parameter
        _commandRegister.execute(_ctx, new String[]{RequestCode.ADD_ALERT, "a" });
        _commandRegister.execute(_ctx, new String[]{RequestCode.ADD_ALERT, "a" });
        _commandRegister.execute(_ctx, new String[]{RequestCode.ADD_ALERT, "a" });

        // must be called once, not twice
        verify(cmd).execute(_ctx, new String[]{"a"});
        // called only once: should be called once
        verify(cmd).execute(_ctx, new String[]{"a"});        

        // repeat the same request twice, but wait for the anti-flood delay before
        _commandRegister.execute(_ctx, new String[]{RequestCode.ADD_ALERT, "1"});
        Thread.sleep(ClientCommandRegister.DELAY_BETWEEN_EXEC);
        _commandRegister.execute(_ctx, new String[]{RequestCode.ADD_ALERT, "1"});
        verify(cmd, times(2)).execute(_ctx, new String[]{"1"});
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
        _commandRegister.execute(_ctx, params);
        // verify the command has been executed
        verify(command).execute(_ctx, new String[] { "param1", "param2"});
    }
    
    @Test
    public void testExecuteAddUsertWithInvalidContract() throws ClientCommandException
    {
        CmdAddUser command = PowerMockito.mock(CmdAddUser.class);

        String[] params = new String[] { RequestCode.ADD_USER, "param1", "param2" };

        _commandRegister.addCommand(RequestCode.ADD_USER, command);
        
        // execute the command
        _commandRegister.execute(_ctx, params);
        // verify the command has been executed
        verify(command, never()).execute(_ctx, new String[] { "param1", "param2"});
    }
}
