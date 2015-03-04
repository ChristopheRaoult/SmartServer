package com.spacecode.smartserver.command;

import com.spacecode.smartserver.SmartServer;
import io.netty.channel.ChannelHandlerContext;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mockito;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;

import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.never;
import static org.powermock.api.mockito.PowerMockito.verifyStatic;

/**
 * JUnit "CmdUpdateReport" testing class.
 */
@RunWith(PowerMockRunner.class)
@PrepareForTest({SmartServer.class})
public class CmdUpdateReportTest
{
    private ChannelHandlerContext _ctx;
    private CmdUpdateReport _command;

    @Before
    public void setUp() throws Exception
    {
        _ctx = PowerMockito.mock(ChannelHandlerContext.class);
        _command = PowerMockito.mock(CmdUpdateReport.class, Mockito.CALLS_REAL_METHODS);

        PowerMockito.mockStatic(SmartServer.class);
    }

    @After
    public void tearDown()
    {
        _ctx = null;
        _command = null;
    }

    @Test
    public void testExecuteInvalidParameter() throws Exception
    {
        _command.execute(_ctx, new String[] { "" });
        _command.execute(_ctx, new String[0]);
        _command.execute(_ctx, new String[] { "A" });
        verifyStatic(never());
        SmartServer.sendAllClients(anyString(), anyString());
    }

    @Test
    public void testExecuteUpdateFailure() throws Exception
    {
        _command.execute(_ctx, new String[] { "-1" });

        verifyStatic();
        SmartServer.sendAllClients(CmdUpdateReport.EVENT_CODE_ENDED, ClientCommand.FALSE);
    }

    @Test
    public void testExecuteUpdateSuccess() throws Exception
    {
        _command.execute(_ctx, new String[] { "0" });

        verifyStatic();
        SmartServer.sendAllClients(CmdUpdateReport.EVENT_CODE_ENDED, ClientCommand.TRUE);
    }

    @Test
    public void testExecuteUpdateProgress() throws Exception
    {
        _command.execute(_ctx, new String[] { "8" });
        _command.execute(_ctx, new String[] { "7" });
        _command.execute(_ctx, new String[] { "3" });

        verifyStatic();
        SmartServer.sendAllClients(CmdUpdateReport.EVENT_CODE_STARTED);
        verifyStatic();
        SmartServer.sendAllClients(CmdUpdateReport.EVENT_CODE_PROGRESS, "7", "8");
        verifyStatic();
        SmartServer.sendAllClients(CmdUpdateReport.EVENT_CODE_PROGRESS, "3", "8");
    }
}