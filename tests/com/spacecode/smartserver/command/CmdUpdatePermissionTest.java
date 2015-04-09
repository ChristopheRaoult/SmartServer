package com.spacecode.smartserver.command;

import com.spacecode.sdk.device.Device;
import com.spacecode.sdk.network.communication.RequestCode;
import com.spacecode.sdk.user.UsersService;
import com.spacecode.sdk.user.data.GrantType;
import com.spacecode.smartserver.SmartServer;
import com.spacecode.smartserver.database.DbManager;
import com.spacecode.smartserver.database.dao.DaoGrantedAccess;
import com.spacecode.smartserver.database.entity.GrantedAccessEntity;
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

/**
 * JUnit "CmdUpdatePermission" testing class.
 */
@RunWith(PowerMockRunner.class)
@PrepareForTest({DeviceHandler.class, Device.class, SmartServer.class, DbManager.class, UserEntity.class})
public class CmdUpdatePermissionTest
{
    private ChannelHandlerContext _ctx;
    private CmdUpdatePermission _command;
    private Device _device;
    private UsersService _usersService;
    private DaoGrantedAccess _daoGrantedAccess;

    public String _username = "Vincent";

    @Before
    public void setUp() throws Exception
    {
        _ctx = PowerMockito.mock(ChannelHandlerContext.class);
        _command = PowerMockito.mock(CmdUpdatePermission.class, Mockito.CALLS_REAL_METHODS);
        _device = PowerMockito.mock(Device.class);
        _usersService = PowerMockito.mock(UsersService.class);
        _daoGrantedAccess = PowerMockito.mock(DaoGrantedAccess.class);

        PowerMockito.doReturn(_usersService).when(_device).getUsersService();

        PowerMockito.mockStatic(DbManager.class);
        PowerMockito.mockStatic(SmartServer.class);

        PowerMockito.mockStatic(DeviceHandler.class);
        PowerMockito.when(DeviceHandler.class, "getDevice").thenReturn(_device);
        PowerMockito.when(DbManager.class, "getDao", GrantedAccessEntity.class).thenReturn(_daoGrantedAccess);
    }

    @After
    public void tearDown()
    {
        _ctx = null;
        _command = null;
        _device = null;
        _usersService = null;
        _daoGrantedAccess = null;
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
        SmartServer.sendMessage(_ctx, RequestCode.UPDATE_PERMISSION, ClientCommand.FALSE);
    }

    @Test
    public void testExecuteDeviceNull() throws Exception
    {
        PowerMockito.when(DeviceHandler.class, "getDevice").thenReturn(null);

        _command.execute(_ctx, new String[] { "username", GrantType.MASTER.name() });

        PowerMockito.verifyStatic();
        SmartServer.sendMessage(_ctx, RequestCode.UPDATE_PERMISSION, ClientCommand.FALSE);
    }

    @Test
    public void testExecuteInvalidUsername() throws Exception
    {
        _command.execute(_ctx, new String[] { "", GrantType.MASTER.name() });

        PowerMockito.verifyStatic();
        SmartServer.sendMessage(_ctx, RequestCode.UPDATE_PERMISSION, ClientCommand.FALSE);
    }

    @Test
    public void testExecuteInvalidPermissionType() throws Exception
    {
        _command.execute(_ctx, new String[] { "User154", "InvalidGrantType" });

        PowerMockito.verifyStatic();
        SmartServer.sendMessage(_ctx, RequestCode.UPDATE_PERMISSION, ClientCommand.FALSE);
    }

    @Test
    public void testExecuteUsersServiceFails() throws Exception
    {
        String username = "Jack";
        GrantType permission = GrantType.ALL;
        
        _command.execute(_ctx, new String[] { username, permission.name() });
        
        PowerMockito.doReturn(false).when(_usersService).updatePermission(username, permission);
        
        Mockito.verify(_daoGrantedAccess, Mockito.never()).persist(username, permission);

        PowerMockito.verifyStatic();
        SmartServer.sendMessage(_ctx, RequestCode.UPDATE_PERMISSION, ClientCommand.FALSE);
    }

    @Test
    public void testExecutePersistFails() throws Exception
    {
        String username = "Jack";
        GrantType permission = GrantType.ALL;

        PowerMockito.doReturn(true).when(_usersService).updatePermission(username, permission);
        PowerMockito.doReturn(false).when(_daoGrantedAccess).persist(username, permission);
        
        _command.execute(_ctx, new String[]{username, permission.name()});

        Mockito.verify(_daoGrantedAccess).persist(username, permission);

        PowerMockito.verifyStatic();
        SmartServer.sendMessage(_ctx, RequestCode.UPDATE_PERMISSION, ClientCommand.FALSE);
    }

    @Test
    public void testExecute() throws Exception
    {
        String username = "Jack";
        GrantType permission = GrantType.ALL;

        PowerMockito.doReturn(true).when(_usersService).updatePermission(username, permission);
        PowerMockito.doReturn(true).when(_daoGrantedAccess).persist(username, permission);

        _command.execute(_ctx, new String[]{ username, permission.name() });

        Mockito.verify(_daoGrantedAccess).persist(username, permission);

        PowerMockito.verifyStatic();
        SmartServer.sendMessage(_ctx, RequestCode.UPDATE_PERMISSION, ClientCommand.TRUE);
    }
}