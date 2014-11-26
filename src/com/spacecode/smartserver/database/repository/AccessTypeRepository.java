package com.spacecode.smartserver.database.repository;

import com.j256.ormlite.dao.Dao;
import com.spacecode.sdk.user.data.AccessType;
import com.spacecode.smartserver.database.entity.AccessTypeEntity;
import com.spacecode.smartserver.helper.SmartLogger;

import java.sql.SQLException;
import java.util.HashMap;
import java.util.Map;
import java.util.logging.Level;

/**
 * AccessTypeEntity Repository
 */
public class AccessTypeRepository extends Repository<AccessTypeEntity>
{
    private static final Map<String, AccessTypeEntity> TYPE_TO_ENTITY = new HashMap<>();

    protected AccessTypeRepository(Dao<AccessTypeEntity, Integer> dao)
    {
        super(dao);
    }

    /**
     * Allow getting a AccessType value from a AccessTypeEntity.
     * @param ate   AccessTypeEntity to be used as equivalent.
     * @return      Matching AccessType value, or null if none exists.
     */
    public static AccessType asAccessType(AccessTypeEntity ate)
    {
        if(ate == null)
        {
            return null;
        }

        try
        {
            return AccessType.valueOf(ate.getType());
        } catch(IllegalArgumentException iae)
        {
            SmartLogger.getLogger().log(Level.WARNING, "Unknown access type", iae);
            return null;
        }
    }

    /**
     * @param accessType    SDK AccessType value.
     * @return              AccessTypeEntity equivalent (from Db).
     */
    public AccessTypeEntity fromAccessType(AccessType accessType)
    {
        if(accessType == null)
        {
            return null;
        }

        if(!TYPE_TO_ENTITY.containsKey(accessType.name()))
        {
            try
            {
                AccessTypeEntity ate = _dao.queryForFirst(_dao.queryBuilder().where()
                        .eq(AccessTypeEntity.TYPE, accessType.name())
                        .prepare());

                TYPE_TO_ENTITY.put(accessType.name(), ate);
            } catch (SQLException sqle)
            {
                SmartLogger.getLogger().log(Level.SEVERE, "Unable to get AccessType from database.", sqle);
                return null;
            }
        }

        return TYPE_TO_ENTITY.get(accessType.name());
    }
}
