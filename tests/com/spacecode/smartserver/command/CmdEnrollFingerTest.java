package com.spacecode.smartserver.command;

import com.spacecode.sdk.device.Device;
import com.spacecode.sdk.network.communication.RequestCode;
import com.spacecode.sdk.user.User;
import com.spacecode.sdk.user.UsersService;
import com.spacecode.sdk.user.data.FingerIndex;
import com.spacecode.smartserver.SmartServer;
import com.spacecode.smartserver.database.DbManager;
import com.spacecode.smartserver.database.dao.DaoFingerprint;
import com.spacecode.smartserver.database.dao.DaoUser;
import com.spacecode.smartserver.database.entity.FingerprintEntity;
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

import java.util.concurrent.TimeoutException;

import static org.junit.Assert.*;
import static org.mockito.Matchers.anyString;
import static org.mockito.Matchers.eq;

/**
 * JUnit "CmdEnrollFinger" testing class.
 */
@RunWith(PowerMockRunner.class)
@PrepareForTest({DeviceHandler.class, Device.class, SmartServer.class, User.class, DbManager.class, UserEntity.class })
public class CmdEnrollFingerTest
{
    private ChannelHandlerContext _ctx;
    private CmdEnrollFinger _command;
    private Device _device;
    private UsersService _usersService;
    private DaoUser _daoUser;
    private DaoFingerprint _daoFp;

    private String _username = "Vincent";
    private User _user;

    @Before
    public void setUp() throws Exception
    {
        _ctx = PowerMockito.mock(ChannelHandlerContext.class);
        _command = PowerMockito.mock(CmdEnrollFinger.class, Mockito.CALLS_REAL_METHODS);
        _device = PowerMockito.mock(Device.class);
        _usersService = PowerMockito.mock(UsersService.class);
        _daoUser = PowerMockito.mock(DaoUser.class);
        _daoFp = PowerMockito.mock(DaoFingerprint.class);

        _user = PowerMockito.mock(User.class);
        PowerMockito.doReturn(_username).when(_user).getUsername();        
        PowerMockito.doReturn(_user).when(_usersService).getUserByName(_username);
        PowerMockito.doReturn(_usersService).when(_device).getUsersService();

        PowerMockito.mockStatic(DbManager.class);
        PowerMockito.mockStatic(SmartServer.class);

        PowerMockito.mockStatic(DeviceHandler.class);
        PowerMockito.when(DeviceHandler.class, "getDevice").thenReturn(_device);
        PowerMockito.when(DbManager.class, "getDao", UserEntity.class).thenReturn(_daoUser);
        PowerMockito.when(DbManager.class, "getDao", FingerprintEntity.class).thenReturn(_daoFp);
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
            _command.execute(_ctx, new String[2]);
            fail("ClientCommandException not thrown.");
        } catch(ClientCommandException cce)
        {
        }

        PowerMockito.verifyStatic();
        SmartServer.sendMessage(_ctx, RequestCode.ENROLL_FINGER, ClientCommand.FALSE);
    }

    @Test
    public void testExecuteDeviceNull() throws Exception
    {
        PowerMockito.when(DeviceHandler.class, "getDevice").thenReturn(null);

        _command.execute(_ctx, new String[] { _username, String.valueOf(FingerIndex.LEFT_INDEX.getIndex()), "true" });

        PowerMockito.verifyStatic();
        SmartServer.sendMessage(_ctx, RequestCode.ENROLL_FINGER, ClientCommand.FALSE);
    }
    
    @Test
    public void testExecuteUnknownUser() throws ClientCommandException
    {
        PowerMockito.doReturn(null).when(_usersService).getUserByName(_username);

        _command.execute(_ctx, new String[]{_username, String.valueOf(FingerIndex.LEFT_INDEX.getIndex()), "true"});
        
        PowerMockito.verifyStatic();
        SmartServer.sendMessage(_ctx, RequestCode.ENROLL_FINGER, ClientCommand.FALSE);
    }

    @Test
    public void testExecuteInvalidFingerIndexDigit() throws Exception
    {
        // fingerindex is supposed to be a value (as string) between 0 and 9
        _command.execute(_ctx, new String[] { _username, "InvalidDigit", "true" });

        PowerMockito.verifyStatic();
        SmartServer.sendMessage(_ctx, RequestCode.ENROLL_FINGER, ClientCommand.FALSE);
    }

    @Test
    public void testExecuteInvalidFingerIndex() throws Exception
    {
        // fingerindex is supposed to be a value (as string) between 0 and 9
        _command.execute(_ctx, new String[] { _username, "80", "true" });

        PowerMockito.verifyStatic();
        SmartServer.sendMessage(_ctx, RequestCode.ENROLL_FINGER, ClientCommand.FALSE);
    }

    @Test
    public void testExecuteNoOverriding() throws Exception
    {
        FingerIndex fi = FingerIndex.LEFT_INDEX;
        
        _command.execute(_ctx, new String[]{_username, String.valueOf(fi.getIndex()), "true"});

        // make sure that if no template is given, we do not use the "enroll overriding"
        Mockito.verify(_usersService, Mockito.never()).enrollFinger(eq(_username), eq(fi), anyString());
    }

    @Test
    public void testExecuteTemplateOverriding() throws Exception
    {
        FingerIndex fi = FingerIndex.LEFT_INDEX;
        boolean useMaster = true;
        String overrideTpl = "tpl";

        PowerMockito.doReturn(overrideTpl).when(_user).getFingerprintTemplate(fi);
        PowerMockito.doReturn(true).when(_usersService).enrollFinger(_username, fi, overrideTpl);
        PowerMockito.doReturn(true).when(_daoFp).persist(_username, fi.getIndex(), overrideTpl);
        
        _command.execute(_ctx,
                new String[]{_username, String.valueOf(fi.getIndex()), String.valueOf(useMaster), overrideTpl});
        
        // make sure that the overriding is done
        Mockito.verify(_usersService).enrollFinger(_username, fi, overrideTpl);
        Mockito.verify(_command).persistTemplate(eq(_user), eq(fi), anyString());
        
        PowerMockito.verifyStatic();
        SmartServer.sendMessage(_ctx, RequestCode.ENROLL_FINGER, ClientCommand.TRUE);
    }
    
    @Test
    public void testEnrollAndPersistUsersServiceFails() throws TimeoutException
    {
        FingerIndex fi = FingerIndex.LEFT_INDEX;
        boolean useMaster = true;
        String oldTemplate = "old_tpl";

        User user = PowerMockito.mock(User.class);
        
        PowerMockito.doReturn(false).when(_usersService).enrollFinger(_username, fi, useMaster);
        PowerMockito.doReturn(_username).when(user).getUsername();
        
        assertFalse(_command.enrollAndPersist(user, fi, useMaster, oldTemplate));
        Mockito.verify(_command, Mockito.never()).persistTemplate(user, fi, oldTemplate);
    }
    
    @Test
    public void testEnrollAndPersistDaoFails() throws TimeoutException
    {
        FingerIndex fi = FingerIndex.LEFT_INDEX;
        boolean useMaster = true;
        String oldTemplate = "old_tpl";
        String newTemplate = "new_tpl";

        // dao fails
        PowerMockito.doReturn(false).when(_daoFp).persist(_username, fi.getIndex(), newTemplate);
        PowerMockito.doReturn(true).when(_usersService).enrollFinger(_username, fi, useMaster);

        assertFalse(_command.enrollAndPersist(_user, fi, useMaster, oldTemplate));
        
        Mockito.verify(_command).persistTemplate(_user, fi, oldTemplate);
        
        // when the DAO fails, make sure the old template is restored
        Mockito.verify(_usersService).enrollFinger(_username, fi, oldTemplate);
    }

    @Test
    public void testPersistTemplate() throws Exception
    {
        FingerIndex fi = FingerIndex.LEFT_INDEX;
        String oldTemplate = "old_tpl";
        String newTemplate = "new_tpl";

        PowerMockito.doReturn(true).when(_daoFp).persist(_username, fi.getIndex(), newTemplate);
        
        PowerMockito.doReturn(newTemplate).when(_user).getFingerprintTemplate(fi);
        assertTrue(_command.persistTemplate(_user, fi, oldTemplate));

        // the old template should not be restored
        Mockito.verify(_usersService, Mockito.never()).enrollFinger(_username, fi, oldTemplate);
    }
}