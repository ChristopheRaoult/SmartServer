package com.spacecode.smartserver.database.dao;

import com.spacecode.sdk.network.alert.AlertType;
import com.spacecode.smartserver.SmartServer;
import com.spacecode.smartserver.database.DbManager;
import com.spacecode.smartserver.database.entity.AlertTypeEntity;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mockito;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;

import static org.junit.Assert.*;
import static org.powermock.api.mockito.PowerMockito.doReturn;
import static org.powermock.api.mockito.PowerMockito.mockStatic;

/**
 * JUnit "AlertTypeRepository" testing class.
 */
@RunWith(PowerMockRunner.class)
@PrepareForTest({ SmartServer.class, DbManager.class })
public class DaoAlertTypeTest
{
    @Before
    public void setUp() throws Exception
    {
        // required to avoid an initialization exception, as SmartServer has some static initialization
        mockStatic(SmartServer.class);

        mockStatic(DbManager.class, Mockito.CALLS_REAL_METHODS);
    }

    @After
    public void tearDown() throws Exception
    {
        DbManager.close();
    }

    @Test
    public void testAsAlertType() throws Exception
    {
        // create an in-memory db using H2, for the purpose of this test
        doReturn("jdbc:h2:mem:asAlertType").when(DbManager.class, "getConnectionString");
        assertTrue(DbManager.initializeDatabase());

        DaoAlertType gtRepo = (DaoAlertType) DbManager.getDao(AlertTypeEntity.class);

        assertEquals(
                DaoAlertType.asAlertType(gtRepo.fromAlertType(AlertType.DEVICE_DISCONNECTED)),
                AlertType.DEVICE_DISCONNECTED);
        assertEquals(
                DaoAlertType.asAlertType(gtRepo.fromAlertType(AlertType.DOOR_OPEN_DELAY)),
                AlertType.DOOR_OPEN_DELAY);
        assertEquals(
                DaoAlertType.asAlertType(gtRepo.fromAlertType(AlertType.TEMPERATURE)),
                AlertType.TEMPERATURE);
        assertEquals(
                DaoAlertType.asAlertType(gtRepo.fromAlertType(AlertType.THIEF_FINGER)),
                AlertType.THIEF_FINGER);

        assertNull(DaoAlertType.asAlertType(null));
    }

    @Test
    public void testFromAlertType() throws Exception
    {
        // create an in-memory db using H2, for the purpose of this test
        doReturn("jdbc:h2:mem:fromAlertType").when(DbManager.class, "getConnectionString");
        assertTrue(DbManager.initializeDatabase());

        DaoAlertType gtRepo = (DaoAlertType) DbManager.getDao(AlertTypeEntity.class);

        assertNotNull(gtRepo.fromAlertType(AlertType.DEVICE_DISCONNECTED));
        assertNotNull(gtRepo.fromAlertType(AlertType.DOOR_OPEN_DELAY));
        assertNotNull(gtRepo.fromAlertType(AlertType.TEMPERATURE));
        assertNotNull(gtRepo.fromAlertType(AlertType.THIEF_FINGER));

        assertNull(gtRepo.fromAlertType(null));
    }
}