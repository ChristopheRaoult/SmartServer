package com.spacecode.smartserver.database.dao;

import com.j256.ormlite.dao.Dao;
import com.spacecode.sdk.user.data.FingerIndex;
import com.spacecode.smartserver.SmartServer;
import com.spacecode.smartserver.database.DbManager;
import com.spacecode.smartserver.database.entity.FingerprintEntity;
import com.spacecode.smartserver.database.entity.UserEntity;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mockito;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;

import java.util.Date;

import static org.junit.Assert.*;
import static org.powermock.api.mockito.PowerMockito.doReturn;
import static org.powermock.api.mockito.PowerMockito.mockStatic;


/**
 * JUnit "FingerprintRepository" testing class.
 */
@RunWith(PowerMockRunner.class)
@PrepareForTest({ SmartServer.class, DbManager.class })
public class DaoFingerprintTest
{
    private UserEntity _userEntity;

    @Before
    public void setUp() throws Exception
    {
        // required to avoid an initialization exception, as SmartServer has some static initialization
        mockStatic(SmartServer.class);

        mockStatic(DbManager.class, Mockito.CALLS_REAL_METHODS);

        String username = "Vincent";
        String badgeNumber = "BCDE05551";
        _userEntity = new UserEntity(username, badgeNumber);
    }

    @After
    public void tearDown() throws Exception
    {
        DbManager.close();
    }

    @Test
    public void testGetFingerprint() throws Exception
    {
        // create an in-memory db using H2, for the purpose of this test
        doReturn("jdbc:h2:mem:getfingerprint").when(DbManager.class, "getConnectionString");
        assertTrue(DbManager.initializeDatabase());

        // get the dao
        DaoFingerprint fpRepo = (DaoFingerprint) DbManager.getDao(FingerprintEntity.class);

        // get the dao's, create the fixtures
        int fingerIndex = FingerIndex.LEFT_INDEX.getIndex();
        String fpTemplate = "fake_template";
        Dao<FingerprintEntity, Integer> daoFp = DbManager.getDao(FingerprintEntity.class);
        Dao<UserEntity, Integer> daoUser = DbManager.getDao(UserEntity.class);
        daoUser.create(_userEntity);
        daoFp.create(new FingerprintEntity(_userEntity, fingerIndex, fpTemplate));

        FingerprintEntity fpe = fpRepo.getFingerprint(_userEntity, fingerIndex);
        assertEquals(fpe.getTemplate(), fpTemplate);
    }

    @Test
    public void testUpdateWhenNotExisting() throws Exception
    {
        // create an in-memory db using H2, for the purpose of this test
        doReturn("jdbc:h2:mem:updateOrCreate").when(DbManager.class, "getConnectionString");
        assertTrue(DbManager.initializeDatabase());

        // get the dao
        DaoFingerprint fpRepo = (DaoFingerprint) DbManager.getDao(FingerprintEntity.class);

        // get the dao's, create the fixtures
        int fingerIndex = FingerIndex.LEFT_INDEX.getIndex();
        String fpTemplate = "fake_template";
        FingerprintEntity fpe = new FingerprintEntity(_userEntity, fingerIndex, fpTemplate);

        Dao<FingerprintEntity, Integer> daoFp = DbManager.getDao(FingerprintEntity.class);
        Dao<UserEntity, Integer> daoUser = DbManager.getDao(UserEntity.class);
        daoUser.create(_userEntity);
        Date lastUpdate = _userEntity.getUpdatedAt();
        
        // make sure there is no fingerprint in the table yet
        assertEquals(daoFp.countOf(), 0);
        // "update" (in fact, create) the given fpe
        assertTrue(fpRepo.updateEntity(fpe));
        // take this new entity from the DB and check its data
        FingerprintEntity fpeFromDb = fpRepo.getFingerprint(_userEntity, fingerIndex);
        assertEquals(fpeFromDb.getTemplate(), fpTemplate);

        // make sure a new entry has been created
        assertEquals(daoFp.countOf(), 1);

        // check that User's "updated_at" has been updated
        daoUser.refresh(_userEntity);
        assertTrue(lastUpdate.before(_userEntity.getUpdatedAt()));
    }

    @Test
    public void testUpdate() throws Exception
    {
        // create an in-memory db using H2, for the purpose of this test
        doReturn("jdbc:h2:mem:updateExistingFp").when(DbManager.class, "getConnectionString");
        assertTrue(DbManager.initializeDatabase());

        // get the dao
        DaoFingerprint fpRepo = (DaoFingerprint) DbManager.getDao(FingerprintEntity.class);

        // get the dao's, create the fixtures
        int fingerIndex = FingerIndex.LEFT_INDEX.getIndex();
        String fpTemplate = "fake_template";
        FingerprintEntity fpe = new FingerprintEntity(_userEntity, fingerIndex, fpTemplate);

        Dao<FingerprintEntity, Integer> daoFp = DbManager.getDao(FingerprintEntity.class);
        Dao<UserEntity, Integer> daoUser = DbManager.getDao(UserEntity.class);
        daoUser.create(_userEntity);
        Date lastUpdate = _userEntity.getUpdatedAt();
        daoFp.create(fpe);

        long entriesCount = daoFp.countOf();

        // now the entity has been created, update it and save the changes in DB
        fpTemplate = "new_template";
        fpe.setTemplate(fpTemplate);
        assertTrue(fpRepo.updateEntity(fpe));

        FingerprintEntity fpeFromDb = fpRepo.getFingerprint(_userEntity, fingerIndex);
        assertEquals(fpeFromDb.getTemplate(), fpTemplate);

        // make sure we did not create a new entry or something wrong happened with entries count
        assertEquals(entriesCount, daoFp.countOf());
        
        // check that User's "updated_at" has been updated
        daoUser.refresh(_userEntity);
        assertTrue(lastUpdate.before(_userEntity.getUpdatedAt()));
    }

    @Test
    public void testDelete() throws Exception
    {
        // create an in-memory db using H2, for the purpose of this test
        doReturn("jdbc:h2:mem:deleteFp").when(DbManager.class, "getConnectionString");
        assertTrue(DbManager.initializeDatabase());

        // get the dao
        DaoFingerprint fpRepo = (DaoFingerprint) DbManager.getDao(FingerprintEntity.class);

        // check it fails for an unknown user
        assertFalse(fpRepo.delete("UnknownUser", 4));

        // get the dao's, create the fixtures
        int fingerIndex = FingerIndex.LEFT_INDEX.getIndex();
        String fpTemplate = "fake_template";
        FingerprintEntity fpe = new FingerprintEntity(_userEntity, fingerIndex, fpTemplate);

        Dao<FingerprintEntity, Integer> daoFp = DbManager.getDao(FingerprintEntity.class);
        Dao<UserEntity, Integer> daoUser = DbManager.getDao(UserEntity.class);
        daoUser.create(_userEntity);
        Date lastUpdate = _userEntity.getUpdatedAt();
        daoFp.create(fpe);

        long entriesCount = daoFp.countOf();
        assertTrue(fpRepo.delete(_userEntity.getUsername(), fingerIndex));
        // make sure there is one less entry
        assertEquals(entriesCount - 1, daoFp.countOf());
        
        // check that User's "updated_at" has been updated
        daoUser.refresh(_userEntity);
        assertTrue(lastUpdate.before(_userEntity.getUpdatedAt()));
    }

    @Test
    public void testPersist() throws Exception
    {
        // create an in-memory db using H2, for the purpose of this test
        doReturn("jdbc:h2:mem:persistFp").when(DbManager.class, "getConnectionString");
        assertTrue(DbManager.initializeDatabase());

        // get the dao
        DaoFingerprint fpRepo = (DaoFingerprint) DbManager.getDao(FingerprintEntity.class);

        // assert it fails for an unknown user
        assertFalse(fpRepo.persist("UnknownUser", 4, "fake_template"));

        // get the DAO's and add the fixtures
        Dao<FingerprintEntity, Integer> daoFp = DbManager.getDao(FingerprintEntity.class);
        Dao<UserEntity, Integer> daoUser = DbManager.getDao(UserEntity.class);
        daoUser.create(_userEntity);

        long entriesCount = daoFp.countOf();
        assertTrue(fpRepo.persist(_userEntity.getUsername(), 4, "fake_template"));
        assertEquals(entriesCount + 1, daoFp.countOf());
    }
}