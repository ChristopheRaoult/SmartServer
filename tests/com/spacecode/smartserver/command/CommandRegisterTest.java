package com.spacecode.smartserver.command;

import com.spacecode.sdk.network.communication.RequestCode;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;

import static junit.framework.Assert.assertTrue;
import static org.junit.Assert.assertFalse;
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
        assertFalse(commandRegister.addCommand(null, null));
        assertFalse(commandRegister.addCommand("nullCommand", null));
    }

    @Test
    public void testAddCommandAlreadyExistingFails()
    {
        // operation must fail as a previous entry already exist (see ClientCommandRegister constructor)
        commandRegister.addCommand(RequestCode.DISCONNECT, new CmdDisconnect());
    }

    @Test
    public void testAddValidUnknownCommandSucceeds()
    {
        // add a new command (not existing)
        assertTrue(commandRegister.addCommand(RequestCode.DISCONNECT + "bis", new CmdDisconnect()));
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
