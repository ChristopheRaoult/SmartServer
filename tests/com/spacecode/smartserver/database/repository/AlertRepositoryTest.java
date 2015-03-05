package com.spacecode.smartserver.database.repository;

import com.j256.ormlite.dao.Dao;
import com.spacecode.sdk.network.alert.AlertType;
import com.spacecode.smartserver.SmartServer;
import com.spacecode.smartserver.database.DbManager;
import com.spacecode.smartserver.database.entity.AlertEntity;
import com.spacecode.smartserver.database.entity.AlertTypeEntity;
import com.spacecode.smartserver.database.entity.DeviceEntity;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mockito;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;

import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.powermock.api.mockito.PowerMockito.doReturn;
import static org.powermock.api.mockito.PowerMockito.mockStatic;

/**
 * JUnit "AlertRepository" testing class.
 */
@RunWith(PowerMockRunner.class)
@PrepareForTest({ SmartServer.class, DbManager.class })
public class AlertRepositoryTest
{
    private DeviceEntity _devEntity;

    @Before
    public void setUp() throws Exception
    {
        // required to avoid an initialization exception, as SmartServer has some static initialization
        mockStatic(SmartServer.class);

        _devEntity = new DeviceEntity("AA7770201");

        mockStatic(DbManager.class, Mockito.CALLS_REAL_METHODS);
        doReturn(_devEntity).when(DbManager.class, "getDevEntity");
    }

    @After
    public void tearDown() throws Exception
    {
        DbManager.close();
    }

    @Test
    public void testDelete() throws Exception
    {

    }

    @Test
    public void testGetEnabledAlerts() throws Exception
    {
        // create an in-memory db using H2, for the purpose of this test
        doReturn("jdbc:h2:mem:enabledAlerts").when(DbManager.class, "getConnectionString");
        assertTrue(DbManager.initializeDatabase());

        AlertRepository alertRepo = (AlertRepository) DbManager.getRepository(AlertEntity.class);

        // get the alert types
        AlertTypeRepository atRepo = (AlertTypeRepository) DbManager.getRepository(AlertTypeEntity.class);
        AlertTypeEntity ateThiefFinger = atRepo.fromAlertType(AlertType.THIEF_FINGER);
        AlertTypeEntity ateDevDisconnected = atRepo.fromAlertType(AlertType.DEVICE_DISCONNECTED);
        AlertTypeEntity ateDoorDelay = atRepo.fromAlertType(AlertType.DOOR_OPEN_DELAY);


        // get the DAO and create some fixtures
        Dao<AlertEntity, Integer> daoAlert = DbManager.getDao(AlertEntity.class);

        daoAlert.create(
            new AlertEntity(
                    ateThiefFinger,
                    "vincent.guilloux@spacecode.com",
                    "Enabled Alert 1",
                    "",
                    true
            )
        );

        daoAlert.create(
            new AlertEntity(
                    ateThiefFinger,
                    "vincent.guilloux@spacecode.com",
                    "Disabled Alert 1",
                    "",
                    false
            )
        );

        daoAlert.create(
                new AlertEntity(
                        ateDevDisconnected,
                        "vincent.guilloux@spacecode.com",
                        "Enabled Alert 2",
                        "",
                        true
                )
        );

        daoAlert.create(
                new AlertEntity(
                        ateDoorDelay,
                        "vincent.guilloux@spacecode.com",
                        "Disabled Alert 2",
                        "",
                        false
                )
        );

        List<AlertEntity> alertsThief = alertRepo.getEnabledAlerts(ateThiefFinger);
        List<AlertEntity> alertsDisco = alertRepo.getEnabledAlerts(ateDevDisconnected);
        List<AlertEntity> alertsDoorDelay = alertRepo.getEnabledAlerts(ateDoorDelay);

        assertEquals(alertRepo.getEnabledAlerts(null).size(), 0);
        assertEquals(alertsThief.size(), 1);
        assertEquals(alertsDisco.size(), 1);
        assertEquals(alertsDoorDelay.size(), 0);
    }

    @Test
    public void testPersist() throws Exception
    {

    }
}