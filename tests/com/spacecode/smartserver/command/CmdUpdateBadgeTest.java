package com.spacecode.smartserver.command;

import com.spacecode.sdk.device.Device;
import com.spacecode.sdk.network.communication.RequestCode;
import com.spacecode.sdk.user.User;
import com.spacecode.sdk.user.UsersService;
import com.spacecode.sdk.user.data.GrantType;
import com.spacecode.smartserver.SmartServer;
import com.spacecode.smartserver.database.DbManager;
import com.spacecode.smartserver.database.dao.DaoUser;
import com.spacecode.smartserver.database.entity.UserEntity;
import com.spacecode.smartserver.helper.DeviceHandler;
import io.netty.channel.ChannelHandlerContext;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mockito;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;

import static org.junit.Assert.fail;
import static org.mockito.Matchers.anyString;

/**
 * JUnit "CmdUpdateBadge" testing class.
 */
@RunWith(PowerMockRunner.class)
@PrepareForTest({DeviceHandler.class, Device.class, SmartServer.class, DbManager.class, UserEntity.class})
public class CmdUpdateBadgeTest
{
    private ChannelHandlerContext _ctx;
    private CmdUpdateBadge _command;
    private Device _device;
    private UsersService _usersService;
    private DaoUser _daoUser;

    public String _username = "Vincent";

    @Before
    public void setUp() throws Exception
    {
        _ctx = PowerMockito.mock(ChannelHandlerContext.class);
        _command = PowerMockito.mock(CmdUpdateBadge.class, Mockito.CALLS_REAL_METHODS);
        _device = PowerMockito.mock(Device.class);
        _usersService = PowerMockito.mock(UsersService.class);
        _daoUser = PowerMockito.mock(DaoUser.class);

        PowerMockito.doReturn(_usersService).when(_device).getUsersService();

        PowerMockito.mockStatic(DbManager.class);
        PowerMockito.mockStatic(SmartServer.class);

        PowerMockito.mockStatic(DeviceHandler.class);
        PowerMockito.when(DeviceHandler.class, "getDevice").thenReturn(_device);
        PowerMockito.when(DbManager.class, "getDao", UserEntity.class).thenReturn(_daoUser);
    }

    @After
    public void tearDown()
    {
        _ctx = null;
        _command = null;
        _device = null;
        _usersService = null;
        _daoUser = null;
    }

    @Test
    public void testExecuteInvalidNumberOfParams() throws Exception
    {
        try
        {
            _command.execute(_ctx, new String[0]);
            fail("ClientCommandException not thrown.");
        } catch(ClientCommandException cce)
        {
        }

        PowerMockito.verifyStatic();
        SmartServer.sendMessage(_ctx, RequestCode.UPDATE_BADGE, ClientCommand.FALSE);
    }

    @Test
    public void testExecuteDeviceNull() throws Exception
    {
        PowerMockito.when(DeviceHandler.class, "getDevice").thenReturn(null);

        _command.execute(_ctx, new String[] { _username });

        PowerMockito.verifyStatic();
        SmartServer.sendMessage(_ctx, RequestCode.UPDATE_BADGE, ClientCommand.FALSE);
    }
    
    @Test
    public void testExecuteUsersServiceFailed() throws Exception
    {
        String newBadge = "NEWBADGE";
        
        PowerMockito.doReturn(new User(_username, GrantType.ALL)).when(_usersService).getUserByName(_username);
        PowerMockito.doReturn(false).when(_usersService).updateBadgeNumber(anyString(), anyString());

        _command.execute(_ctx, new String[]{ _username, newBadge });

        // make sure that updateBadgeNumber() is called, but as it fails, that dao's method is never called
        Mockito.verify(_usersService).updateBadgeNumber(_username, newBadge);
        Mockito.verify(_daoUser, Mockito.never()).updateBadgeNumber(_username, newBadge);

        PowerMockito.verifyStatic();
        SmartServer.sendMessage(_ctx, RequestCode.UPDATE_BADGE, ClientCommand.FALSE);
    }

    @Test
    public void testExecuteDaoFailed() throws Exception
    {
        String badgeSave = "B000777123";
        String newBadge = "NEWBADGE";
        
        PowerMockito.doReturn(new User(_username, GrantType.ALL, badgeSave)).when(_usersService).getUserByName(_username);
        PowerMockito.doReturn(true).when(_usersService).updateBadgeNumber(anyString(), anyString());
        
        // dao fails
        PowerMockito.doReturn(false).when(_daoUser).updateBadgeNumber(anyString(), anyString());

        _command.execute(_ctx, new String[]{_username, newBadge});

        // Make sure that we tried first to update the badge number, but as DAO failed, UsersService restore the old.
        Mockito.verify(_usersService).updateBadgeNumber(_username, newBadge);
        Mockito.verify(_daoUser).updateBadgeNumber(_username, newBadge);
        Mockito.verify(_usersService).updateBadgeNumber(_username, badgeSave);

        PowerMockito.verifyStatic();
        SmartServer.sendMessage(_ctx, RequestCode.UPDATE_BADGE, ClientCommand.FALSE);
    }

    @Test
    public void testExecute() throws Exception
    {
        String badgeSave = "B000777123";
        String newBadge = "NEWBADGE";

        PowerMockito.doReturn(new User(_username, GrantType.ALL, badgeSave)).when(_usersService).getUserByName(_username);
        PowerMockito.doReturn(true).when(_usersService).updateBadgeNumber(anyString(), anyString());
        PowerMockito.doReturn(true).when(_daoUser).updateBadgeNumber(anyString(), anyString());

        _command.execute(_ctx, new String[]{_username, newBadge});
        
        Mockito.verify(_usersService).updateBadgeNumber(_username, newBadge);
        Mockito.verify(_daoUser).updateBadgeNumber(_username, newBadge);
        // the badge should not be "restored" by UsersService
        Mockito.verify(_usersService, Mockito.never()).updateBadgeNumber(_username, badgeSave);

        PowerMockito.verifyStatic();
        SmartServer.sendMessage(_ctx, RequestCode.UPDATE_BADGE, ClientCommand.TRUE);
    }
}