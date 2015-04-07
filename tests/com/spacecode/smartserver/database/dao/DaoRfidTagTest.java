package com.spacecode.smartserver.database.dao;

import com.j256.ormlite.dao.Dao;
import com.spacecode.smartserver.SmartServer;
import com.spacecode.smartserver.database.DbManager;
import com.spacecode.smartserver.database.entity.RfidTagEntity;
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
 * JUnit "RfidTagRepository" testing class.
 */
@RunWith(PowerMockRunner.class)
@PrepareForTest({ SmartServer.class, DbManager.class })
public class DaoRfidTagTest
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
        DbManager.close();
    }

    @Test
    public void testCreateIfNotExists() throws Exception
    {
        // create an in-memory db using H2, for the purpose of this test
        doReturn("jdbc:h2:mem:createTag").when(DbManager.class, "getConnectionString");
        assertTrue(DbManager.initializeDatabase());

        // get the dao
        DaoRfidTag tagRepo = (DaoRfidTag) DbManager.getDao(RfidTagEntity.class);

        // check the defensive programming against invalid parameter
        assertNull(tagRepo.getByUid(null));
        assertNull(tagRepo.getByUid(""));
        assertNull(tagRepo.getByUid("   "));

        // get the DAO
        Dao<RfidTagEntity, Integer> daoTag = DbManager.getDao(RfidTagEntity.class);

        // if the tag already exist, check it's just returned
        String uidExisting = "123456";
        assertEquals(daoTag.countOf(), 0);
        daoTag.create(new RfidTagEntity(uidExisting));
        RfidTagEntity rte = tagRepo.createIfNotExists(uidExisting);
        assertEquals(daoTag.countOf(), 1);
        assertEquals(rte.getUid(), uidExisting);

        // if the tag does not exist, check it's created
        String uidNew = "234567";
        assertEquals(daoTag.countOf(), 1);
        rte = tagRepo.createIfNotExists(uidNew);
        assertEquals(daoTag.countOf(), 2);
        assertEquals(rte.getUid(), uidNew);
    }

    @Test
    public void testGetByUid() throws Exception
    {
        // create an in-memory db using H2, for the purpose of this test
        doReturn("jdbc:h2:mem:getByUid").when(DbManager.class, "getConnectionString");
        assertTrue(DbManager.initializeDatabase());

        // get the dao
        DaoRfidTag tagRepo = (DaoRfidTag) DbManager.getDao(RfidTagEntity.class);

        // get the DAO
        Dao<RfidTagEntity, Integer> daoTag = DbManager.getDao(RfidTagEntity.class);

        assertNull(tagRepo.getByUid("not_existing"));

        String uid = "234567";
        daoTag.create(new RfidTagEntity(uid));

        RfidTagEntity rte = tagRepo.getByUid(uid);
        assertNotNull(rte);
        assertEquals(rte.getUid(), uid);
    }
}