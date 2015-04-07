package com.spacecode.smartserver.database.dao;

import com.j256.ormlite.dao.Dao;
import com.spacecode.sdk.network.alert.AlertTemperature;
import com.spacecode.sdk.network.alert.AlertType;
import com.spacecode.smartserver.SmartServer;
import com.spacecode.smartserver.database.DbManager;
import com.spacecode.smartserver.database.entity.AlertEntity;
import com.spacecode.smartserver.database.entity.AlertTemperatureEntity;
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

import static org.junit.Assert.*;
import static org.powermock.api.mockito.PowerMockito.doReturn;
import static org.powermock.api.mockito.PowerMockito.mockStatic;

/**
 * JUnit "AlertRepository" testing class.
 */
@RunWith(PowerMockRunner.class)
@PrepareForTest({ SmartServer.class, DbManager.class })
public class DaoAlertTest
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
        // create an in-memory db using H2, for the purpose of this test
        doReturn("jdbc:h2:mem:deleteAlert").when(DbManager.class, "getConnectionString");
        assertTrue(DbManager.initializeDatabase());

        DaoAlert alertRepo = (DaoAlert) DbManager.getDao(AlertEntity.class);
        assertFalse(alertRepo.deleteEntity((AlertEntity) null));

        // get the alert types
        DaoAlertType atRepo = (DaoAlertType) DbManager.getDao(AlertTypeEntity.class);
        AlertTypeEntity ateThiefFinger = atRepo.fromAlertType(AlertType.THIEF_FINGER);

        // get the DAO and create some fixtures
        Dao<AlertEntity, Integer> daoAlert = DbManager.getDao(AlertEntity.class);
        Dao<AlertTemperatureEntity, Integer> daoAlertTemp = DbManager.getDao(AlertTemperatureEntity.class);

        daoAlert.create(
                new AlertEntity(
                        ateThiefFinger,
                        "vincent.guilloux@spacecode.com",
                        "Enabled Alert 1",
                        "",
                        true
                )
        );

        assertEquals(daoAlert.countOf(), 1);
        AlertEntity newAlert = daoAlert.queryForAll().get(0);
        assertTrue(alertRepo.deleteEntity(newAlert));
        assertEquals(daoAlert.countOf(), 0);

        // Now try to remove an alert of TEMPERATURE type and check the "AlertTemperatureEntity" is well removed
        assertEquals(daoAlertTemp.countOf(), 0);
        alertRepo.persist(new AlertTemperature("vincent.guilloux@spacecode.com", "subject", "", true, 5.5, 9.5));
        assertEquals(daoAlertTemp.countOf(), 1);
        assertEquals(daoAlert.countOf(), 1);
        AlertEntity newAlertTemperature = daoAlert.queryForAll().get(0);
        assertTrue(alertRepo.deleteEntity(newAlertTemperature));
        assertEquals(daoAlertTemp.countOf(), 0);
        assertEquals(daoAlert.countOf(), 0);

    }

    @Test
    public void testGetEnabledAlerts() throws Exception
    {
        // create an in-memory db using H2, for the purpose of this test
        doReturn("jdbc:h2:mem:enabledAlerts").when(DbManager.class, "getConnectionString");
        assertTrue(DbManager.initializeDatabase());

        DaoAlert alertRepo = (DaoAlert) DbManager.getDao(AlertEntity.class);

        // get the alert types
        DaoAlertType atRepo = (DaoAlertType) DbManager.getDao(AlertTypeEntity.class);
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

    /**
     * Test both "Create if not existing" and "Update if existing" behaviors.
     *
     * @throws Exception
     */
    @Test
    public void testPersist() throws Exception
    {
        // create an in-memory db using H2, for the purpose of this test
        doReturn("jdbc:h2:mem:persistAlert").when(DbManager.class, "getConnectionString");
        assertTrue(DbManager.initializeDatabase());

        DaoAlert alertRepo = (DaoAlert) DbManager.getDao(AlertEntity.class);
        Dao<AlertEntity, Integer> daoAlert = DbManager.getDao(AlertEntity.class);
        Dao<AlertTemperatureEntity, Integer> daoAlertTemp = DbManager.getDao(AlertTemperatureEntity.class);

        String to = "vincent.guilloux@spacecode.com";
        String cc = "christophe.raoult@spacecode.com, pavlo@spacecode.com";
        String bcc = "eric.gout@spacecode.com";
        String eSubject = "Alert: Temperature out of bounds!";
        String eContent = "An alert has been raised: The last temperature measure is out of bounds.";
        boolean enabled = true;
        double tMin = 4.5;
        double tMax = 8.5;

        /** Test "Create if not existing" behavior */
        AlertTemperature newAlert = new AlertTemperature(to, cc, bcc, eSubject, eContent, enabled, tMin, tMax);

        assertEquals(daoAlert.countOf(), 0);
        assertEquals(daoAlertTemp.countOf(), 0);
        assertTrue(alertRepo.persist(newAlert));
        assertEquals(daoAlert.countOf(), 1);
        assertEquals(daoAlertTemp.countOf(), 1);

        // get the two parts of the alert to check its data
        AlertEntity newAlInDb = daoAlert.queryForAll().get(0);
        AlertTemperatureEntity newAlTempInDb = daoAlertTemp.queryForAll().get(0);
        // check the "Alert" part
        assertEquals(DaoAlertType.asAlertType(newAlInDb.getAlertType()), AlertType.TEMPERATURE);
        assertEquals(newAlInDb.getToList(), to);
        assertEquals(newAlInDb.getCcList(), cc);
        assertEquals(newAlInDb.getBccList(), bcc);
        assertEquals(newAlInDb.getEmailSubject(), eSubject);
        assertEquals(newAlInDb.getEmailContent(), eContent);
        assertEquals(newAlInDb.isEnabled(), enabled);
        // check the "AlertTemperature" part
        assertEquals(newAlTempInDb.getAlert().getId(), newAlInDb.getId());
        assertEquals(newAlTempInDb.getTemperatureMin(), tMin, 0);
        assertEquals(newAlTempInDb.getTemperatureMax(), tMax, 0);

        /** Test "Update if existing" behavior */
        String newTo = "vincent.guilloux@spacecode.com, arthur.herbreteau@spacecode.com";
        double newTmin = 4.0;
        boolean newEnabled = false;
        AlertTemperature existingAlert =
                new AlertTemperature(newAlInDb.getId(), newTo, cc, bcc, eSubject, eContent, newEnabled, newTmin, tMax);
        assertTrue(alertRepo.persist(existingAlert));
        // make sure no new entity has been created
        assertEquals(daoAlert.countOf(), 1);
        assertEquals(daoAlertTemp.countOf(), 1);

        // get the two parts of the alert to check its data
        newAlInDb = daoAlert.queryForAll().get(0);
        newAlTempInDb = daoAlertTemp.queryForAll().get(0);
        // check the "Alert" part
        assertEquals(DaoAlertType.asAlertType(newAlInDb.getAlertType()), AlertType.TEMPERATURE);
        assertEquals(newAlInDb.getToList(), newTo);
        assertEquals(newAlInDb.getCcList(), cc);
        assertEquals(newAlInDb.getBccList(), bcc);
        assertEquals(newAlInDb.getEmailSubject(), eSubject);
        assertEquals(newAlInDb.getEmailContent(), eContent);
        assertEquals(newAlInDb.isEnabled(), newEnabled);
        // check the "AlertTemperature" part
        assertEquals(newAlTempInDb.getAlert().getId(), newAlInDb.getId());
        assertEquals(newAlTempInDb.getTemperatureMin(), newTmin, 0);
        assertEquals(newAlTempInDb.getTemperatureMax(), tMax, 0);
    }
}