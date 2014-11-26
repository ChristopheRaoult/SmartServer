package com.spacecode.smartserver.command;

import com.spacecode.sdk.network.communication.RequestCode;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;

import static org.junit.Assert.assertEquals;

/**
 * JUnit "ClientCommandRegister" testing class.
 */
@RunWith(PowerMockRunner.class)
@PrepareForTest(ClientCommandRegister.class)
public class CommandRegisterTest
{
    private static ClientCommandRegister _commandRegister;

    @BeforeClass
    public static void setUpBeforeClass()
    {
        _commandRegister = new ClientCommandRegister();
    }

    @Test
    public void testAddCommand()
    {
        int registerSize = _commandRegister._commands.size();
        _commandRegister.addCommand(null, null);
        _commandRegister.addCommand("nullCommand", null);
        // size mustn't change as previous addCommand have invalid parameters.
        assertEquals(registerSize, _commandRegister._commands.size());

        _commandRegister.addCommand(RequestCode.DISCONNECT, new CommandDisconnect());
        // size mustn't change as previous entry already exist (see ClientCommandRegister constructor)
        assertEquals(registerSize, _commandRegister._commands.size());

        _commandRegister.addCommand(RequestCode.DISCONNECT + "bis", new CommandDisconnect());
        assertEquals(registerSize+1, _commandRegister._commands.size());
    }

    @Test(expected = ClientCommandException.class)
    public void testExecuteUnknownRule() throws ClientCommandException
    {
        String[] params = new String[] { "not_existing_rule" };

        _commandRegister.execute(null, params);
    }
}
