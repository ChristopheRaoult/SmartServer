package com.spacecode.smartserver.database;

import com.j256.ormlite.dao.Dao;
import com.j256.ormlite.dao.DaoManager;
import com.j256.ormlite.jdbc.JdbcPooledConnectionSource;
import com.j256.ormlite.support.ConnectionSource;
import com.spacecode.sdk.device.Device;
import com.spacecode.smartserver.SmartServer;
import com.spacecode.smartserver.database.entity.DeviceEntity;
import com.spacecode.smartserver.database.entity.Entity;
import com.spacecode.smartserver.database.repository.DeviceRepository;
import com.spacecode.smartserver.database.repository.Repository;
import com.spacecode.smartserver.helper.ConfManager;
import com.spacecode.smartserver.helper.DeviceHandler;
import com.spacecode.smartserver.helper.SmartLogger;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;
import org.powermock.reflect.Whitebox;

import java.lang.reflect.Method;
import java.sql.SQLException;

import static org.junit.Assert.*;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyString;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.*;
import static org.powermock.api.mockito.PowerMockito.doNothing;
import static org.powermock.api.mockito.PowerMockito.doReturn;
import static org.powermock.api.mockito.PowerMockito.*;
import static org.powermock.api.mockito.PowerMockito.when;

/**
 * JUnit "DbManager" testing class.
 */
@RunWith(PowerMockRunner.class)
@PrepareForTest({ DbManager.class, ConfManager.class, SmartServer.class, JdbcPooledConnectionSource.class,
        SmartLogger.class, DaoManager.class, DeviceHandler.class, Device.class, DeviceEntity.class, Repository.class })
public class DbManagerTest
{
    private String _defaultConnectionString = Whitebox.getInternalState(DbManager.class, "CONNECTION_STRING");
    private JdbcPooledConnectionSource _connectionSource;

    @Before
    public void setUp()
    {
        mockStatic(SmartServer.class);
        mockStatic(ConfManager.class);

        _connectionSource = PowerMockito.mock(JdbcPooledConnectionSource.class);
    }

    @Test
    public void testGetConnectionStringNullHost() throws Exception
    {
        doReturn(null).when(ConfManager.class, "getDbHost");
        assertEquals(Whitebox.invokeMethod(DbManager.class, "getConnectionString"), _defaultConnectionString);
    }

    @Test
    public void testGetConnectionStringNullDbName() throws Exception
    {
        doReturn(null).when(ConfManager.class, "getDbName");
        assertEquals(Whitebox.invokeMethod(DbManager.class, "getConnectionString"), _defaultConnectionString);
    }

    @Test
    public void testGetConnectionStringNullDbms() throws Exception
    {
        doReturn(null).when(ConfManager.class, "getDbDbms");
        assertEquals(Whitebox.invokeMethod(DbManager.class, "getConnectionString"), _defaultConnectionString);
    }

    @Test
    public void testGetConnectionStringNullUser() throws Exception
    {
        doReturn(null).when(ConfManager.class, "getDbUser");
        assertEquals(Whitebox.invokeMethod(DbManager.class, "getConnectionString"), _defaultConnectionString);
    }

    @Test
    public void testGetConnectionStringUnknownDbms() throws Exception
    {
        String dbHost = "192.168.1.9";
        String dbName = "smartserver";
        String dbUser = "spacecode";
        String dbDbms = "unknown_dbms";

        doReturn(dbHost).when(ConfManager.class, "getDbHost");
        doReturn(dbName).when(ConfManager.class, "getDbName");
        doReturn(dbUser).when(ConfManager.class, "getDbUser");
        doReturn(dbDbms).when(ConfManager.class, "getDbDbms");
        doReturn(null).when(ConfManager.class, "getDbPort");

        assertEquals(Whitebox.invokeMethod(DbManager.class, "getConnectionString"), _defaultConnectionString);
    }

    @Test
    public void testGetConnectionStringMySqlDefaultPort() throws Exception
    {
        String dbHost = "192.168.1.9";
        String dbName = "smartserver";
        String dbUser = "spacecode";
        String dbDbms = "mysql";

        doReturn(dbHost).when(ConfManager.class, "getDbHost");
        doReturn(dbName).when(ConfManager.class, "getDbName");
        doReturn(dbUser).when(ConfManager.class, "getDbUser");
        doReturn(dbDbms).when(ConfManager.class, "getDbDbms");
        doReturn(null).when(ConfManager.class, "getDbPort");

        assertEquals(
                Whitebox.invokeMethod(DbManager.class, "getConnectionString"),
                String.format("jdbc:%s://%s:%s/%s", dbDbms, dbHost, 3306, dbName)
        );
    }

    @Test
    public void testGetConnectionStringPostgreSqlDefaultPort() throws Exception
    {
        String dbHost = "192.168.1.9";
        String dbName = "smartserver";
        String dbUser = "spacecode";
        String dbDbms = "postgresql";

        doReturn(dbHost).when(ConfManager.class, "getDbHost");
        doReturn(dbName).when(ConfManager.class, "getDbName");
        doReturn(dbUser).when(ConfManager.class, "getDbUser");
        doReturn(dbDbms).when(ConfManager.class, "getDbDbms");
        doReturn(null).when(ConfManager.class, "getDbPort");

        assertEquals(
                Whitebox.invokeMethod(DbManager.class, "getConnectionString"),
                String.format("jdbc:%s://%s:%s/%s", dbDbms, dbHost, 5432, dbName)
        );
    }

    @Test
    public void testGetConnectionStringSqlServer() throws Exception
    {
        String dbHost = "192.168.1.9";
        String dbName = "smartserver";
        String dbUser = "spacecode";
        String dbDbms = "sqlserver";
        String dbPort = "7890";

        doReturn(dbHost).when(ConfManager.class, "getDbHost");
        doReturn(dbName).when(ConfManager.class, "getDbName");
        doReturn(dbUser).when(ConfManager.class, "getDbUser");
        doReturn(dbDbms).when(ConfManager.class, "getDbDbms");
        doReturn(dbPort).when(ConfManager.class, "getDbPort");

        assertEquals(
                Whitebox.invokeMethod(DbManager.class, "getConnectionString"),
                String.format("jdbc:%s://%s:%s;databaseName=%s;", dbDbms, dbHost, dbPort, dbName)
        );
    }

    @Test
    public void testInitializeDatabaseFailure() throws Exception
    {
        mockStatic(DbManager.class);

        String dbHost = "192.168.1.9";
        String dbName = "smartserver";
        String dbUser = "spacecode";
        String dbDbms = "sqlserver";
        String dbPort = "7890";
        String dbPassword = "password";

        doReturn(dbHost).when(ConfManager.class, "getDbHost");
        doReturn(dbName).when(ConfManager.class, "getDbName");
        doReturn(dbUser).when(ConfManager.class, "getDbUser");
        doReturn(dbDbms).when(ConfManager.class, "getDbDbms");
        doReturn(dbPort).when(ConfManager.class, "getDbPort");
        doReturn(dbPassword).when(ConfManager.class, "getDbPassword");

        whenNew(JdbcPooledConnectionSource.class).withArguments(anyString(), anyString(), anyString())
                .thenThrow(new SQLException());

        when(DbManager.class, "getConnectionString").thenCallRealMethod();
        when(DbManager.class, "initializeDatabase").thenCallRealMethod();

        assertFalse(DbManager.initializeDatabase());

        verifyNew(JdbcPooledConnectionSource.class).withArguments(
                String.format("jdbc:%s://%s:%s;databaseName=%s;", dbDbms, dbHost, dbPort, dbName),
                dbUser,
                dbPassword
        );

        verifyPrivate(DbManager.class).invoke("getConnectionString");
        verifyPrivate(DbManager.class, never()).invoke("createModelIfNotExists");
    }

    @Test
    public void testInitializeDatabase() throws Exception
    {
        mockStatic(DbManager.class);

        String dbHost = "192.168.1.9";
        String dbName = "smartserver";
        String dbUser = "spacecode";
        String dbDbms = "sqlserver";
        String dbPort = "7890";
        String dbPassword = "password";

        doReturn(dbHost).when(ConfManager.class, "getDbHost");
        doReturn(dbName).when(ConfManager.class, "getDbName");
        doReturn(dbUser).when(ConfManager.class, "getDbUser");
        doReturn(dbDbms).when(ConfManager.class, "getDbDbms");
        doReturn(dbPort).when(ConfManager.class, "getDbPort");
        doReturn(dbPassword).when(ConfManager.class, "getDbPassword");

        whenNew(JdbcPooledConnectionSource.class).withAnyArguments().thenReturn(_connectionSource);

        when(DbManager.class, "getConnectionString").thenCallRealMethod();
        when(DbManager.class, "initializeDatabase").thenCallRealMethod();
        doNothing().when(DbManager.class, "createModelIfNotExists");

        assertTrue(DbManager.initializeDatabase());

        verifyNew(JdbcPooledConnectionSource.class).withArguments(
                String.format("jdbc:%s://%s:%s;databaseName=%s;", dbDbms, dbHost, dbPort, dbName),
                dbUser,
                dbPassword
        );

        verify(_connectionSource).setMaxConnectionAgeMillis(10 * 60 * 1000);
        verifyPrivate(DbManager.class).invoke("getConnectionString");
        verifyPrivate(DbManager.class).invoke("createModelIfNotExists");
    }

    @Test
    public void testClose() throws Exception
    {
        mockStatic(DbManager.class);
        doReturn(true).when(_connectionSource).isOpen();
        Whitebox.setInternalState(DbManager.class, "_pooledConnectionSrc", _connectionSource);
        when(DbManager.class, "close").thenCallRealMethod();

        DbManager.close();
        verify(_connectionSource).isOpen();
        verify(_connectionSource).close();
    }

    @Test
    public void testCreateModelIfNotExists() throws Exception
    {
        mockStatic(DaoManager.class);
        mockStatic(DbManager.class);
        Dao dao = PowerMockito.mock(Dao.class);

        doReturn(true).when(dao).isTableExists();
        // need to use a Method instance to avoid a bug of PowerMock with overloaded methods
        Method createDaoMethod = Whitebox.getMethod(DaoManager.class, "createDao", ConnectionSource.class, Class.class);
        doReturn(dao).when(DaoManager.class, createDaoMethod).withArguments(eq(_connectionSource), any(Entity.class));
        when(DbManager.class, "createModelIfNotExists").thenCallRealMethod();
        Whitebox.setInternalState(DbManager.class, "_pooledConnectionSrc", _connectionSource);

        Whitebox.invokeMethod(DbManager.class, "createModelIfNotExists");

        // 16 "entities" (tables) at the time this test is written (27/02/2015)
        verifyStatic(times(16));
        DaoManager.createDao(eq(_connectionSource), any(Class.class));
        verify(dao, times(16)).isTableExists();
    }

    @Test
    public void testGetDevEntity() throws Exception
    {
        mockStatic(DbManager.class);

        String serialNumber = "AA770201";
        Device device = PowerMockito.mock(Device.class);
        doReturn(serialNumber).when(device).getSerialNumber();

        mockStatic(DeviceHandler.class);
        doReturn(device).when(DeviceHandler.class, "getDevice");

        DeviceEntity devEntity = PowerMockito.mock(DeviceEntity.class);
        DeviceRepository devRepo = PowerMockito.mock(DeviceRepository.class);
        doReturn(devEntity).when(devRepo).getEntityBy(DeviceEntity.SERIAL_NUMBER, serialNumber);
        doReturn(devRepo).when(DbManager.class, "getRepository", DeviceEntity.class);

        Whitebox.setInternalState(DbManager.class, "_deviceEntity", (Object) null);
        when(DbManager.class, "getDevEntity").thenCallRealMethod();

        assertEquals(DbManager.getDevEntity(), devEntity);

        verifyStatic();
        DbManager.getRepository(DeviceEntity.class);
        verify(devRepo).getEntityBy(DeviceEntity.SERIAL_NUMBER, serialNumber);
    }

    @Test
    public void testGetDevEntityNoResult() throws Exception
    {
        mockStatic(DbManager.class);

        String serialNumber = "AA770201";
        Device device = PowerMockito.mock(Device.class);
        doReturn(serialNumber).when(device).getSerialNumber();

        mockStatic(DeviceHandler.class);
        doReturn(device).when(DeviceHandler.class, "getDevice");

        DeviceRepository devRepo = PowerMockito.mock(DeviceRepository.class);
        doReturn(null).when(devRepo).getEntityBy(DeviceEntity.SERIAL_NUMBER, serialNumber);
        doReturn(devRepo).when(DbManager.class, "getRepository", DeviceEntity.class);

        Whitebox.setInternalState(DbManager.class, "_deviceEntity", (Object) null);
        when(DbManager.class, "getDevEntity").thenCallRealMethod();

        assertNull(DbManager.getDevEntity());

        verifyStatic();
        DbManager.getRepository(DeviceEntity.class);
        verify(devRepo).getEntityBy(DeviceEntity.SERIAL_NUMBER, serialNumber);
    }

    @Test
    public void testCreateDeviceIfNotExists() throws Exception
    {
        mockStatic(DbManager.class);
        when(DbManager.class, "createDeviceIfNotExists", anyString()).thenCallRealMethod();
        Whitebox.setInternalState(DbManager.class, "_deviceEntity", (Object) null);

        String serialNumber = "AA777201";
        doReturn(null).when(DbManager.class, "getDevEntity");

        DeviceRepository devRepo = PowerMockito.mock(DeviceRepository.class);
        doReturn(devRepo).when(DbManager.class, "getRepository", DeviceEntity.class);

        DeviceEntity devEntity = PowerMockito.mock(DeviceEntity.class);
        whenNew(DeviceEntity.class).withArguments(serialNumber).thenReturn(devEntity);

        DbManager.createDeviceIfNotExists(serialNumber);

        verifyStatic();
        DbManager.getDevEntity();
        verify(devRepo).insert(devEntity);
    }
}