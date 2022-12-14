package com.spacecode.smartserver.database.dao;

import com.spacecode.sdk.user.data.AccessType;
import com.spacecode.smartserver.SmartServer;
import com.spacecode.smartserver.database.DbManager;
import com.spacecode.smartserver.database.entity.AccessTypeEntity;
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
 * JUnit "AccessTypeRepository" testing class.
 */
@RunWith(PowerMockRunner.class)
@PrepareForTest({ SmartServer.class, DbManager.class })
public class DaoAccessTypeTest
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
    public void testAsAccessType() throws Exception
    {
        // create an in-memory db using H2, for the purpose of this test
        doReturn("jdbc:h2:mem:asAccessType").when(DbManager.class, "getConnectionString");
        assertTrue(DbManager.initializeDatabase());

        DaoAccessType atRepo = (DaoAccessType) DbManager.getDao(AccessTypeEntity.class);

        assertEquals(
                DaoAccessType.asAccessType(atRepo.fromAccessType(AccessType.BADGE)), AccessType.BADGE);

        assertEquals(
                DaoAccessType.asAccessType(atRepo.fromAccessType(AccessType.UNDEFINED)), AccessType.UNDEFINED);

        assertEquals(
                DaoAccessType.asAccessType(atRepo.fromAccessType(AccessType.FINGERPRINT)), AccessType.FINGERPRINT);

        assertNull(DaoAccessType.asAccessType(null));
    }

    @Test
    public void testFromAccessType() throws Exception
    {
        // create an in-memory db using H2, for the purpose of this test
        doReturn("jdbc:h2:mem:fromAccessType").when(DbManager.class, "getConnectionString");
        assertTrue(DbManager.initializeDatabase());

        DaoAccessType atRepo = (DaoAccessType) DbManager.getDao(AccessTypeEntity.class);

        assertNotNull(atRepo.fromAccessType(AccessType.BADGE));
        assertNotNull(atRepo.fromAccessType(AccessType.FINGERPRINT));
        assertNotNull(atRepo.fromAccessType(AccessType.UNDEFINED));

        assertNull(atRepo.fromAccessType(null));
    }
}