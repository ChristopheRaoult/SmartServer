package com.spacecode.smartserver.database.repository;

import com.j256.ormlite.dao.Dao;
import com.spacecode.smartserver.database.entity.RfidTagEntity;
import com.spacecode.smartserver.helper.SmartLogger;

import java.sql.SQLException;
import java.util.logging.Level;

/**
 * RfidTag Repository
 */
public class RfidTagRepository extends Repository<RfidTagEntity>
{
    protected RfidTagRepository(Dao<RfidTagEntity, Integer> dao)
    {
        super(dao);
    }

    /**
     * Create the RfidTagEntity if a tag with the same UID does not exists in Db.
     *
     * @param uid   RFID Tag Unique Identifier.
     *
     * @return      RfidTagEntity just inserted in Db (or already existing). Null if something went wrong (SQLException).
     */
    public RfidTagEntity createIfNotExists(String uid)
    {
        if(uid == null || "".equals(uid.trim()))
        {
            return null;
        }

        RfidTagEntity rte = getByUid(uid);

        if(rte != null)
        {
            return rte;
        }

        try
        {
            RfidTagEntity newRte = new RfidTagEntity(uid);
            _dao.create(newRte);

            return newRte;
        } catch (SQLException sqle)
        {
            SmartLogger.getLogger().log(Level.SEVERE, "Unable to insert RfidTag in DB.", sqle);
            return null;
        }
    }

    /**
     * @param uid Desired RFID Tag's unique identifier.
     *
     * @return  RfidTagEntity if any tag with given uid is found. Null if none is found or SQLException occurs.
     */
    public RfidTagEntity getByUid(String uid)
    {
        if(uid == null || uid.trim().isEmpty())
        {
            return null;
        }

        try
        {
            return _dao.queryForFirst(
                    _dao.queryBuilder()
                            .where()
                            .eq(RfidTagEntity.UID, uid)
                            .prepare()
            );
        } catch (SQLException sqle)
        {
            SmartLogger.getLogger().log(Level.SEVERE, "Unable to get Rfid Tag entity from DB.", sqle);
            return null;
        }
    }
}
