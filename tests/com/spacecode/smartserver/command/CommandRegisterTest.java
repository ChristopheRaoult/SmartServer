package com.spacecode.smartserver.command;

import com.spacecode.sdk.network.communication.RequestCode;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;

import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.verify;

/**
 * JUnit "ClientCommandRegister" testing class.
 */
@RunWith(PowerMockRunner.class)
@PrepareForTest(ClientCommandRegister.class)
public class CommandRegisterTest
{
    private ClientCommandRegister commandRegister;

    @Before
    public void setUpbeforeTest()
    {
        commandRegister = new ClientCommandRegister();
    }

    @Test
    public void testAddNullCommandFails()
    {
        int registerSize = commandRegister._commands.size();
        commandRegister.addCommand(null, null);
        commandRegister.addCommand("nullCommand", null);
        // size mustn't change as previous addCommand have invalid parameters.
        assertEquals(registerSize, commandRegister._commands.size());
    }

    @Test
    public void testAddCommandAlreadyExistingFails()
    {
        int registerSize = commandRegister._commands.size();
        commandRegister.addCommand(RequestCode.DISCONNECT, new CommandDisconnect());
        // size mustn't change as previous entry already exist (see ClientCommandRegister constructor)
        assertEquals(registerSize, commandRegister._commands.size());
    }

    @Test
    public void testAddValidUnknownCommandSucceeds()
    {
        int registerSize = commandRegister._commands.size();
        // add a new command (not existing), supposed to have a new rule
        commandRegister.addCommand(RequestCode.DISCONNECT + "bis", new CommandDisconnect());
        assertEquals(registerSize+1, commandRegister._commands.size());
    }

    @Test(expected = ClientCommandException.class)
    public void testExecuteUnknownRequestCode() throws ClientCommandException
    {
        String[] params = new String[] { "not_existing_rule" };

        commandRegister.execute(null, params);
    }

    @Test
    public void testExecuteKnownRequestCode() throws ClientCommandException
    {
        ClientCommand command = PowerMockito.mock(ClientCommand.class);
        String requestCode = "FakeRequest";
        String[] params = new String[] { requestCode };

        // register the command
        commandRegister.addCommand(requestCode, command);

        // execute the command
        commandRegister.execute(null, params);
        // verify the command has been executed [without the request code, obviously]
        verify(command).execute(null, new String[0]);
    }

    @Test
    public void testExecuteKnownRequestCodeWithParameters() throws ClientCommandException
    {
        ClientCommand command = PowerMockito.mock(ClientCommand.class);
        String requestCode = "FakeRequest";
        String[] params = new String[] { requestCode, "param1", "param2" };

        // register the command
        commandRegister.addCommand(requestCode, command);

        // execute the command
        commandRegister.execute(null, params);
        // verify the command has been executed [without the request code, obviously]
        verify(command).execute(null, new String[] { "param1", "param2"});
    }
}
