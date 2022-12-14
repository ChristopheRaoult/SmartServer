package com.spacecode.smartserver.command;

import com.j256.ormlite.dao.ForeignCollection;
import com.spacecode.sdk.device.Device;
import com.spacecode.sdk.network.communication.RequestCode;
import com.spacecode.sdk.user.User;
import com.spacecode.sdk.user.UsersService;
import com.spacecode.sdk.user.data.GrantType;
import com.spacecode.smartserver.SmartServer;
import com.spacecode.smartserver.database.DbManager;
import com.spacecode.smartserver.database.dao.DaoUser;
import com.spacecode.smartserver.database.entity.FingerprintEntity;
import com.spacecode.smartserver.database.entity.UserEntity;
import com.spacecode.smartserver.helper.DeviceHandler;
import io.netty.channel.ChannelHandlerContext;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Matchers;
import org.mockito.Mockito;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;

import java.util.Collection;
import java.util.Iterator;

/**
 * JUnit "CmdAddUser" testing class.
 */
@RunWith(PowerMockRunner.class)
@PrepareForTest({DeviceHandler.class, Device.class, SmartServer.class, DbManager.class, UserEntity.class})
public class CmdAddUserTest
{
    private ChannelHandlerContext _ctx;
    private CmdAddUser _command;
    private Device _device;
    private UsersService _usersService;
    private DaoUser _daoUser;

    private String _serializedUser;
    public String _username = "Vincent";

    @Before
    public void setUp() throws Exception
    {
        _ctx = PowerMockito.mock(ChannelHandlerContext.class);
        _command = PowerMockito.mock(CmdAddUser.class, Mockito.CALLS_REAL_METHODS);
        _device = PowerMockito.mock(Device.class);
        _usersService = PowerMockito.mock(UsersService.class);
        _daoUser = PowerMockito.mock(DaoUser.class);

        _serializedUser = new User(_username, GrantType.ALL, "BBC019575").serialize();

        PowerMockito.doReturn(_usersService).when(_device).getUsersService();

        PowerMockito.mockStatic(DbManager.class);
        PowerMockito.mockStatic(SmartServer.class);

        PowerMockito.mockStatic(DeviceHandler.class);
        PowerMockito.when(DeviceHandler.class, "getDevice").thenReturn(_device);
        PowerMockito.when(DeviceHandler.class, "isAvailable").thenReturn(true);
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
        _serializedUser = null;
    }

    @Test
    public void testExecuteDeviceNull() throws Exception
    {
        PowerMockito.when(DeviceHandler.class, "getDevice").thenReturn(null);

        _command.execute(_ctx, new String[] { "Serialized User" });

        PowerMockito.verifyStatic();
        SmartServer.sendMessage(_ctx, RequestCode.ADD_USER, ClientCommand.FALSE);
    }

    @Test
    public void testExecuteInvalidSerializedUser() throws Exception
    {
        _command.execute(_ctx, new String[] { "Serialized User" });

        PowerMockito.verifyStatic();
        SmartServer.sendMessage(_ctx, RequestCode.ADD_USER, ClientCommand.FALSE);
    }

    @Test
    public void testExecuteUserWithSameNameExists() throws Exception
    {
        String existingBadge = "123456";
        int fingerIndex = 1;
        String fingerTpl = "fake_template";

        // mock the existing user entity
        UserEntity existingUserEntity = PowerMockito.mock(UserEntity.class);
        PowerMockito.doReturn(_username).when(existingUserEntity).getUsername();
        PowerMockito.doReturn(existingBadge).when(existingUserEntity).getBadgeNumber();

        // mock its fingerprint collection
        Collection<FingerprintEntity> fpEntities = PowerMockito.mock(ForeignCollection.class);
        // mock an iterator for the foreach loop browsing the collection
        Iterator<FingerprintEntity> iterator = PowerMockito.mock(Iterator.class);
        PowerMockito.doReturn(true).doReturn(false).when(iterator).hasNext();
        PowerMockito.doReturn(new FingerprintEntity(existingUserEntity, fingerIndex, fingerTpl)).when(iterator).next();
        PowerMockito.doReturn(iterator).when(fpEntities).iterator();
        PowerMockito.doReturn(fpEntities).when(existingUserEntity).getFingerprints();

        // mock the user repo to return the desired user entity
        PowerMockito.doReturn(existingUserEntity).when(_daoUser).getByUsername(_username);
        _command.execute(_ctx, new String[]{_serializedUser});
        Mockito.verify(_usersService, Mockito.never()).addUser(Matchers.any(User.class));
        PowerMockito.verifyStatic();
        SmartServer.sendMessage(_ctx, RequestCode.ADD_USER, ClientCommand.FALSE);

        // mock the user repo to return null (= there is no user with the same name)
        PowerMockito.doReturn(null).when(_daoUser).getByUsername(_username);
        
        // Step 1 - UsersService fails
        PowerMockito.doReturn(false).when(_usersService).addUser(Matchers.any(User.class));
        _command.execute(_ctx, new String[]{_serializedUser});
        Mockito.verify(_usersService).addUser(Matchers.any(User.class));
        // check that False is returned to user [times(2) because the mock already registered the two calls above]
        PowerMockito.verifyStatic(Mockito.times(2));
        SmartServer.sendMessage(_ctx, RequestCode.ADD_USER, ClientCommand.FALSE);

        // Step 2 - UsersService succeeds, but UserRepository fails persisting
        PowerMockito.doReturn(true).when(_usersService).addUser(Matchers.any(User.class));
        _command.execute(_ctx, new String[]{_serializedUser});
        PowerMockito.doReturn(false).when(_daoUser).persist(Matchers.any(User.class));
        // check the user is removed from UsersService
        Mockito.verify(_usersService).removeUser(_username);
        // check that False is returned to user [times(3) because the mock already registered the two calls above]
        PowerMockito.verifyStatic(Mockito.times(3));
        SmartServer.sendMessage(_ctx, RequestCode.ADD_USER, ClientCommand.FALSE);

        // Step 3 - DaoUser succeeds in persisting user
        PowerMockito.doReturn(true).when(_daoUser).persist(Matchers.any(User.class));
        _command.execute(_ctx, new String[]{_serializedUser});
        // check that True is returned to user
        PowerMockito.verifyStatic();
        SmartServer.sendMessage(_ctx, RequestCode.ADD_USER, ClientCommand.TRUE);
    }
}