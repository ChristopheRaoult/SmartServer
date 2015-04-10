package com.spacecode.smartserver.command;

import com.spacecode.sdk.device.Device;
import com.spacecode.sdk.network.alert.Alert;
import com.spacecode.sdk.network.alert.AlertType;
import com.spacecode.sdk.network.communication.RequestCode;
import com.spacecode.smartserver.SmartServer;
import com.spacecode.smartserver.database.DbManager;
import com.spacecode.smartserver.database.dao.DaoAlert;
import com.spacecode.smartserver.database.entity.AlertEntity;
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
import static org.mockito.Matchers.any;

/**
 * JUnit "CmdAddAlert" testing class.
 */
@RunWith(PowerMockRunner.class)
@PrepareForTest({DeviceHandler.class, Device.class, SmartServer.class, DbManager.class, UserEntity.class})
public class CmdAddAlertTest
{
    private ChannelHandlerContext _ctx;
    private CmdAddAlert _command;
    private DaoAlert _daoAlert;

    @Before
    public void setUp() throws Exception
    {
        _ctx = PowerMockito.mock(ChannelHandlerContext.class);
        _command = PowerMockito.mock(CmdAddAlert.class, Mockito.CALLS_REAL_METHODS);
        _daoAlert = PowerMockito.mock(DaoAlert.class);

        PowerMockito.mockStatic(DbManager.class);
        PowerMockito.mockStatic(SmartServer.class);

        PowerMockito.mockStatic(DeviceHandler.class);
        PowerMockito.when(DbManager.class, "getDao", AlertEntity.class).thenReturn(_daoAlert);
    }

    @After
    public void tearDown()
    {
        _ctx = null;
        _command = null;
        _daoAlert = null;
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
        SmartServer.sendMessage(_ctx, RequestCode.ADD_ALERT, ClientCommand.FALSE);
    }

    @Test
    public void testExecuteInvalidSerializedAlert() throws Exception
    {
        _command.execute(_ctx, new String[]{"Invalid Alert"});

        PowerMockito.verifyStatic();
        SmartServer.sendMessage(_ctx, RequestCode.ADD_ALERT, ClientCommand.FALSE);
    }

    /**
     * "Add Alert" only accepts instances of Alert which have no ID initialized (=> id equals 0). Alerts with an ID
     * (=> alerts which come from the DB) can be updated using "Update Alert".
     * 
     * @throws Exception
     */
    @Test
    public void testExecuteAlertAlreadyHasId() throws Exception
    {
        Alert alertWithId = new Alert(5, 
                AlertType.DEVICE_DISCONNECTED, 
                "vincent@sc.com", "", "", 
                "Subject", "Content", 
                true);
        
        _command.execute(_ctx, new String[]{alertWithId.serialize()});

        // make sure the dao does not try to persist this alert
        Mockito.verify(_daoAlert, Mockito.never()).persist(alertWithId);

        PowerMockito.verifyStatic();
        SmartServer.sendMessage(_ctx, RequestCode.ADD_ALERT, ClientCommand.FALSE);
    }

    /**
     * If the "AlertType" of the alert is TEMPERATURE, then the Alert MUST BE an instance of AlertTemperature
     * 
     * @throws Exception
     */
    @Test
    public void testExecuteInvalidAlertTemperature() throws Exception
    {
        Alert alertWithId = new Alert(AlertType.TEMPERATURE, "vincent@sc.com", "Subject", "Content", true);

        _command.execute(_ctx, new String[]{alertWithId.serialize()});

        // make sure the dao does not try to persist this alert
        Mockito.verify(_daoAlert, Mockito.never()).persist(alertWithId);

        PowerMockito.verifyStatic();
        SmartServer.sendMessage(_ctx, RequestCode.ADD_ALERT, ClientCommand.FALSE);
    }
    
    @Test
    public void testExecuteDaoFailed() throws Exception
    {        
        Alert alertWithId = new Alert(AlertType.DEVICE_DISCONNECTED, "vincent@sc.com", "Subject", "Content", true);
        PowerMockito.doReturn(false).when(_daoAlert).persist(any(Alert.class));

        _command.execute(_ctx, new String[]{alertWithId.serialize()});

        Mockito.verify(_daoAlert).persist(any(Alert.class));

        PowerMockito.verifyStatic();
        SmartServer.sendMessage(_ctx, RequestCode.ADD_ALERT, ClientCommand.FALSE);
    }

    @Test
    public void testExecute() throws Exception
    {
        Alert alertWithId = new Alert(AlertType.DEVICE_DISCONNECTED, "vincent@sc.com", "Subject", "Content", true);
        PowerMockito.doReturn(true).when(_daoAlert).persist(any(Alert.class));

        _command.execute(_ctx, new String[]{alertWithId.serialize()});

        Mockito.verify(_daoAlert).persist(any(Alert.class));

        PowerMockito.verifyStatic();
        SmartServer.sendMessage(_ctx, RequestCode.ADD_ALERT, ClientCommand.TRUE);
    }
}