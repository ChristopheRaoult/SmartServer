package com.spacecode.smartserver.database.dao;

import com.j256.ormlite.support.ConnectionSource;
import com.spacecode.sdk.user.data.GrantType;
import com.spacecode.smartserver.database.entity.GrantTypeEntity;
import com.spacecode.smartserver.helper.SmartLogger;

import java.sql.SQLException;
import java.util.HashMap;
import java.util.Map;
import java.util.logging.Level;

/**
 * GrantTypeEntity Repository
 */
public class DaoGrantType extends DaoEntity<GrantTypeEntity, Integer>
{
    private static final Map<String, GrantTypeEntity> TYPE_TO_ENTITY = new HashMap<>();

    public DaoGrantType(ConnectionSource connectionSource) throws SQLException
    {
        super(connectionSource, GrantTypeEntity.class);
    }

    /**
     * Allow getting a GrantType value from a GrantTypeEntity.
     * @param gte   GrantTypeEntity to be used as equivalent.
     * @return      Matching GrantType value, or null if none exists.
     */
    public static GrantType asGrantType(GrantTypeEntity gte)
    {
        if(gte == null)
        {
            return null;
        }

        try
        {
            return GrantType.valueOf(gte.getType());
        } catch(IllegalArgumentException iae)
        {
            SmartLogger.getLogger().log(Level.WARNING, "Unknown grant type", iae);
            return null;
        }
    }

    /**
     * @param grantType SDK GrantType value.
     * @return          GrantTypeEntity equivalent (from Db).
     */
    public GrantTypeEntity fromGrantType(GrantType grantType)
    {
        if(grantType == null)
        {
            return null;
        }

        if(!TYPE_TO_ENTITY.containsKey(grantType.name()))
        {
            try
            {
                GrantTypeEntity gte = queryForFirst(queryBuilder().where()
                        .eq(GrantTypeEntity.TYPE, grantType.name())
                        .prepare());

                TYPE_TO_ENTITY.put(grantType.name(), gte);
            } catch (SQLException sqle)
            {
                SmartLogger.getLogger().log(Level.SEVERE, "Unable to get GrantType.", sqle);
                return null;
            }
        }

        return TYPE_TO_ENTITY.get(grantType.name());
    }
}
