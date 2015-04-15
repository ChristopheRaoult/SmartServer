package com.spacecode.smartserver.helper;

import com.spacecode.sdk.device.Device;
import com.spacecode.sdk.device.DeviceCreationException;
import com.spacecode.sdk.device.data.DeviceStatus;
import com.spacecode.sdk.device.data.DeviceType;
import com.spacecode.sdk.device.data.Inventory;
import com.spacecode.sdk.device.data.PluggedDevice;
import com.spacecode.sdk.device.event.DeviceEventHandler;
import com.spacecode.sdk.device.module.authentication.FingerprintReader;
import com.spacecode.sdk.network.communication.EventCode;
import com.spacecode.sdk.user.User;
import com.spacecode.sdk.user.UsersService;
import com.spacecode.sdk.user.data.AccessType;
import com.spacecode.sdk.user.data.GrantType;
import com.spacecode.smartserver.SmartServer;
import com.spacecode.smartserver.database.DbManager;
import com.spacecode.smartserver.database.dao.DaoAuthentication;
import com.spacecode.smartserver.database.dao.DaoInventory;
import com.spacecode.smartserver.database.dao.DaoUser;
import com.spacecode.smartserver.database.entity.AuthenticationEntity;
import com.spacecode.smartserver.database.entity.InventoryEntity;
import com.spacecode.smartserver.database.entity.UserEntity;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;
import org.powermock.reflect.Whitebox;

import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import static org.junit.Assert.*;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyString;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.*;
import static org.mockito.Mockito.anyListOf;
import static org.powermock.api.mockito.PowerMockito.doNothing;
import static org.powermock.api.mockito.PowerMockito.doReturn;
import static org.powermock.api.mockito.PowerMockito.*;
import static org.powermock.api.mockito.PowerMockito.when;

/**
 * JUnit "DeviceHandler" testing class.
 */
@RunWith(PowerMockRunner.class)
@PrepareForTest({ DeviceHandler.class, Device.class, SmartLogger.class, SmartServer.class,
        DbManager.class, Inventory.class, ConfManager.class, FingerprintReader.class })
public class DeviceHandlerTest
{
    private DeviceHandler.SmartEventHandler _eventHandler;
    private Device _device;
    private SmartLogger _smartLogger;

    private String _devSerial = "AA770201";
    private String _devPort = "COM6";
    private String _swVersion = "3.58";
    private String _hwVersion = "1";
    private DeviceType _devType = DeviceType.SMARTBOARD;

    @Before
    public void setUp() throws Exception
    {
        mockStatic(DbManager.class);
        mockStatic(SmartServer.class);
        mockStatic(SmartLogger.class);
        _device = PowerMockito.mock(Device.class);
        _smartLogger = PowerMockito.mock(SmartLogger.class);

        Whitebox.setInternalState(SmartLogger.class, "LOGGER", _smartLogger);
        doReturn(_smartLogger).when(SmartLogger.class, "getLogger");
        doNothing().when(_smartLogger).warning(anyString());
        whenNew(SmartLogger.class).withNoArguments().thenReturn(_smartLogger);

        Whitebox.setInternalState(DeviceHandler.class, _device);

        doReturn(_devSerial).when(_device).getSerialNumber();
        doReturn(_swVersion).when(_device).getSoftwareVersion();
        doReturn(_hwVersion).when(_device).getHardwareVersion();
        doReturn(_devType).when(_device).getDeviceType();

        _eventHandler = PowerMockito.mock(DeviceHandler.SmartEventHandler.class, CALLS_REAL_METHODS);
    }

    @After
    public void tearDown()
    {
        _device = null;
        _smartLogger = null;
        _eventHandler = null;
    }

    @Test
    public void testConnectDeviceAlreadyCreated() throws Exception
    {
        assertTrue(DeviceHandler.connectDevice());
    }

    @Test
    public void testConnectDeviceNoDevice() throws Exception
    {
        // _device must be null if we want to try to connect it
        Whitebox.setInternalState(DeviceHandler.class, "_device", (Object) null);

        mockStatic(Device.class);
        doReturn(new TreeMap<String, PluggedDevice>())
                .when(Device.class, "getPluggedDevices");

        assertFalse(DeviceHandler.connectDevice());
        verify(_smartLogger).warning(anyString());
    }

    @Test
    public void testConnectDeviceFailInstantiate() throws Exception
    {
        // _device must be null if we want to try to connect it
        Whitebox.setInternalState(DeviceHandler.class, "_device", (Object) null);

        // information about the mocked connected device
        Map<String, PluggedDevice> pluggedDevices = new TreeMap<>();
        pluggedDevices.put(_devSerial, new PluggedDevice(_devSerial, _devPort,
                _swVersion, _hwVersion, _devType));

        mockStatic(Device.class);
        doReturn(pluggedDevices).when(Device.class, "getPluggedDevices");

        whenNew(Device.class).withArguments(any(), eq(_devPort)).thenThrow(new DeviceCreationException(""));
        assertFalse(DeviceHandler.connectDevice());
        verify(_device, never()).addListener(any(DeviceEventHandler.class));
    }

    @Test
    public void testConnectDevice() throws Exception
    {
        // _device must be null if we want to try to connect it
        Whitebox.setInternalState(DeviceHandler.class, "_device", (Object) null);

        // information about the mocked connected device
        Map<String, PluggedDevice> pluggedDevices = new TreeMap<>();
        pluggedDevices.put(_devSerial, new PluggedDevice(_devSerial, _devPort,
                _swVersion, _hwVersion, _devType));

        mockStatic(Device.class);
        doReturn(pluggedDevices).when(Device.class, "getPluggedDevices");

        whenNew(Device.class).withArguments(any(), eq(_devPort)).thenReturn(_device);
        assertTrue(DeviceHandler.connectDevice());
        verify(_device).addListener(any(DeviceEventHandler.class));
    }

    @Test
    public void testDisconnectDevice() throws Exception
    {
        DeviceHandler.disconnectDevice();
        verify(_device).release();
    }

    @Test
    public void testReconnectDevice() throws Exception
    {
        mockStatic(DeviceHandler.class);
        doReturn(true).when(DeviceHandler.class, "connectDevice");
        doNothing().when(DeviceHandler.class, "connectModules");
        doReturn(false).when(DeviceHandler.class, "loadUsers");
        doReturn(false).when(DeviceHandler.class, "loadLastInventory");
        when(DeviceHandler.class, "reconnectDevice").thenCallRealMethod();

        DeviceHandler.reconnectDevice();

        verifyStatic();
        DeviceHandler.connectDevice();
        verifyStatic();
        DeviceHandler.connectModules();
        verifyStatic();
        DeviceHandler.loadUsers();
        verifyStatic();
        DeviceHandler.loadLastInventory();
        // SmartLogger warned twice: failure of loading authorized users and failure of loading the last inventory
        verify(_smartLogger, times(2)).warning(anyString());
    }

    @Test
    public void testGetDevice() throws Exception
    {
        assertEquals(DeviceHandler.getDevice(), _device);
    }

    @Test
    public void testConnectModulesDeviceNull() throws Exception
    {
        Whitebox.setInternalState(DeviceHandler.class, "_device", (Object) null);

        mockStatic(DeviceHandler.class);
        when(DeviceHandler.class, "connectModules").thenCallRealMethod();
        DeviceHandler.connectModules();

        verifyPrivate(DeviceHandler.class, never()).invoke("connectProbeIfEnabled");

        // warning that device is null
        verify(_smartLogger).warning(anyString());
    }

    @Test
    public void testConnectModulesNoAuthenticationModules() throws Exception
    {
        mockStatic(ConfManager.class);
        doReturn(null).when(ConfManager.class, "getDevFprMaster");
        doReturn(null).when(ConfManager.class, "getDevFprSlave");
        doReturn(null).when(ConfManager.class, "getDevBrMaster");
        doReturn(null).when(ConfManager.class, "getDevBrSlave");

        mockStatic(DeviceHandler.class);
        when(DeviceHandler.class, "connectModules").thenCallRealMethod();
        DeviceHandler.connectModules();

        verifyPrivate(DeviceHandler.class).invoke("connectProbeIfEnabled");
    }

    @Test
    public void testConnectModulesOneFingerprintReader() throws Exception
    {
        String fprMaster = "{478455-5478-111441}";

        mockStatic(ConfManager.class);
        doReturn(fprMaster).when(ConfManager.class, "getDevFprMaster");
        doReturn(null).when(ConfManager.class, "getDevFprSlave");
        doReturn(null).when(ConfManager.class, "getDevBrMaster");
        doReturn(null).when(ConfManager.class, "getDevBrSlave");

        mockStatic(FingerprintReader.class);
        doReturn(1).when(FingerprintReader.class, "connectFingerprintReader");

        mockStatic(DeviceHandler.class);
        when(DeviceHandler.class, "connectModules").thenCallRealMethod();
        DeviceHandler.connectModules();

        verify(_device).addFingerprintReader(fprMaster, true);
    }

    @Test
    public void testConnectModulesTwoFingerprintReaders() throws Exception
    {
        String fprMaster = "{478455-5478-111441}";
        String fprSlave = "{478455-5478-222442}";

        mockStatic(ConfManager.class);
        doReturn(fprMaster).when(ConfManager.class, "getDevFprMaster");
        doReturn(fprSlave).when(ConfManager.class, "getDevFprSlave");
        doReturn(null).when(ConfManager.class, "getDevBrMaster");
        doReturn(null).when(ConfManager.class, "getDevBrSlave");

        mockStatic(FingerprintReader.class);
        doReturn(2).when(FingerprintReader.class, "connectFingerprintReaders", 2);

        // the "add" of master reader must succeeds if we wanna verify that the second has been added
        doReturn(true).when(_device).addFingerprintReader(fprMaster, true);

        mockStatic(DeviceHandler.class);
        when(DeviceHandler.class, "connectModules").thenCallRealMethod();
        DeviceHandler.connectModules();

        verify(_device).addFingerprintReader(fprMaster, true);
        verify(_device).addFingerprintReader(fprSlave, false);
    }

    @Test
    public void testConnectModulesTwoBadgeReaders() throws Exception
    {
        String brMaster = "/dev/ttyUSB1";
        String brSlave = "/dev/ttyUSB2";

        mockStatic(ConfManager.class);
        doReturn(null).when(ConfManager.class, "getDevFprMaster");
        doReturn(null).when(ConfManager.class, "getDevFprSlave");
        doReturn(brMaster).when(ConfManager.class, "getDevBrMaster");
        doReturn(brSlave).when(ConfManager.class, "getDevBrSlave");

        // the "add" of master reader must succeeds if we wanna verify that the second has been added
        doReturn(true).when(_device).addBadgeReader(brMaster, true);
        doReturn(true).when(_device).addBadgeReader(brSlave, true);

        mockStatic(DeviceHandler.class);
        when(DeviceHandler.class, "connectModules").thenCallRealMethod();
        DeviceHandler.connectModules();

        verify(_device).addBadgeReader(brMaster, true);
        verify(_device).addBadgeReader(brSlave, false);
    }

    @Test
    public void testReloadTemperatureProbe() throws Exception
    {
        DeviceHandler.reloadTemperatureProbe();
        verify(_device).disconnectTemperatureProbe();
    }

    @Test
    public void testSetForwardingSerialPort() throws Exception
    {
        DeviceHandler.setForwardingSerialPort(true);
        assertTrue((boolean) Whitebox.getInternalState(DeviceHandler.class, "SERIAL_PORT_FORWARDING"));

        DeviceHandler.setForwardingSerialPort(false);
        assertFalse((boolean) Whitebox.getInternalState(DeviceHandler.class, "SERIAL_PORT_FORWARDING"));
    }

    @Test
    public void testIsForwardingSerialPort() throws Exception
    {
        Whitebox.setInternalState(DeviceHandler.class, "SERIAL_PORT_FORWARDING", true);
        assertTrue(DeviceHandler.isForwardingSerialPort());

        Whitebox.setInternalState(DeviceHandler.class, "SERIAL_PORT_FORWARDING", false);
        assertFalse(DeviceHandler.isForwardingSerialPort());
    }

    @Test
    public void testLoadAuthorizedUsersDeviceNull() throws Exception
    {
        Whitebox.setInternalState(DeviceHandler.class, "_device", (Object) null);
        assertFalse(DeviceHandler.loadUsers());
    }

    @Test
    public void testSortUsersFromDbFails() throws Exception
    {
        UsersService usersService = PowerMockito.mock(UsersService.class);
        DaoUser userRepo = PowerMockito.mock(DaoUser.class);
        
        // get a fake list of authorized and unregistered users from DB
        doReturn(false).when(userRepo).sortUsersFromDb(anyListOf(User.class), anyListOf(User.class));
        
        doReturn(usersService).when(_device).getUsersService();
        doReturn(userRepo).when(DbManager.class, "getDao", UserEntity.class);

        assertFalse(DeviceHandler.loadUsers());
        verify(_smartLogger).severe(anyString());
    }

    @Test
    public void testLoadLastInventoryNull() throws Exception
    {
        DaoInventory inventoryRepo = PowerMockito.mock(DaoInventory.class);
        doReturn(null).when(inventoryRepo).getLastInventory();
        doReturn(inventoryRepo).when(DbManager.class, "getDao", InventoryEntity.class);

        assertFalse(DeviceHandler.loadLastInventory());
    }

    @Test
    public void testLoadLastInventory() throws Exception
    {
        Inventory lastInv = PowerMockito.mock(Inventory.class);
        DaoInventory inventoryRepo = PowerMockito.mock(DaoInventory.class);
        doReturn(lastInv).when(inventoryRepo).getLastInventory();
        doReturn(inventoryRepo).when(DbManager.class, "getDao", InventoryEntity.class);

        assertTrue(DeviceHandler.loadLastInventory());
        verify(_device).setLastInventory(lastInv);
    }

    @Test
    public void testEventHandlerDeviceDisconnected() throws Exception
    {
        PowerMockito.mockStatic(DeviceHandler.class);

        _eventHandler.deviceDisconnected();

        PowerMockito.verifyStatic();
        DeviceHandler.reconnectDevice();
        PowerMockito.verifyStatic();
        SmartServer.sendAllClients(EventCode.DEVICE_DISCONNECTED);
    }

    @Test
    public void testEventHandlerDoorOpened() throws Exception
    {
        _eventHandler.doorOpened();

        PowerMockito.verifyStatic();
        SmartServer.sendAllClients(EventCode.DOOR_OPENED);
    }

    @Test
    public void testEventHandlerDoorClosed() throws Exception
    {
        _eventHandler.doorClosed();

        PowerMockito.verifyStatic();
        SmartServer.sendAllClients(EventCode.DOOR_CLOSED);
    }

    @Test
    public void testEventHandlerDoorOpenDelay() throws Exception
    {
        _eventHandler.doorOpenDelay();

        PowerMockito.verifyStatic();
        SmartServer.sendAllClients(EventCode.DOOR_OPEN_DELAY);
    }

    @Test
    public void testEventHandlerScanStarted() throws Exception
    {
        _eventHandler.scanStarted();

        PowerMockito.verifyStatic();
        SmartServer.sendAllClients(EventCode.SCAN_STARTED);
    }

    @Test
    public void testEventHandlerScanCancelledByHost() throws Exception
    {
        _eventHandler.scanCancelledByHost();

        PowerMockito.verifyStatic();
        SmartServer.sendAllClients(EventCode.SCAN_CANCELLED_BY_HOST);
    }

    @Test
    public void testEventHandlerScanCompleted() throws Exception
    {
        DaoInventory inventoryRepo = PowerMockito.mock(DaoInventory.class);
        doReturn(inventoryRepo).when(DbManager.class, "getDao", InventoryEntity.class);

        _eventHandler.scanCompleted();

        PowerMockito.verifyStatic();
        SmartServer.sendAllClients(EventCode.SCAN_COMPLETED);

        verify(inventoryRepo).persist(any(Inventory.class));
    }

    @Test
    public void testEventHandlerScanFailed() throws Exception
    {
        _eventHandler.scanFailed();

        PowerMockito.verifyStatic();
        SmartServer.sendAllClients(EventCode.SCAN_FAILED);
    }

    @Test
    public void testEventHandlerTagAdded() throws Exception
    {
        String tagUid = "3001234567";
        _eventHandler.tagAdded(tagUid);

        PowerMockito.verifyStatic();
        SmartServer.sendAllClients(EventCode.TAG_ADDED, tagUid);
    }

    @Test
    public void testEventHandlerAuthenticationSuccess() throws Exception
    {
        DaoAuthentication authenticationRepo = PowerMockito.mock(DaoAuthentication.class);
        doReturn(authenticationRepo).when(DbManager.class, "getDao", AuthenticationEntity.class);

        AccessType accessType = AccessType.BADGE;
        User user = new User("Vincent", GrantType.MASTER);

        _eventHandler.authenticationSuccess(user, accessType, true);

        PowerMockito.verifyStatic();
        SmartServer.sendAllClients(EventCode.AUTHENTICATION_SUCCESS, user.serialize(), accessType.name(),
                String.valueOf(true));

        verify(authenticationRepo).persist(user, accessType);
    }

    @Test
    public void testEventHandlerAuthenticationFailure() throws Exception
    {
        AccessType accessType = AccessType.BADGE;
        User user = new User("Vincent", GrantType.MASTER);

        _eventHandler.authenticationFailure(user, accessType, true);

        PowerMockito.verifyStatic();
        SmartServer.sendAllClients(EventCode.AUTHENTICATION_FAILURE, user.serialize(), accessType.name(),
                String.valueOf(true));
    }

    @Test
    public void testEventHandlerFingerTouched() throws Exception
    {
        _eventHandler.fingerTouched(true);

        PowerMockito.verifyStatic();
        SmartServer.sendAllClients(EventCode.FINGER_TOUCHED, String.valueOf(true));

        _eventHandler.fingerTouched(false);

        PowerMockito.verifyStatic();
        SmartServer.sendAllClients(EventCode.FINGER_TOUCHED, String.valueOf(false));
    }

    @Test
    public void testEventHandlerFingerEnrollmentSample() throws Exception
    {
        _eventHandler.fingerprintEnrollmentSample((byte) 5);

        PowerMockito.verifyStatic();
        SmartServer.sendAllClients(EventCode.ENROLLMENT_SAMPLE, String.valueOf(5));

        _eventHandler.fingerprintEnrollmentSample((byte) 6);

        PowerMockito.verifyStatic();
        SmartServer.sendAllClients(EventCode.ENROLLMENT_SAMPLE, String.valueOf(6));
    }

    @Test
    public void testEventHandlerBadgeScanned() throws Exception
    {
        _eventHandler.badgeScanned("ABCDEFG");

        PowerMockito.verifyStatic();
        SmartServer.sendAllClients(EventCode.BADGE_SCANNED, "ABCDEFG");

        _eventHandler.badgeScanned("123456");

        PowerMockito.verifyStatic();
        SmartServer.sendAllClients(EventCode.BADGE_SCANNED, "123456");
    }

    @Test
    public void testEventHandlerTemperatureMeasure() throws Exception
    {
        _eventHandler.temperatureMeasure(4.56);

        PowerMockito.verifyStatic();
        SmartServer.sendAllClients(EventCode.TEMPERATURE_MEASURE, "4.56");

        _eventHandler.temperatureMeasure(5.0);

        PowerMockito.verifyStatic();
        SmartServer.sendAllClients(EventCode.TEMPERATURE_MEASURE, "5.0");
    }

    @Test
    public void testEventHandlerLightingStartedNoTagsLeft() throws Exception
    {
        List<String> tagsLeft = Arrays.asList();
        _eventHandler.lightingStarted(tagsLeft);

        PowerMockito.verifyStatic();
        SmartServer.sendAllClients(EventCode.LIGHTING_STARTED);
    }

    @Test
    public void testEventHandlerLightingStarted() throws Exception
    {
        String tag1 = "3001234567", tag2 = "3002345678";
        List<String> tagsLeft = Arrays.asList(tag1, tag2);
        _eventHandler.lightingStarted(tagsLeft);

        PowerMockito.verifyStatic();
        SmartServer.sendAllClients(EventCode.LIGHTING_STARTED, tag1, tag2);
    }

    @Test
    public void testEventHandlerLightingStopped() throws Exception
    {
        _eventHandler.lightingStopped();

        PowerMockito.verifyStatic();
        SmartServer.sendAllClients(EventCode.LIGHTING_STOPPED);
    }

    @Test
    public void testEventHandlerDeviceStatusChanged() throws Exception
    {
        _eventHandler.deviceStatusChanged(DeviceStatus.READY);

        PowerMockito.verifyStatic();
        SmartServer.sendAllClients(EventCode.STATUS_CHANGED, DeviceStatus.READY.name());

        _eventHandler.deviceStatusChanged(DeviceStatus.SCANNING);

        PowerMockito.verifyStatic();
        SmartServer.sendAllClients(EventCode.STATUS_CHANGED, DeviceStatus.SCANNING.name());
    }


    @Test
    public void testEventHandlerFlashingProgress() throws Exception
    {
        _eventHandler.flashingProgress(2, 200);

        PowerMockito.verifyStatic();
        SmartServer.sendAllClients(EventCode.FLASHING_PROGRESS, "2", "200");
    }
}