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

/**
 * JUnit "CmdRemoveUser" testing class.
 */
@RunWith(PowerMockRunner.class)
@PrepareForTest({DeviceHandler.class, Device.class, SmartServer.class, DbManager.class, UserEntity.class})
public class CmdRemoveUserTest
{
    private ChannelHandlerContext _ctx;
    private CmdRemoveUser _command;
    private Device _device;
    private UsersService _usersService;
    private DaoUser _daoUser;

    public String _username = "Vincent";

    @Before
    public void setUp() throws Exception
    {
        _ctx = PowerMockito.mock(ChannelHandlerContext.class);
        _command = PowerMockito.mock(CmdRemoveUser.class, Mockito.CALLS_REAL_METHODS);
        _device = PowerMockito.mock(Device.class);
        _usersService = PowerMockito.mock(UsersService.class);
        _daoUser = PowerMockito.mock(DaoUser.class);

        PowerMockito.doReturn(_usersService).when(_device).getUsersService();

        PowerMockito.mockStatic(DbManager.class);
        PowerMockito.mockStatic(SmartServer.class);

        PowerMockito.mockStatic(DeviceHandler.class);
        PowerMockito.when(DeviceHandler.class, "isAvailable").thenReturn(true);
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
    public void testExecuteInvalidSerializedUser() throws Exception
    {
        _command.execute(_ctx, new String[] { _username });

        PowerMockito.verifyStatic();
        SmartServer.sendMessage(_ctx, RequestCode.REMOVE_USER, ClientCommand.FALSE);
    }

    @Test
    public void testExecuteRemoveUserUnknown() throws Exception
    {        
        _command.execute(_ctx, new String[]{ _username });
        
        // as the user does not exist, the following methods are never called
        Mockito.verify(_usersService, Mockito.never()).removeUser(_username);
        Mockito.verify(_daoUser, Mockito.never()).removePermission(_username);
        
        PowerMockito.verifyStatic();
        SmartServer.sendMessage(_ctx, RequestCode.REMOVE_USER, ClientCommand.FALSE);
    }

    @Test
    public void testExecuteUsersServiceFailed() throws Exception
    {
        PowerMockito.doReturn(new User(_username, GrantType.ALL)).when(_usersService).getUserByName(_username);
        PowerMockito.doReturn(false).when(_usersService).removeUser(_username);
        
        _command.execute(_ctx, new String[]{ _username });
        
        // make sure that removeUser() is called, but as it fails, that removePermission is never called
        Mockito.verify(_usersService).removeUser(_username);
        Mockito.verify(_daoUser, Mockito.never()).removePermission(_username);
        
        PowerMockito.verifyStatic();
        SmartServer.sendMessage(_ctx, RequestCode.REMOVE_USER, ClientCommand.FALSE);
    }

    @Test
    public void testExecuteRemovePermissionFailed() throws Exception
    {
        GrantType grantSave = GrantType.ALL;
        
        PowerMockito.doReturn(new User(_username, grantSave)).when(_usersService).getUserByName(_username);
        PowerMockito.doReturn(true).when(_usersService).removeUser(_username);
        PowerMockito.doReturn(false).when(_daoUser).removePermission(_username);

        _command.execute(_ctx, new String[]{_username});

        // both methods should be called
        Mockito.verify(_usersService).removeUser(_username);
        Mockito.verify(_daoUser).removePermission(_username);
        
        // make sure the user is added again if the Dao failed removing the permission from DB
        Mockito.verify(_usersService).updatePermission(_username, grantSave);

        PowerMockito.verifyStatic();
        SmartServer.sendMessage(_ctx, RequestCode.REMOVE_USER, ClientCommand.FALSE);
    }

    @Test
    public void testExecute() throws Exception
    {
        GrantType grantSave = GrantType.ALL;

        PowerMockito.doReturn(new User(_username, grantSave)).when(_usersService).getUserByName(_username);
        PowerMockito.doReturn(true).when(_usersService).removeUser(_username);
        PowerMockito.doReturn(true).when(_daoUser).removePermission(_username);

        _command.execute(_ctx, new String[]{_username});

        // both methods should be called
        Mockito.verify(_usersService).removeUser(_username);
        Mockito.verify(_daoUser).removePermission(_username);

        // this method should NOT be called (to restore the user's permission) as it has been successfully removed
        Mockito.verify(_usersService, Mockito.never()).updatePermission(_username, grantSave);

        PowerMockito.verifyStatic();
        SmartServer.sendMessage(_ctx, RequestCode.REMOVE_USER, ClientCommand.TRUE);
    }
}